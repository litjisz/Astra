package lol.jisz.astra.command;

import lol.jisz.astra.Astra;
import org.bukkit.command.PluginCommand;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

    private final Astra plugin;
    private final List<CommandBase> commands;

    /**
     * Constructor
     * @param plugin Plugin instance
     */
    public CommandManager(Astra plugin) {
        this.plugin = plugin;
        this.commands = new ArrayList<>();
    }

    /**
     * Registers a command in the plugin
     * @param command Command to register
     */
    public void registerCommand(CommandBase command) {
        commands.add(command);

        PluginCommand pluginCommand = plugin.getCommand(command.getName());

        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
            if (command.getAliases() != null && !command.getAliases().isEmpty()) {
                pluginCommand.setAliases(command.getAliases());
            }
            plugin.logger().info("Command registered: " + command.getName());
        } else {
            plugin.logger().warning("Could not register the command: " + command.getName() +
                    ". Make sure it is defined in plugin.yml");
        }
    }

    /**
     * Unregisters a command from the plugin
     * @param command Command to unregister
     */
    public void unregisterCommand(CommandBase command) {
        commands.remove(command);
        PluginCommand pluginCommand = plugin.getCommand(command.getName());
        if (pluginCommand != null) {
            pluginCommand.setExecutor(null);
            pluginCommand.setTabCompleter(null);
            plugin.logger().info("Command unregistered: " + command.getName());
        } else {
            plugin.logger().warning("Could not unregister the command: " + command.getName() +
                    ". Make sure it is defined in plugin.yml");
        }
    }

    /**
     * Gets all registered commands
     * @return List of registered commands
     */
    public List<CommandBase> getCommands() {
        return commands;
    }

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