package lol.jisz.astra.database;

import lol.jisz.astra.Astra;
import lol.jisz.astra.utils.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.SQLException;

/**
 * Manages database connections and provides access to different database providers.
 */
public class DatabaseManager {
    private final Logger logger;
    private final Map<String, DatabaseProvider> providers;
    private DatabaseProvider defaultProvider;

    public DatabaseManager(Astra plugin) {
        this.logger = plugin.logger();
        this.providers = new ConcurrentHashMap<>();
    }

    /**
     * Registers a database provider with the manager.
     *
     * @param name The name identifier for this provider
     * @param provider The database provider implementation
     * @param setAsDefault Whether to set this provider as the default
     * @return The registered provider
     */
    public DatabaseProvider registerProvider(String name, DatabaseProvider provider, boolean setAsDefault) {
        if (name == null || provider == null) {
            throw new IllegalArgumentException("Provider name and implementation cannot be null");
        }
        
        providers.put(name.toLowerCase(), provider);
        if (setAsDefault || defaultProvider == null) {
            defaultProvider = provider;
        }

        logger.info("Registered database provider: " + name);
        return provider;
    }

    /**
     * Gets a database provider by name.
     *
     * @param name The name of the provider to retrieve
     * @return The database provider or null if not found
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
     * Sets the default database provider.
     *
     * @param name The name of the provider to set as default
     * @return True if the provider was found and set as default, false otherwise
     */
    public boolean setDefaultProvider(String name) {
        DatabaseProvider provider = getProvider(name.toLowerCase());
        if (provider != null) {
            defaultProvider = provider;
            return true;
        }
        return false;
    }

    /**
     * Closes all database connections.
     */
    public void closeAll() {
        for (Map.Entry<String, DatabaseProvider> entry : providers.entrySet()) {
            try {
                entry.getValue().close();
                logger.info("Closed database provider: " + entry.getKey());
            } catch (SQLException e) {
                logger.error("SQL error closing database provider: " + entry.getKey(), e);
            } catch (Exception e) {
                logger.error("Error closing database provider: " + entry.getKey(), e);
            }
        }
        logger.info("All database connections closed");
    }

    /**
     * Initializes the default database provider.
     * 
     * @return True if initialization was successful, false otherwise
     */
    public boolean initializeDefaultProvider() {
        if (defaultProvider == null) {
            logger.error("No default database provider set");
            return false;
        }
        
        try {
            defaultProvider.initialize();
            return true;
        } catch (Exception e) {
            logger.error("Failed to initialize default database provider", e);
            return false;
        }
    }

    /**
     * Initializes all registered database providers.
     */
    public void initializeAllProviders() {
        for (Map.Entry<String, DatabaseProvider> entry : providers.entrySet()) {
            try {
                entry.getValue().initialize();
                logger.info("Initialized database provider: " + entry.getKey());
            } catch (Exception e) {
                logger.error("Failed to initialize database provider: " + entry.getKey(), e);
            }
        }
    }
}