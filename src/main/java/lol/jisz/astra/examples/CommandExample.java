package lol.jisz.astra.examples;

import lol.jisz.astra.command.AutoRegisterCommand;
import lol.jisz.astra.command.CommandBase;
import org.bukkit.command.CommandSender;

/**
 * Example command class demonstrating the usage of the command system.
 * This class is automatically registered as a command through the {@link AutoRegisterCommand} annotation.
 *
 * <p>The command is configured with the name "name", requires the "permission" permission,
 * can only be executed by players, and has aliases "alias1" and "alias2".</p>
 */
@AutoRegisterCommand(
        name = "name",
        permission = "permission",
        playerOnly = true,
        aliases = {"alias1", "alias2"}
)
public class CommandExample extends CommandBase {

    /**
     * Constructs a new CommandExample instance.
     * Initializes the command with the name "name", permission "permission", and restricts it to players only.
     */
    public CommandExample() {
        super("name", "permission", true);
    }

    /**
     * Executes the command logic when the command is invoked.
     *
     * @param sender The entity (player, console, etc.) that executed the command
     * @param label The alias of the command that was used
     * @param args An array of arguments passed to the command
     * @return true if the command executed successfully, false otherwise
     */
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        return false;
    }
}