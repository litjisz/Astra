package lol.jisz.astra.command;

import lol.jisz.astra.Astra;
import lol.jisz.astra.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages commands for the plugin
 */
public class CommandManager {
    private final Astra plugin;
    private final List<CommandBase> commands;
    private final Logger logger;
    private CommandMap commandMap;

    /**
     * Constructor
     * @param plugin Plugin instance
     */
    public CommandManager(Astra plugin) {
        this.plugin = plugin;
        this.commands = new ArrayList<>();
        this.logger = plugin.logger();

        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            this.commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Failed to access Bukkit CommandMap", e);
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Registers a command in the plugin
     * @param command Command to register
     */
    public void registerCommand(CommandBase command) {
        if (command == null) {
            logger.warning("Attempted to register a null command");
            return;
        }

        try {
            if (commandMap != null) {
                org.bukkit.command.PluginCommand pluginCommand = plugin.getServer().getPluginCommand(command.getName());
                
                if (pluginCommand == null) {
                    try {
                        Constructor<org.bukkit.command.PluginCommand> constructor =
                            org.bukkit.command.PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                        constructor.setAccessible(true);
                        pluginCommand = constructor.newInstance(command.getName(), plugin);
                        
                        pluginCommand.setDescription("Command provided by " + plugin.getName());
                        pluginCommand.setUsage("/" + command.getName());
                        if (command.getAliases() != null) {
                            pluginCommand.setAliases(command.getAliases());
                        }
                        if (command.getPermission() != null) {
                            pluginCommand.setPermission(command.getPermission());
                        }
                        
                        pluginCommand.setExecutor(command);
                        pluginCommand.setTabCompleter(command);
                        
                        commandMap.register(plugin.getName().toLowerCase(), pluginCommand);
                        commands.add(command);

                        if (logger.isDebugMode()) {
                            logger.info("Registered command: " + command.getName());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to create plugin command for " + command.getName(), e);
                    }
                } else {
                    pluginCommand.setExecutor(command);
                    pluginCommand.setTabCompleter(command);
                    commands.add(command);

                    if (logger.isDebugMode()) {
                        logger.info("Registered existing command: " + command.getName());
                    }
                }
            } else {
                logger.error("Cannot register command " + command.getName() + " - CommandMap is null");
            }
        } catch (Exception e) {
            logger.error("Error registering command: " + command.getName(), e);
        }
    }

    /**
     * Unregisters a command from the plugin
     * @param command Command to unregister
     */
    public void unregisterCommand(CommandBase command) {
        if (command == null) {
            logger.warning("Attempted to unregister a null command");
            return;
        }

        try {
            command.unregister(commandMap);
            commands.remove(command);

            if (logger.isDebugMode()) {
                logger.info("Unregistered command: " + command.getName());
            }
        } catch (Exception e) {
            logger.error("Error unregistering command: " + command.getName(), e);
        }
    }

    /**
     * Gets all registered commands
     * @return List of registered commands
     */
    public List<CommandBase> getCommands() {
        return new ArrayList<>(commands);
    }

    /**
     * Gets a command by its name
     * @param name Name of the command to get
     * @return CommandBase instance if found, null otherwise
     */
    public CommandBase getCommand(String name) {
        for (CommandBase command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Checks if a command with the given name is registered
     * @param name Name of the command to check
     * @return true if a command with that name is registered, false otherwise
     */
    public boolean isRegistered(String name) {
        for (CommandBase command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the plugin associated with the command manager
     * @return Plugin instance
     */
    public Astra getPlugin() {
        return plugin;
    }
}