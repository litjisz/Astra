package lol.jisz.astra.utils;

import lol.jisz.astra.Astra;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.logging.Level;

/**
 * A utility class for handling plugin logging with enhanced formatting and color support.
 * This logger provides various methods for sending formatted messages to the console,
 * players, or broadcasting to all players. It supports colored messages, debug mode,
 * and custom prefixes.
 * <p>
 * The logger can be configured to use colors or not, and can be set to debug mode
 * to display additional information when needed.
 */
public class Logger {

    private final Astra plugin;
    private String prefix;
    private boolean useColors;
    private boolean debugMode;

    /**
     * Constructor with plugin and custom prefix
     *
     * @param plugin Plugin instance
     * @param prefix Prefix for messages (can include color codes)
     */
    public Logger(Astra plugin, String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;
        this.useColors = true;
        this.debugMode = false;
    }

    /**
     * Constructor with plugin
     *
     * @param plugin Plugin instance
     */
    public Logger(Astra plugin) {
        this.plugin = plugin;
        this.prefix = Text.gradient("Astra", "9863E7", "C69FFF") + " &8| &r";
        this.useColors = true;
        this.debugMode = false;
    }

    /**
     * Sets the prefix for messages
     *
     * @param prefix New prefix (can include color codes)
     * @return Logger instance for chaining
     */
    public Logger setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Enables or disables the use of colors
     *
     * @param useColors true to enable colors, false to disable them
     * @return Logger instance for chaining
     */
    public Logger setUseColors(boolean useColors) {
        this.useColors = useColors;
        return this;
    }

    /**
     * Enables or disables debug mode
     *
     * @param debugMode true to enable, false to disable
     * @return Logger instance for chaining
     */
    public Logger setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    /**
     * Gets the current prefix
     *
     * @return Current prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Formats a message with the prefix and colors if enabled
     *
     * @param message Message to format
     * @return Formatted message
     */
    private String format(String message) {
        String formattedMessage = prefix + message;
        return useColors ? Text.colorize(formattedMessage) : Text.stripColor(formattedMessage);
    }

    /**
     * Sends an informational message to the console
     *
     * @param message Message to send
     */
    public void info(String message) {
        plugin.getLogger().info(useColors ? Text.stripColor(message) : message);

        // Also send to Bukkit console to see colors if available
        if (useColors && Bukkit.getConsoleSender() != null) {
            Bukkit.getConsoleSender().sendMessage(format("&f" + message));
        }
    }

    /**
     * Sends a warning message to the console
     *
     * @param message Message to send
     */
    public void warning(String message) {
        plugin.getLogger().warning(useColors ? Text.stripColor(message) : message);

        if (useColors && Bukkit.getConsoleSender() != null) {
            Bukkit.getConsoleSender().sendMessage(format("&e" + message));
        }
    }

    /**
     * Sends an error message to the console
     *
     * @param message Message to send
     */
    public void error(String message) {
        plugin.getLogger().severe(useColors ? Text.stripColor(message) : message);

        if (useColors && Bukkit.getConsoleSender() != null) {
            Bukkit.getConsoleSender().sendMessage(format("&c" + message));
        }
    }

    /**
     * Sends an error message with an exception to the console
     *
     * @param message Message to send
     * @param throwable Associated exception
     */
    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, useColors ? Text.stripColor(message) : message, throwable);

        if (useColors && Bukkit.getConsoleSender() != null) {
            Bukkit.getConsoleSender().sendMessage(format("&c" + message));
            Bukkit.getConsoleSender().sendMessage(format("&7Cause: &c" + throwable.getMessage()));

            // Show stack trace in the console with colors
            if (debugMode) {
                Bukkit.getConsoleSender().sendMessage(format("&7Stack trace:"));
                Arrays.stream(throwable.getStackTrace())
                        .limit(10) // Limit to 10 lines to avoid console overflow
                        .forEach(element -> Bukkit.getConsoleSender().sendMessage(
                                format("&8  at &7" + element.toString())));
            }
        }
    }

    /**
     * Sends a debug message to the console (only if debug mode is enabled)
     *
     * @param message Message to send
     */
    public void debug(String message) {
        if (!debugMode) return;

        if (useColors && Bukkit.getConsoleSender() != null) {
            Bukkit.getConsoleSender().sendMessage(format("&b[DEBUG] &7" + message));
        } else {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Sends a success message to the console
     *
     * @param message Message to send
     */
    public void success(String message) {
        plugin.getLogger().info(useColors ? Text.stripColor(message) : message);

        if (useColors && Bukkit.getConsoleSender() != null) {
            Bukkit.getConsoleSender().sendMessage(format("&a" + message));
        }
    }

    /**
     * Sends a message to a player or the console
     *
     * @param sender Message recipient (player or console)
     * @param message Message to send
     */
    public void send(CommandSender sender, String message) {
        if (sender instanceof Player) {
            sender.sendMessage(useColors ? Text.colorize(prefix + message) : prefix + message);
        } else {
            info(message);
        }
    }

    /**
     * Sends a message to all players and the console
     *
     * @param message Message to send
     */
    public void broadcast(String message) {
        String formattedMessage = format(message);
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(formattedMessage));
        info(message);
    }

    /**
     * Sends a message to all players with a specific permission and the console
     *
     * @param message Message to send
     * @param permission Permission required to receive the message
     */
    public void broadcast(String message, String permission) {
        String formattedMessage = format(message);
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .forEach(player -> player.sendMessage(formattedMessage));
        info(message);
    }

    /**
     * Creates a nice separator line for logs
     *
     * @param color Line color (without the & symbol)
     * @param length Line length
     * @return Separator line
     */
    public String line(String color, int length) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < length; i++) {
            line.append("-");
        }
        return format("&" + color + line.toString());
    }

    /**
     * Creates a nice separator line for logs with a default length (50)
     *
     * @param color Line color (without the & symbol)
     * @return Separator line
     */
    public String line(String color) {
        return line(color, 50);
    }

    /**
     * Creates a nice header for logs
     *
     * @param title Header title
     * @param color Header color (without the & symbol)
     * @return Formatted header
     */
    public String header(String title, String color) {
        int sideLength = (48 - title.length()) / 2;
        StringBuilder header = new StringBuilder();

        for (int i = 0; i < sideLength; i++) {
            header.append("=");
        }

        header.append(" ").append(title).append(" ");

        while (header.length() < 50) {
            header.append("=");
        }

        return format("&" + color + header.toString());
    }

    /**
     * Creates a nice header for logs with a default color (b - light blue)
     *
     * @param title Header title
     * @return Formatted header
     */
    public String header(String title) {
        return header(title, "b");
    }
}