package lol.jisz.astra.api;

import lol.jisz.astra.Astra;
import lol.jisz.astra.command.CommandManager;
import lol.jisz.astra.database.DatabaseManager;
import lol.jisz.astra.database.DatabaseModule;
import lol.jisz.astra.task.TaskManager;
import lol.jisz.astra.utils.Logger;

public class PluginHelper {

    private final Astra plugin;
    private final Logger logger;

    private CommandManager commandManager;
    private DatabaseManager databaseManager;

    /**
     * Constructor for the PluginHelper class.
     * @param plugin Instance of the main plugin
     */
    public PluginHelper(Astra plugin) {
        this.plugin = plugin;
        this.logger = plugin.logger();
        
        try {
            this.commandManager = new CommandManager(plugin);
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
     * Initializes the database system by creating a DatabaseManager and optionally registering 
     * the DatabaseModule in the Implements system.
     * This method can be called to enable database functionality in the plugin.
     *
     * @param registerModule Whether to register the module in the Implements system
     * @return The initialized DatabaseModule instance, or null if initialization failed
     */
    public DatabaseModule initDatabaseSystem(boolean registerModule) {
        try {
            this.databaseManager = new DatabaseManager(plugin);
            DatabaseModule databaseModule = new DatabaseModule(plugin);

            if (registerModule) {
                Implements.register(databaseModule);
                logger.info("Database system initialized and module registered successfully");
            } else {
                logger.info("Database system initialized successfully");
            }

            return databaseModule;
        } catch (Exception e) {
            logger.error("Error initializing the database system", e);
            return null;
        }
    }

    /**
     * Initializes the database system without registering the module.
     * This method can be called to enable database functionality in the plugin.
     *
     * @return The initialized DatabaseModule instance, or null if initialization failed
     */
    public DatabaseModule initDatabaseSystem() {
        try {
            this.databaseManager = new DatabaseManager(plugin);
            DatabaseModule databaseModule = new DatabaseModule(plugin);

            Implements.register(databaseModule);
            logger.info("Database system initialized and module registered successfully");

            return databaseModule;
        } catch (Exception e) {
            logger.error("Error initializing the database system", e);
            return null;
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

    /**
     * Gets the database manager instance.
     * @return The DatabaseManager instance used by this plugin, or null if not initialized
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}