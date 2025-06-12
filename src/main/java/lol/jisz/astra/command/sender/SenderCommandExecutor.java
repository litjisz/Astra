package lol.jisz.astra.command.sender;

import org.bukkit.command.Command;

/**
 * Interface for command executors that use the Sender wrapper
 */
public interface SenderCommandExecutor {
    /**
     * Executes the command
     * @param sender The wrapped command sender
     * @param command The command that was executed
     * @param label The alias of the command that was used
     * @param args The arguments passed to the command
     * @return true if the command was executed successfully, false otherwise
     */
    boolean onCommand(Sender sender, Command command, String label, String[] args);
}