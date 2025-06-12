package lol.jisz.astra.api;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.interfaces.Module;
import lol.jisz.astra.utils.ConfigManager;
import lol.jisz.astra.utils.Logger;

public abstract class AbstractModule implements Module {

    private final Astra plugin;
    private final ConfigManager config;

    /**
     * Constructor
     * @param plugin Instance of the main plugin
     */
    public AbstractModule(Astra plugin) {
        this.plugin = plugin;
        this.config = Implements.getConfigManager();
    }

    public AbstractModule() {
        this.plugin = Implements.getPlugin();
        this.config = Implements.getConfigManager();
    }

    @Override
    public void enable() {
        // Default empty implementation
    }

    @Override
    public void disable() {
        // Default empty implementation
    }

    @Override
    public void reload() {
        // Default empty implementation
    }

    /**
     * Gets the instance of the main plugin
     * @return Plugin instance
     */
    public Astra getPlugin() {
        return plugin;
    }

    /**
     * Gets the name of the module
     * @return Module name
     */
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Gets the config manager
     * @return Config manager instance
     */
    public ConfigManager getConfigManager() {
        return config;
    }
    
    /**
     * Gets the logger instance for convenient logging from modules
     * @return Logger instance
     */
    public Logger logger() {
        return plugin.logger();
    }
}