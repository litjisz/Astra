package lol.jisz.astra.examples;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.AbstractModule;
import lol.jisz.astra.api.Implements;
import lol.jisz.astra.utils.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Example class demonstrating configuration management in the Astra framework.
 * This class shows how to load, access, and save both main and custom configuration files.
 */
public class ConfigExample extends AbstractModule {

    private ConfigManager configManager;

    /**
     * Constructs a new ConfigExample module.
     *
     * @param plugin The main Astra plugin instance that this module belongs to
     */
    public ConfigExample(Astra plugin) {
        super(plugin);
        this.configManager = Implements.getConfigManager();
    }

    /**
     * Enables the module and demonstrates configuration operations.
     * This method shows how to:
     * 1. Load and access values from the main configuration
     * 2. Create and load a custom configuration file
     * 3. Set default values in a configuration if they don't exist
     * 4. Access and use configuration values
     */
    @Override
    public void enable() {
        // Example 1: Load and access the main configuration
        FileConfiguration mainConfig = configManager.getConfig("config");
        String serverName = mainConfig.getString("server-name", "My Server");
        logger().info("Server name: " + serverName);

        // Example 2: Create and load a custom configuration
        FileConfiguration customConfig = configManager.loadConfig("players");

        // Set some default values if it is a new file
        if (!customConfig.contains("default-group")) {
            customConfig.set("default-group", "user");
            customConfig.set("max-homes", 3);
            configManager.saveConfig("players");
        }

        String defaultGroup = customConfig.getString("default-group");
        int maxHomes = customConfig.getInt("max-homes");

        logger().info("Default group: " + defaultGroup);
        logger().info("Maximum homes: " + maxHomes);
    }

    /**
     * Disables the module and performs cleanup operations.
     * This method ensures all configuration changes are saved when the module is disabled.
     */
    @Override
    public void disable() {
        // Save all configurations when disabling
        configManager.saveAllConfigs();
    }
}