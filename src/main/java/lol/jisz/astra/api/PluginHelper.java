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
            this.commandManager = new CommandManager(plugin);
            this.databaseRegistry = new DatabaseRegistry(plugin);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Failed to initialize CommandManager", e);
            }
        }
    }

    /**
     * Loads the plugin by initializing the module system, command system, and loading configuration.
     * This method should be called during the plugin's enable phase.
     * Any exceptions during loading are caught and logged.
     */
    public void load() {
        try {
            initModuleSystem();
            initCommandSystem();
            initTaskSystem();
            loadConfig();
            logger.info("PluginHelper loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading the plugin", e);
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
            logger.info("PluginHelper unloaded successfully");

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
            logger.info("PluginHelper reloaded successfully");

        } catch (Exception e) {
            logger.error("Error reloading the plugin", e);
        }
    }

    /**
     * Initializes the module system by setting up the Implements framework.
     * This is a private helper method called during the load process.
     */
    private void initModuleSystem() {
        Implements.init(plugin);
    }

    /**
     * Initializes the command system by creating a new CommandManager instance.
     * This is a private helper method called during the load process.
     * Any exceptions during initialization are caught and logged.
     */
    private void initCommandSystem() {
        try {
            commandManager = new CommandManager(plugin);
        } catch (Exception e) {
            logger.error("Error initializing the command system", e);
        }
    }

    /**
     * Initializes the task system by registering the TaskManager.
     * This is a private helper method called during the load process.
     */
    private void initTaskSystem() {
        Implements.register(new TaskManager());
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
         * Registers a database with the specified type.
         * @param type The type of database to register
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

                logger.info("Registered database of type: " + type);
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