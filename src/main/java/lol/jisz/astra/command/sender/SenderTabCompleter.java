package lol.jisz.astra.command.sender;

import org.bukkit.command.Command;

import java.util.List;

/**
 * Interface for tab completers that use the Sender wrapper
 */
public interface SenderTabCompleter {
    /**
     * Provides tab completion options for a command
     * @param sender The wrapped command sender
     * @param command The command being tab-completed
     * @param alias The alias being used for the command
     * @param args The arguments passed to the command so far
     * @return A list of possible tab completions
     */
    List<String> onTabComplete(Sender sender, Command command, String alias, String[] args);
}