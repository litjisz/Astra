package lol.jisz.astra.command.adapter;

import lol.jisz.astra.command.sender.Sender;
import lol.jisz.astra.command.sender.SenderTabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Adapter that converts a SenderTabCompleter to a standard Bukkit TabCompleter
 */
public class TabCompleterAdapter implements TabCompleter {
    private final SenderTabCompleter completer;

    public TabCompleterAdapter(SenderTabCompleter completer) {
        this.completer = completer;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return completer.onTabComplete(Sender.from(sender), command, alias, args);
    }
}