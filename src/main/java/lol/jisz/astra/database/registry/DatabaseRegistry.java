package lol.jisz.astra.database.registry;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.module.AbstractModule;
import lol.jisz.astra.database.AstraDatabase;
import lol.jisz.astra.database.providers.DatabaseType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for managing database connections in the Astra framework.
 * Handles registration, initialization, and access to database providers.
 */
public class DatabaseRegistry extends AbstractModule {

    private final Map<String, AstraDatabase> databases = new ConcurrentHashMap<>();
    private final DatabaseFactory databaseFactory;

    public DatabaseRegistry(Astra plugin) {
        super(plugin);
        this.databaseFactory = new DatabaseFactory(plugin);
    }

    @Override
    public void enable() {
        logger().info("Initializing database registry");
    }

    @Override
    public void disable() {
        closeAllDatabases();
    }

    /**
     * Closes all registered database connections.
     */
    public void closeAllDatabases() {
        for (Map.Entry<String, AstraDatabase> entry : databases.entrySet()) {
            try {
                entry.getValue().close();
                logger().info("Closed database connection: " + entry.getKey());
            } catch (Exception e) {
                logger().error("Error closing database connection: " + entry.getKey(), e);
            }
        }
        databases.clear();
        logger().info("All database connections closed");
    }

    /**
     * Registers a database provider with the registry.
     *
     * @param id               The identifier for this database
     * @param database         The database implementation
     */
    public void registerDatabase(String id, AstraDatabase database) throws Exception {
        if (id == null || database == null) {
            throw new IllegalArgumentException("Database ID and implementation cannot be null");
        }

        String normalizedId = id.toLowerCase();
        databases.put(normalizedId, database);

        logger().info("Registered database: " + normalizedId + " (" + database.getType() + ")");
    }

    /**
     * Creates and registers a database from configuration.
     *
     * @param config     The configuration section containing database settings
     * @param path       The path within the configuration to the database settings
     * @param type       The type of database to create
     * @param initialize Whether to initialize the database immediately
     * @param id         The identifier for this database (if null, path will be used)
     */
    public void registerDatabase(
            FileConfiguration config,
            String path,
            DatabaseType type,
            boolean initialize,
            String id) throws Exception {

        String databaseId = id != null ? id : path;

        AstraDatabase database = databaseFactory.createDatabase(config, path, type);
        registerDatabase(databaseId, database);

        if (initialize) {
            try {
                database.initialize();
                logger().info("Initialized database: " + databaseId);
            } catch (Exception e) {
                logger().error("Failed to initialize database: " + databaseId, e);
            }
        }

    }

    /**
     * Gets a database by its identifier.
     *
     * @param id The identifier of the database
     * @return The database or null if not found
     */
    public AstraDatabase getDatabase(String id) {
        return databases.get(id.toLowerCase());
    }

    /**
     * Gets all registered databases.
     *
     * @return An unmodifiable map of all registered databases
     */
    public Map<String, AstraDatabase> getAllDatabases() {
        return Collections.unmodifiableMap(databases);
    }

}