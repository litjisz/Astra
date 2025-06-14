package lol.jisz.astra.api;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.interfaces.ModuleLifecycleListener;
import lol.jisz.astra.api.module.Module;
import lol.jisz.astra.utils.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Interface for classes that can fetch instances of other classes.
 * This is a shorthand for the {@link Implements} class, providing a more convenient way to access its methods.
 */
public interface Implementer {

    /**
     * Fetches an instance of the specified class.
     * This method is a shorthand for {@link Implements#fetch(Class)}.
     *
     * @param clazz The class to fetch an instance of
     * @param <T>   The type of the class
     * @return An instance of the specified class
     */
    default <T> T fetch(Class<T> clazz) {
        return Implements.fetch(clazz);
    }

    /**
     * Fetches an instance of the specified class using an identifier.
     * This method is a shorthand for {@link Implements#fetch(Class, String)}.
     *
     * @param clazz      The class to fetch an instance of
     * @param identifier The identifier to use for fetching
     * @param <T>        The type of the class
     * @return An instance of the specified class identified by the given identifier
     */
    default <T> T fetch(Class<T> clazz, String identifier) {
        return Implements.fetch(clazz, identifier);
    }

    /**
     * Registers a module in the system.
     * This method is a shorthand for {@link Implements#register(Module)}.
     *
     * @param module The module to register
     * @param <T>    The type of the module
     * @return An instance of the registered module
     */
    default <T extends Module> T registerModule(T module) {
        return Implements.register(module);
    }

    /**
     * Checks if an instance of the specified class exists.
     *
     * @param clazz The class to check for existence
     * @return True if an instance of the specified class exists, false otherwise
     */
    default boolean isRegistered(Class<? extends Module> clazz) {
        return Implements.isRegistered(clazz);
    }

    /**
     * Adds a lifecycle listener to the module.
     * This method is a shorthand for {@link Implements#addLifecycleListener(ModuleLifecycleListener)}.
     *
     * @param listener The listener to add
     */
    default void addLifecycleListener(ModuleLifecycleListener listener) {
        Implements.addLifecycleListener(listener);
    }

    /**
     * Removes a lifecycle listener.
     *
     * @param listener The listener to remove
     */
    default void removeLifecycleListener(ModuleLifecycleListener listener) {
        Implements.removeLifecycleListener(listener);
    }

    /**
     * Gets the main plugin instance.
     * This method is a shorthand for {@link Implements#getPlugin()}.
     *
     * @return The main plugin instance
     */
    default Astra getPlugin() {
        return Implements.getPlugin();
    }

    /**
     * Gets the configuration manager.
     * This method is a shorthand for {@link Implements#getConfigManager()}.
     *
     * @return The configuration manager instance
     */
    default ConfigManager getConfig()  {
        return Implements.getConfigManager();
    }

    /**
     * Gets a specific configuration file by its name.
     *
     * @param fileName The name of the configuration file
     * @return The FileConfiguration instance for the specified file
     */
    default FileConfiguration getConfig(String fileName) {
        return Implements.fetch(FileConfiguration.class, fileName);
    }
}