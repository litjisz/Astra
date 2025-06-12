package lol.jisz.astra.command.adapter;

import lol.jisz.astra.command.sender.Sender;
import lol.jisz.astra.command.sender.SenderCommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Adapter that converts a SenderCommandExecutor to a standard Bukkit CommandExecutor
 */
public class CommandExecutorAdapter implements CommandExecutor {
    private final SenderCommandExecutor executor;

    public CommandExecutorAdapter(SenderCommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return executor.onCommand(Sender.from(sender), command, label, args);
    }
}