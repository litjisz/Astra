package lol.jisz.astra.command.impl;

import lol.jisz.astra.api.Implements;
import lol.jisz.astra.command.CommandBase;
import lol.jisz.astra.task.TaskManager;
import lol.jisz.astra.task.TaskPriority;
import lol.jisz.astra.utils.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AstraCommand extends CommandBase {

    public AstraCommand() {
        super("astra", "astra.command", false);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            List<String> messages = Arrays.asList(
                    Text.colorize(" "),
                    Text.gradient("☽ Astra", "9863E7", "C69FFF") +
                    Text.colorize(" &8| &fFramwork built with &c❤ &fby &Astra Team"),
                    Text.colorize(" "),
                    Text.center("&eThis plugin is developed with Astra Framework"),
                    Text.center("&eA framework to optimize plugin development"),
                    Text.colorize(" ")
            );

            messages.forEach(sender::sendMessage);
        } else {
            handleSubcommands(sender, args);
        }
        return true;
    }

    private void handleSubcommands(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "github":
                    sendGithubMessage(sender);
                    break;
                case "tasks":
                    sendTasksMessage(sender);
                    break;
                default:
                    sender.sendMessage(Text.colorize("&c ✘ Unknown command: &n" + subcommand));
                    sender.sendMessage(Text.colorize("&eThe available subcommands are &ngithub&e and &ntasks&e."));
            }
        } else {
            sender.sendMessage(Text.colorize("&cPlease provide a subcommand."));
        }
    }

    private void sendGithubMessage(CommandSender sender) {
        List<String> githubMessages = Arrays.asList(
                Text.colorize(" "),
                Text.gradient("☽ Astra", "9863E7", "C69FFF") +
                Text.colorize(" &8| &fFramwork built with &c❤ &fby &Astra Team"),
                Text.colorize(" "),
                Text.colorize("&8 ⏩ &fRepository: &bhttps://github.com/litjisz/Astra"),
                Text.colorize("&8 ⚠ &fReport issues: &chttps://github.com/litjisz/Astra/issues"),
                Text.colorize("&8 ✎ &fWiki: &dhttps://github.com/litjisz/Astra/wiki"),
                Text.colorize(" ")
        );

        githubMessages.forEach(sender::sendMessage);
    }

    private void sendTasksMessage(CommandSender sender) {
        Map<String, Object> stats = Implements.fetch(TaskManager.class).getStatistics();
        
        int completedTasks = (int) stats.get("completedTasks");
        int failedTasks = (int) stats.get("failedTasks");
        int pendingTasks = (int) stats.get("pendingTasks");
        int runningAsyncTasks = (int) stats.get("runningAsyncTasks");
        int maxConcurrentAsyncTasks = (int) stats.get("maxConcurrentAsyncTasks");
        String serverLoad = (String) stats.get("serverLoad");
        Map<TaskPriority, Integer> tasksByPriority = (Map<TaskPriority, Integer>) stats.get("tasksByPriority");
        
        List<String> tasksMessages = Arrays.asList(
                Text.colorize(" "),
                Text.gradient("☽ Astra", "9863E7", "C69FFF") +
                Text.colorize(" &8| &fTask Manager Statistics"),
                Text.colorize(" "),
                Text.colorize("&8 ⏩ &fServer Load: &b" + serverLoad),
                Text.colorize("&8 ☑ &fCompleted Tasks: &a" + completedTasks),
                Text.colorize("&8 ☒ &fFailed Tasks: &c" + failedTasks),
                Text.colorize("&8 ⏳ &fPending Tasks: &e" + pendingTasks),
                Text.colorize("&8 ⚡ &fRunning Async Tasks: &d" + runningAsyncTasks),
                Text.colorize("&8 ⏶ &fMax Concurrent Async Tasks: &6" + maxConcurrentAsyncTasks),
                Text.colorize(" "),
                Text.colorize("&e ⚑ &lTasks by Priority:"),
                Text.colorize("&8 ⚠ &fCritical: &c" + getTaskCountByPriority(tasksByPriority, TaskPriority.CRITICAL)),
                Text.colorize("&8 ▲ &fHigh: &6" + getTaskCountByPriority(tasksByPriority, TaskPriority.HIGH)),
                Text.colorize("&8 ● &fNormal: &e" + getTaskCountByPriority(tasksByPriority, TaskPriority.NORMAL)),
                Text.colorize("&8 ▼ &fLow: &a" + getTaskCountByPriority(tasksByPriority, TaskPriority.LOW)),
                Text.colorize("&8 ◆ &fMinimal: &b" + getTaskCountByPriority(tasksByPriority, TaskPriority.MINIMAL)),
                Text.colorize(" ")
        );

        tasksMessages.forEach(sender::sendMessage);
    }

    private int getTaskCountByPriority(Map<TaskPriority, Integer> tasksByPriority, TaskPriority priority) {
        return tasksByPriority.getOrDefault(priority, 0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] subcommands = {"github", "tasks"};
            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        }

        return completions;
    }
}