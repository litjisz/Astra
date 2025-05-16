package lol.jisz.astra.database;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.AbstractModule;
import lol.jisz.astra.database.providers.MongoDBProvider;
import lol.jisz.astra.database.providers.MySQLProvider;
import lol.jisz.astra.database.providers.SQLiteProvider;
import lol.jisz.astra.utils.ConfigManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Module for managing database connections and operations.
 * Supports MySQL, MongoDB, and SQLite database types.
 */
public class DatabaseModule extends AbstractModule {

    private final Map<String, DatabaseProvider> providers;
    private DatabaseProvider defaultProvider;
    private boolean initialized = false;

    public DatabaseModule(Astra plugin) {
        super(plugin);
        this.providers = new HashMap<>();
    }

    @Override
    public void enable() {
        logger().info("Initializing Database Module...");

        ConfigManager configManager = getConfigManager();
        if (!configManager.configExists("database")) {
            configManager.createFileConfiguration("database");
            setupDefaultConfig(configManager);
        }

        loadDatabaseProviders();

        initialized = true;
        logger().info("Database Module initialized successfully");
    }

    @Override
    public void disable() {
        for (DatabaseProvider provider : providers.values()) {
            try {
                provider.close();
            } catch (Exception e) {
                logger().error("Error closing database provider: " + e.getMessage(), e);
            }
        }

        providers.clear();
        defaultProvider = null;
        initialized = false;
        logger().info("Database Module disabled");
    }

    @Override
    public void reload() {
        logger().info("Reloading Database Module...");

        for (DatabaseProvider provider : providers.values()) {
            try {
                provider.close();
            } catch (Exception e) {
                logger().error("Error closing database provider: " + e.getMessage(), e);
            }
        }

        providers.clear();
        defaultProvider = null;

        getConfigManager().reloadConfig("database");
        loadDatabaseProviders();

        logger().info("Database Module reloaded successfully");
    }

    /**
     * Registers a database provider with the manager.
     *
     * @param name         The name identifier for this provider
     * @param provider     The database provider implementation
     * @param setAsDefault Whether to set this provider as the default
     */
    public void registerProvider(String name, DatabaseProvider provider, boolean setAsDefault) {
        providers.put(name.toLowerCase(), provider);

        if (setAsDefault || defaultProvider == null) {
            defaultProvider = provider;
        }

    }

    /**
     * Gets a registered database provider by name.
     *
     * @param name The name of the provider to retrieve
     * @return The database provider, or null if not found
     */
    public DatabaseProvider getProvider(String name) {
        return providers.get(name.toLowerCase());
    }

    /**
     * Gets the default database provider.
     *
     * @return The default database provider
     */
    public DatabaseProvider getDefaultProvider() {
        return defaultProvider;
    }

    /**
     * Checks if the module has been initialized.
     *
     * @return True if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Sets up the default database configuration.
     *
     * @param configManager The config manager instance
     */
    private void setupDefaultConfig(ConfigManager configManager) {
        // Default database settings
        configManager.set("database", "default-provider", "sqlite");

        // MySQL settings
        configManager.set("database", "mysql.enabled", false);
        configManager.set("database", "mysql.host", "localhost");
        configManager.set("database", "mysql.port", 3306);
        configManager.set("database", "mysql.database", "minecraft");
        configManager.set("database", "mysql.username", "root");
        configManager.set("database", "mysql.password", "password");
        configManager.set("database", "mysql.options", "useSSL=false&autoReconnect=true");

        // MongoDB settings
        configManager.set("database", "mongodb.enabled", false);
        configManager.set("database", "mongodb.host", "localhost");
        configManager.set("database", "mongodb.port", 27017);
        configManager.set("database", "mongodb.database", "minecraft");
        configManager.set("database", "mongodb.username", "");
        configManager.set("database", "mongodb.password", "");

        // SQLite settings
        configManager.set("database", "sqlite.enabled", true);
        configManager.set("database", "sqlite.database", "astra");

        configManager.saveConfig("database");
    }

    /**
     * Loads and initializes database providers from configuration.
     */
    private void loadDatabaseProviders() {
        ConfigManager configManager = getConfigManager();
        String defaultProviderName = configManager.getString("database", "default-provider", "sqlite");

        if (configManager.getBoolean("database", "sqlite.enabled", true)) {
            String database = configManager.getString("database", "sqlite.database", "astra");

            SQLiteProvider sqliteProvider = new SQLiteProvider(getPlugin(), database);
            try {
                sqliteProvider.initialize();
                registerProvider("sqlite", sqliteProvider, defaultProviderName.equalsIgnoreCase("sqlite"));
                logger().info("SQLite provider initialized successfully");
            } catch (Exception e) {
                logger().error("Failed to initialize SQLite provider", e);
            }
        }

        if (configManager.getBoolean("database", "mysql.enabled", false)) {
            String host = configManager.getString("database", "mysql.host", "localhost");
            int port = configManager.getInt("database", "mysql.port", 3306);
            String database = configManager.getString("database", "mysql.database", "minecraft");
            String username = configManager.getString("database", "mysql.username", "root");
            String password = configManager.getString("database", "mysql.password", "password");
            String options = configManager.getString("database", "mysql.options", "useSSL=false&autoReconnect=true");

            MySQLProvider mysqlProvider = new MySQLProvider(getPlugin(), host, port, database, username, password, options);
            try {
                mysqlProvider.initialize();
                registerProvider("mysql", mysqlProvider, defaultProviderName.equalsIgnoreCase("mysql"));
                logger().info("MySQL provider initialized successfully");
            } catch (Exception e) {
                logger().error("Failed to initialize MySQL provider", e);
            }
        }

        if (configManager.getBoolean("database", "mongodb.enabled", false)) {
            String host = configManager.getString("database", "mongodb.host", "localhost");
            int port = configManager.getInt("database", "mongodb.port", 27017);
            String database = configManager.getString("database", "mongodb.database", "minecraft");
            String username = configManager.getString("database", "mongodb.username", "");
            String password = configManager.getString("database", "mongodb.password", "");

            MongoDBProvider mongoProvider = new MongoDBProvider(getPlugin(), host, port, database, username, password);
            try {
                mongoProvider.initialize();
                registerProvider("mongodb", mongoProvider, defaultProviderName.equalsIgnoreCase("mongodb"));
                logger().info("MongoDB provider initialized successfully");
            } catch (Exception e) {
                logger().error("Failed to initialize MongoDB provider", e);
            }
        }

        if (defaultProvider == null && !providers.isEmpty()) {
            defaultProvider = providers.values().iterator().next();
            logger().warning("No default provider specified or available. Using " +
                    defaultProvider.getType() + " as default.");
        } else if (defaultProvider == null) {
            logger().error("No database providers were initialized. Database functionality will not be available.");
        }
    }
}