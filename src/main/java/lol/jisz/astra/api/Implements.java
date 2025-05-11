package lol.jisz.astra.api;

import lol.jisz.astra.Astra;
import lol.jisz.astra.utils.ConfigManager;

import java.util.HashMap;
import java.util.Map;

public class Implements {

    private static Astra plugin;
    private static final Map<Class<? extends Module>, Module> modules = new HashMap<>();
    private static ConfigManager configManager;

    /**
     * Initializes the module system
     * @param plugin Plugin instance
     */
    public static void init(Astra plugin) {
        Implements.plugin = plugin;
        Implements.configManager = new ConfigManager(plugin);
    }

    /**
     * Registers a module
     * @param module Module to register
     * @param <T> Module type
     * @return Instance of the registered module
     */
    @SuppressWarnings("all")
    public static <T extends Module> T register(T module) {
        modules.put(module.getClass(), module);
        return module;
    }

    /**
     * Retrieves a registered module
     * @param clazz Module class
     * @param <T> Module type
     * @return Instance of the module
     */
    @SuppressWarnings("unchecked")
    public static <T> T fetch(Class<T> clazz) {
        return (T) modules.get(clazz);
    }

    /**
     * Enables all registered modules.
     * This method iterates through all registered modules and calls their enable() method.
     * If a module fails to enable, an error is logged but the process continues for other modules.
     */
    public static void enableAll() {
        for (Module module : modules.values()) {
            try {
                module.enable();
                plugin.logger().info("Module enabled: " + module.getClass().getSimpleName());
            } catch (Exception e) {
                plugin.logger().error("Error enabling module: " + module.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Disables all registered modules.
     * This method iterates through all registered modules and calls their disable() method.
     * If a module fails to disable, an error is logged but the process continues for other modules.
     */
    public static void disableAll() {
        for (Module module : modules.values()) {
            try {
                module.disable();
                plugin.logger().info("Module disabled: " + module.getClass().getSimpleName());
            } catch (Exception e) {
                plugin.logger().error("Error disabling module: " + module.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Reloads all registered modules.
     * This method iterates through all registered modules and calls their reload() method.
     * If a module fails to reload, an error is logged but the process continues for other modules.
     */
    public static void reloadAll() {
        for (Module module : modules.values()) {
            try {
                module.reload();
                plugin.logger().info("Module reloaded: " + module.getClass().getSimpleName());
            } catch (Exception e) {
                plugin.logger().error("Error reloading module: " + module.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Checks if a module is registered
     * @param clazz Module class
     * @return true if the module is registered, false otherwise
     */
    public static boolean isRegistered(Class<? extends Module> clazz) {
        return modules.containsKey(clazz);
    }

    /**
     * Retrieves the configuration manager
     * @return Instance of the ConfigManager
     */
    public static ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Retrieves the plugin associated with the module system
     * @return Plugin instance
     */
    public static Astra getPlugin() {
        return plugin;
    }
}