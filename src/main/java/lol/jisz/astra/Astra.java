package lol.jisz.astra;

import lol.jisz.astra.api.Implements;
import lol.jisz.astra.api.PluginHelper;
import lol.jisz.astra.api.annotations.AutoRegisterModule;
import lol.jisz.astra.api.interfaces.Module;
import lol.jisz.astra.command.AutoRegisterCommand;
import lol.jisz.astra.command.CommandBase;
import lol.jisz.astra.command.CommandManager;
import lol.jisz.astra.utils.ClassScanner;
import lol.jisz.astra.utils.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Abstract base class for Astra plugins that provides core functionality for command management,
 * module registration, and automatic class scanning.
 */
public abstract class Astra extends JavaPlugin {

    private CommandManager commandManager;
    private ClassScanner classScanner;
    private PluginHelper pluginHelper;
    private boolean initialized = false;

    private static Logger logger;
    private static Astra instance;

    @Override
    public void onEnable() {
        try {
            instance = this;
            logger = new Logger(this);

            if (getResource("config.yml") != null) {
                saveDefaultConfig();
            } else {
                if (logger.isDebugMode()) {
                    getLogger().warning("No config.yml found. Default configuration will not be loaded.");
                }
            }

            initAstra();

            if (pluginHelper != null) {
                pluginHelper.load();
            } else {
                if (logger.isDebugMode()) {
                    getLogger().warning("PluginHelper could not be initialized. Plugin will not function correctly.");
                }
            }

            onInitialize();
            initialized = true;

            if (logger.isDebugMode()) {
                logger.info("Plugin initialized successfully!");
            }
        } catch (Exception e) {
            logger.error("Error enabling the plugin", e);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (pluginHelper != null) {
                pluginHelper.unload();
            }
            onShutdown();

            if (logger.isDebugMode()) {
                logger.info("Plugin disabled successfully!");
            }
        } catch (Exception e) {
            logger.error("Error disabling the plugin", e);
        }
    }

    private void initAstra() {
        try {
            pluginHelper = new PluginHelper(this);

            if (pluginHelper != null) {
                commandManager = pluginHelper.getCommandManager();
                classScanner = new ClassScanner(this);

            } else {
                logger.error("PluginHelper could not be initialized.");
            }
        } catch (Exception e) {
            logger.error("Error during Astra initialization", e);
        }
    }

    public void reload() {
        try {
            if (pluginHelper != null) {
                pluginHelper.reload();
            }
            onReload();

            if (logger.isDebugMode()) {
                logger.info("Plugin reloaded successfully!");
            }
        } catch (Exception e) {
            logger.error("Error reloading the plugin", e);
        }
    }

    /**
     * Registers a module in the system
     * @param module Module to register
     * @param <T> Module type
     * @return Instance of the registered module
     */
    public <T extends Module> T registerModule(T module) {
        return Implements.register(module);
    }

    /**
     * Manually registers a command
     * @param command Command to register
     */
    public void registerCommand(CommandBase command) {
        if (commandManager != null) {
            commandManager.registerCommand(command);
        } else {
            logger.warning("The command could not be registered because CommandManager is null.");
        }
    }

    /**
     * Removes a registered command
     * @param command Name of the command to remove
     */
    public void unregisterCommand(CommandBase command) {
        if (commandManager != null) {
            commandManager.unregisterCommand(command);
        } else {
            logger.warning("The command could not be removed because CommandManager is null.");
        }
    }

    /**
     * Registers a class as a command or module if it has the corresponding annotations
     * @param clazz Class to register
     * @return true if successfully registered, false otherwise
     */
    public boolean registerClass(Class<?> clazz) {
        try {
            if (Module.class.isAssignableFrom(clazz) &&
                    clazz.isAnnotationPresent(AutoRegisterModule.class)) {
                Implements.register((Module) clazz.getDeclaredConstructor().newInstance());
                return true;
            } else if (CommandBase.class.isAssignableFrom(clazz) &&
                    clazz.isAnnotationPresent(AutoRegisterCommand.class) &&
                    commandManager != null) {
                commandManager.registerCommand((CommandBase) clazz.getDeclaredConstructor().newInstance());
                return true;
            }
        } catch (Exception e) {
            logger.error("Error registering the class: " + clazz.getName(), e);
        }
        return false;
    }

    /**
     * Gets the command manager
     * @return Instance of the command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Gets the class scanner
     * @return Instance of the class scanner
     */
    public ClassScanner getClassScanner() {
        return classScanner;
    }

    /**
     * Gets the plugin helper
     * @return Instance of the plugin helper
     */
    public PluginHelper getPluginHelper() {
        return pluginHelper;
    }

    /**
     * Gets the plugin instance
     * @return Plugin instance
     */
    public static Astra getInstance() {
        return instance;
    }

    /**
     * Provides access to the plugin's logger instance for logging messages.
     * @return The logger instance for this plugin
     */
    public Logger logger() {
        return logger;
    }

    /**
     * Checks if the logger is in debug mode.
     * @return true if debug mode is enabled, false otherwise
     */
    public boolean isDebugMode() {
        return logger.isDebugMode();
    }

    /**
     * Sets the debug mode for the logger.
     * @param debug true to enable debug mode, false to disable
     */
    public void setDebugMode(boolean debug) {
        logger.setDebugMode(debug);
    }

    /**
     * Scans an additional package to register modules and commands
     * @param packageName Name of the package to scan
     */
    public void scanPackage(String packageName) {
        if (packageName != null && !packageName.isEmpty() && classScanner != null) {
            classScanner.scanPackage(packageName);
        } else if (classScanner == null) {
            logger.warning("The package could not be scanned because ClassScanner is null.");
        }
    }

    /**
     * Called when the plugin is initialized after all core systems have been set up.
     * This method should be implemented by subclasses to perform plugin-specific initialization.
     * It is called during the onEnable phase after Astra's internal initialization is complete.
     */
    protected abstract void onInitialize();

    /**
     * Called when the plugin is being shut down before core systems are unloaded.
     * This method should be implemented by subclasses to perform plugin-specific cleanup.
     * It is called during the onDisable phase before Astra's internal shutdown procedures.
     */
    protected abstract void onShutdown();

    /**
     * Called when the plugin is being reloaded.
     * This method should be implemented by subclasses to handle plugin-specific reload logic.
     * It is called after the PluginHelper has been reloaded but before the reload completion message.
     */
    protected abstract void onReload();

    /**
     * Checks if the plugin was successfully initialized
     * @return true if the plugin was initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
}