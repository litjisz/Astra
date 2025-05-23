package lol.jisz.astra.database.registry;

import lol.jisz.astra.Astra;
import lol.jisz.astra.database.AstraDatabase;
import lol.jisz.astra.database.providers.DatabaseType;
import lol.jisz.astra.database.providers.MongoDBProvider;
import lol.jisz.astra.database.providers.NullDBProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Factory class for creating database instances based on configuration.
 */
public class DatabaseFactory {

    private final Astra plugin;

    public DatabaseFactory(Astra plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a database instance based on the specified type and configuration.
     *
     * @param config The configuration section containing database settings
     * @param path The path within the configuration to the database settings
     * @param type The type of database to create
     * @return The created database instance
     */
    public AstraDatabase createDatabase(FileConfiguration config, String path, DatabaseType type) {
        ConfigurationSection dbConfig = config.getConfigurationSection(path);
        
        if (dbConfig == null) {
            plugin.logger().warning("No configuration found at path: " + path + ", using empty configuration");
            dbConfig = config.createSection(path);
        }
        
        if (type == null || type == DatabaseType.NONE) {
            String typeStr = dbConfig.getString("type", "NONE");
            type = DatabaseType.fromString(typeStr);
        }

        return switch (type) {
            case MONGODB -> new MongoDBProvider(plugin, config);
            case MYSQL, SQLITE, POSTGRESQL, MARIADB -> {
                plugin.logger().warning("MySQL, SQLite, PostgreSQL, and MariaDB are not yet implemented. Using NullDatabase.");
                yield new NullDBProvider();
            }
            default -> {
                plugin.logger().warning("Unsupported database type: " + type + ", using NullDatabase");
                yield new NullDBProvider();
            }
        };
    }
}