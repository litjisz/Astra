package lol.jisz.astra;

import lol.jisz.astra.api.AutoRegisterModule;
import lol.jisz.astra.api.Implements;
import lol.jisz.astra.api.Module;
import lol.jisz.astra.api.PluginHelper;
import lol.jisz.astra.examples.CommandExample;
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

    private static Logger logger;
    private static Astra instance;

    @Override
    public void onEnable() {
        try {
            instance = this;
            logger = new Logger(this);

            initAstra();
            scanAutoPackages();
            pluginHelper.load();

            onInitialize();

            logger.info("Plugin enabled successfully!");
        } catch (Exception e) {
            logger.error("Error enabling the plugin", e);
        }
    }

    @Override
    public void onDisable() {
        try {
            pluginHelper.unload();
            onShutdown();

            logger.info("Plugin disabled successfully!");
        } catch (Exception e) {
            logger.error("Error disabling the plugin", e);
        }
    }

    private void initAstra() {
        Implements.init(this);
        pluginHelper = new PluginHelper(this);
        commandManager = pluginHelper.getCommandManager();
        classScanner = new ClassScanner(this);
        saveDefaultConfig();
        registerDefaultCommand();
    }

    private void scanAutoPackages() {
        String[] packages = getAutoScanPackages();
        if (packages != null && packages.length > 0) {
            for (String packageName : packages) {
                if (packageName != null && !packageName.isEmpty()) {
                    classScanner.scanPackage(packageName);
                }
            }
        }
    }

    public void reload() {
        try {
            pluginHelper.reload();
            onReload();

            logger.info("Plugin reloaded successfully!");
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

    private void registerDefaultCommand() {
        CommandBase defaultCommand = new CommandExample();
        commandManager.registerCommand(defaultCommand);
    }

    /**
     * Manually registers a command
     * @param command Command to register
     */
    public void registerCommand(CommandBase command) {
        commandManager.registerCommand(command);
    }

    /**
     * Removes a registered command
     * @param command Name of the command to remove
     */
    public void unregisterCommand(CommandBase command) {
        commandManager.unregisterCommand(command);
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
                    clazz.isAnnotationPresent(AutoRegisterCommand.class)) {
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
     * 
     * @return The logger instance for this plugin
     */
    public Logger logger() {
        return logger;
    }

    /**
     * Returns the packages that should be automatically scanned to register modules and commands
     * @return Array of package names to scan or null if no automatic scanning is desired
     */
    protected String[] getAutoScanPackages() {
        return new String[]{"lol.jisz.astra"};
    }

    /**
     * Scans an additional package to register modules and commands
     * @param packageName Name of the package to scan
     */
    public void scanPackage(String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            classScanner.scanPackage(packageName);
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
}