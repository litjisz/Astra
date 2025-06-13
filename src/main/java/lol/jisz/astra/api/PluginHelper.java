package lol.jisz.astra.api;

import lol.jisz.astra.Astra;
import lol.jisz.astra.command.CommandManager;
import lol.jisz.astra.database.providers.DatabaseType;
import lol.jisz.astra.database.registry.DatabaseRegistry;
import lol.jisz.astra.task.TaskManager;
import lol.jisz.astra.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginHelper {

    private final Astra plugin;
    private final Logger logger;

    private CommandManager commandManager;
    private DatabaseRegistry databaseRegistry;

    /**
     * Constructor for the PluginHelper class.
     * @param plugin Instance of the main plugin
     */
    public PluginHelper(Astra plugin) {
        this.plugin = plugin;
        this.logger = plugin.logger();
        
        try {
            Implements.init(plugin);
            this.commandManager = new CommandManager(plugin);
            this.databaseRegistry = new DatabaseRegistry(plugin);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Failed to initialize CommandManager", e);
            }
        }
    }

    /**
     * Unloads the plugin by disabling all implementations and saving configuration.
     * This method should be called during the plugin's disable phase.
     * Any exceptions during unloading are caught and logged.
     */
    public void unload() {
        try {
            Implements.disableAll();
            saveConfig();

            if (logger.isDebugMode()) {
                logger.info("PluginHelper unloaded successfully");
            }

        } catch (Exception e) {
            logger.error("Error unloading the plugin", e);
        }
    }

    /**
     * Reloads the plugin by disabling all implementations, reloading configuration,
     * and then re-enabling all implementations.
     * This method can be called to refresh the plugin state without a full server restart.
     * Any exceptions during reloading are caught and logged.
     */
    public void reload() {
        try {
            Implements.disableAll();

            plugin.reloadConfig();
            loadConfig();

            Implements.enableAll();

            if (logger.isDebugMode()) {
                logger.info("PluginHelper reloaded successfully");
            }

        } catch (Exception e) {
            logger.error("Error reloading the plugin", e);
        }
    }

    /**
     * Loads the plugin configuration by ensuring default configuration exists.
     * This is a private helper method called during the load process.
     */
    private void loadConfig() {
        plugin.saveDefaultConfig();
    }

    /**
     * Saves the current plugin configuration to disk.
     * This is a private helper method called during the unload process.
     */
    private void saveConfig() {
        plugin.saveConfig();
    }

    /**
     * Gets a fluent registration API for this plugin helper.
     * @return A new PluginRegistrar instance for fluent method chaining
     */
    public PluginRegistrar register() {
        return new PluginRegistrar();
    }

    /**
     * Fluent API for plugin registration and initialization operations.
     */
    public class PluginRegistrar {

        /**
         * Scans the specified package for plugin components to register.
         * This method searches the given package for classes that should be registered
         * with the plugin system, such as commands, listeners, or other components.
         * Any errors during scanning are caught and logged.
         *
         * @param packageName The fully qualified name of the package to scan
         * @return This registrar for method chaining
         */
        public PluginRegistrar scanPackage(String packageName) {
            try {
                plugin.scanPackage(packageName);
                if (logger.isDebugMode()) {
                    logger.info("Scanned package: " + packageName);
                }
            } catch (Exception e) {
                logger.error("Failed to scan package: " + packageName, e);
            }
            return this;
        }

        /**
         * Initializes the command system for this plugin.
         * @return This registrar for method chaining
         */
        public PluginRegistrar initTaskSystem() {
            try {
                Implements.register(new TaskManager());
                if (logger.isDebugMode()) {
                    logger.info("Task system initialized successfully");
                }
            } catch (Exception e) {
                logger.error("Failed to initialize task system", e);
            }
            return this;
        }

        /**
         * Registers a database with the specified type.
         * @param type The type of database to register
         * @param config The configuration file containing database settings
         * @param path The path in the configuration file where database settings are located
         * @return This registrar for method chaining
         */
        public PluginRegistrar registerDatabase(DatabaseType type, FileConfiguration config, String path) {
            try {
                if (databaseRegistry == null) {
                    databaseRegistry = new DatabaseRegistry(plugin);
                }

                databaseRegistry.registerDatabase(
                        config,
                        path,
                        type,
                        true,
                        type.getName()
                );

                if (logger.isDebugMode()) {
                    logger.info("Registered database of type: " + type);
                }
            } catch (Exception e) {
                logger.error("Failed to register database of type: " + type, e);
            }
            return this;
        }
        
        /**
         * Loads the plugin configuration.
         * @return This registrar for method chaining
         */
        public PluginRegistrar loadConfig() {
            PluginHelper.this.loadConfig();
            return this;
        }
        
        /**
         * Saves the plugin configuration.
         * @return This registrar for method chaining
         */
        public PluginRegistrar saveConfig() {
            PluginHelper.this.saveConfig();
            return this;
        }
        
        /**
         * Completes the registration process and returns the plugin helper.
         * @return The PluginHelper instance
         */
        public PluginHelper done() {
            return PluginHelper.this;
        }
    }

    /**
     * Gets the command manager instance.
     * @return The CommandManager instance used by this plugin
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Gets the plugin instance.
     * @return The main Astra plugin instance
     */
    public Astra getPlugin() {
        return plugin;
    }
}