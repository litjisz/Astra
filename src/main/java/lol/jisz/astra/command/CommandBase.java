package lol.jisz.astra.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandBase implements CommandExecutor, TabCompleter {

    private final String name;
    private final String permission;
    private final List<String> aliases;
    private final boolean playerOnly;

    /**
     * Base constructor for commands
     * @param name Command name
     * @param permission Permission required to execute the command
     * @param playerOnly If the command can only be executed by players
     */
    public CommandBase(String name, String permission, boolean playerOnly) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.aliases = new ArrayList<>();
    }

    /**
     * Base constructor for commands with aliases
     * @param name Command name
     * @param permission Permission required to execute the command
     * @param playerOnly If the command can only be executed by players
     * @param aliases Command aliases
     */
    public CommandBase(String name, String permission, boolean playerOnly, List<String> aliases) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.aliases = aliases;
    }

    /**
     * Executes the command
     * @param sender Command sender
     * @param label Label used
     * @param args Command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    public abstract boolean execute(CommandSender sender, String label, String[] args);

    /**
     * Handles the command execution after checking permissions and player-only restrictions.
     * This method is called by Bukkit when a player executes a command registered to this class.
     * 
     * @param sender The entity that executed the command (player, console, etc.)
     * @param command The command that was executed
     * @param label The alias of the command that was used
     * @param args The arguments passed to the command
     * @return true if the command was handled, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (playerOnly && !(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("§cThis command can only be executed by players.");
            return true;
        }

        if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage("§cYou do not have permission to execute this command.");
            return true;
        }

        return execute(sender, label, args);
    }

    /**
     * Provides tab completion options for this command.
     * This method is called by Bukkit when a player attempts to tab-complete this command.
     * The default implementation returns an empty list. Override this method to provide
     * custom tab completion options.
     * 
     * @param sender The entity requesting tab completion
     * @param command The command being tab-completed
     * @param alias The alias being used for the command
     * @param args The arguments passed to the command so far
     * @return A list of possible tab completions
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }

    /**
     * Gets the command name
     * @return Command name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the permission required to execute the command
     * @return Required permission
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Gets the command aliases
     * @return Command aliases
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * Checks if the command can only be executed by players
     * @return true if it can only be executed by players, false otherwise
     */
    public boolean isPlayerOnly() {
        return playerOnly;
    }
}