package lol.jisz.astra.utils;

import lol.jisz.astra.Astra;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private final Astra plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;
    private final Logger logger;

    /**
     * Constructor
     * @param plugin Plugin instance
     */
    public ConfigManager(Astra plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
        this.logger = plugin.logger();

        loadDefaultConfig();
    }

    private void loadDefaultConfig() {
        plugin.saveDefaultConfig();
        configs.put("config", plugin.getConfig());
        configFiles.put("config", new File(plugin.getDataFolder(), "config.yml"));
    }

    /**
     * Creates and loads a configuration file
     * @param name File name without extension
     * @return Loaded FileConfiguration
     */
    public FileConfiguration loadConfig(String name) {
        return loadConfig(name, true);
    }

    /**
     * Creates and loads a configuration file
     * @param name File name without extension
     * @param createIfNotExists Whether to create the file if it does not exist
     * @return Loaded FileConfiguration
     */
    public FileConfiguration loadConfig(String name, boolean createIfNotExists) {
        File file = new File(plugin.getDataFolder(), name + ".yml");

        if (!file.exists() && createIfNotExists) {
            try {
                if (plugin.getResource(name + ".yml") != null) {
                    plugin.saveResource(name + ".yml", false);

                    if (logger.isDebugMode()) {
                        logger.info("Configuration file extracted from resources: " + name + ".yml");
                    }
                } else {
                    if (file.getParentFile() != null) {
                        file.getParentFile().mkdirs();
                    }
                    if (file.createNewFile()) {
                        if (logger.isDebugMode()) {
                            logger.info("Configuration file created: " + name + ".yml");
                        }
                    }
                }
            } catch (IOException ex) {
                logger.error("Could not create the file: " + name + ".yml", ex);
                return null;
            }
        }

        if (!file.exists()) {
            logger.warning("Could not find the file: " + name + ".yml");
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        InputStream defaultStream = plugin.getResource(name + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        configs.put(name, config);
        configFiles.put(name, file);

        return config;
    }

    /**
     * Gets a loaded configuration
     * @param name Configuration name
     * @return FileConfiguration or null if it does not exist
     */
    public FileConfiguration getConfig(String name) {
        if (configs.containsKey(name)) {
            return configs.get(name);
        }

        return loadConfig(name);
    }

    /**
     * Saves a configuration
     * @param name Configuration name
     */
    public void saveConfig(String name) {
        if (!configs.containsKey(name) || !configFiles.containsKey(name)) {
            logger.warning("Cannot save configuration: " + name + " (does not exist)");
            return;
        }

        try {
            configs.get(name).save(configFiles.get(name));

            if (logger.isDebugMode()) {
                logger.debug("Configuration saved: " + name);
            }
        } catch (IOException e) {
            logger.error("Error saving configuration: " + name, e);
        }
    }

    /**
     * Checks if a configuration exists
     * @param name Configuration name
     * @return true if it exists, false otherwise
     */
    public boolean configExists(String name) {
        return configs.containsKey(name) || new File(plugin.getDataFolder(), name + ".yml").exists();
    }

    /**
     * Creates a new configuration file
     *
     * @param name Configuration name
     */
    public void createFileConfiguration(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                if (file.createNewFile()) {
                    if (logger.isDebugMode()) {
                        logger.info("Configuration file created: " + name + ".yml");
                    }
                }
            } catch (IOException ex) {
                logger.error("Could not create the file: " + name + ".yml", ex);
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(name, config);
        configFiles.put(name, file);
    }

    /**
     * Reloads a configuration
     * @param name Configuration name
     * @return Reloaded FileConfiguration
     */
    public FileConfiguration reloadConfig(String name) {
        if (!configFiles.containsKey(name)) {
            return loadConfig(name);
        }

        if (name.equals("config")) {
            plugin.reloadConfig();
            configs.put("config", plugin.getConfig());
            return plugin.getConfig();
        }

        File file = configFiles.get(name);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStream = plugin.getResource(name + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        configs.put(name, config);

        return config;
    }

    public void reloadAllConfigs() {
        for (String name : configs.keySet()) {
            reloadConfig(name);
        }

        if (logger.isDebugMode()) {
            logger.info("All configurations have been reloaded");
        }
    }

    public void saveAllConfigs() {
        for (String name : configs.keySet()) {
            saveConfig(name);
        }

        if (logger.isDebugMode()) {
            logger.info("All configurations have been saved");
        }
    }

    /**
     * Gets a value with a default value
     * @param config Configuration name
     * @param path Value path
     * @param defaultValue Default value
     * @param <T> Value type
     * @return Retrieved value or default value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String config, String path, T defaultValue) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig == null) {
            return defaultValue;
        }

        if (fileConfig.contains(path)) {
            return (T) fileConfig.get(path, defaultValue);
        }
        set(config, path, defaultValue);
        return defaultValue;
    }

    /**
     * Sets a value in a configuration
     * @param config Configuration name
     * @param path Value path
     * @param value Value to set
     */
    public void set(String config, String path, Object value) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig != null) {
            fileConfig.set(path, value);
        }
    }

    /**
     * Creates a configuration section if it does not exist
     * @param config Configuration name
     * @param path Section path
     * @return Created or existing ConfigurationSection
     */
    public ConfigurationSection createSection(String config, String path) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig == null) {
            return null;
        }

        if (fileConfig.contains(path) && fileConfig.isConfigurationSection(path)) {
            return fileConfig.getConfigurationSection(path);
        }
        return fileConfig.createSection(path);
    }

    /**
     * Checks if a configuration exists
     * @param name Configuration name
     * @return true if it exists, false otherwise
     */
    public boolean exists(String name) {
        return configs.containsKey(name) || new File(plugin.getDataFolder(), name + ".yml").exists();
    }

    /**
     * Deletes a configuration
     * @param name Configuration name
     * @return true if successfully deleted, false otherwise
     */
    public boolean deleteConfig(String name) {
        if (name.equals("config")) {
            logger.warning("Cannot delete the main configuration");
            return false;
        }

        configs.remove(name);
        File file = configFiles.remove(name);

        if (file != null && file.exists()) {
            return file.delete();
        }

        return false;
    }

    /**
     * Gets all top-level keys of a configuration
     * @param config Configuration name
     * @return Set of keys or null if the configuration does not exist
     */
    public Set<String> getKeys(String config) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig == null) {
            return null;
        }

        return fileConfig.getKeys(false);
    }

    /**
     * Gets all keys of a section
     * @param config Configuration name
     * @param path Section path
     * @param deep Whether to include nested keys
     * @return Set of keys or null if the section does not exist
     */
    public Set<String> getKeys(String config, String path, boolean deep) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig == null || !fileConfig.contains(path) || !fileConfig.isConfigurationSection(path)) {
            return null;
        }

        return fileConfig.getConfigurationSection(path).getKeys(deep);
    }

    /**
     * Gets a list of strings
     * @param config Configuration name
     * @param path List path
     * @return List of strings or null if it does not exist
     */
    public List<String> getStringList(String config, String path) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig == null || !fileConfig.contains(path)) {
            return null;
        }

        return fileConfig.getStringList(path);
    }

    /**
     * Gets a string
     * @param config Configuration name
     * @param path Value path
     * @param defaultValue Default value
     * @return Retrieved string or default value
     */
    public String getString(String config, String path, String defaultValue) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig == null) {
            return defaultValue;
        }

        return fileConfig.getString(path, defaultValue);
    }

    /**
     * Gets an int
     * @param config Configuration name
     * @param path Value path
     * @param defaultValue Default value
     * @return Retrieved int or default value
     */
    public int getInt(String config, String path, int defaultValue) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig == null) {
            return defaultValue;
        }

        return fileConfig.getInt(path, defaultValue);
    }

    /**
     * Gets a double value from the specified configuration.
     * 
     * @param config The name of the configuration file to retrieve the value from
     * @param path The path to the value within the configuration
     * @param defaultValue The default value to return if the path doesn't exist or the configuration can't be loaded
     * @return The double value at the specified path, or the default value if not found
     */
    public double getDouble(String config, String path, double defaultValue) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig == null) {
            return defaultValue;
        }

        return fileConfig.getDouble(path, defaultValue);
    }

    /**
     * Gets a boolean value from the specified configuration.
     *
     * @param config The name of the configuration file to retrieve the value from
     * @param path The path to the value within the configuration
     * @param defaultValue The default value to return if the path doesn't exist or the configuration can't be loaded
     * @return The boolean value at the specified path, or the default value if not found
     */
    public boolean getBoolean(String config, String path, boolean defaultValue) {
        FileConfiguration fileConfig = getConfig(config);
        if (fileConfig == null) {
            return defaultValue;
        }

        return fileConfig.getBoolean(path, defaultValue);
    }
}