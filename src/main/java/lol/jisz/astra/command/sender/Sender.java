package lol.jisz.astra.command.sender;

import lol.jisz.astra.Astra;
import lol.jisz.astra.utils.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Enhanced wrapper for Bukkit's CommandSender that provides additional functionality
 * and integrates with Astra's command framework.
 */
public record Sender(CommandSender sender) {
    /**
     * Creates a new Sender instance wrapping the given CommandSender
     *
     * @param sender The CommandSender to wrap
     */
    public Sender {
    }

    /**
     * Gets the underlying CommandSender
     *
     * @return The wrapped CommandSender
     */
    @Override
    public CommandSender sender() {
        return sender;
    }

    /**
     * Checks if the sender is a player
     *
     * @return true if the sender is a player, false otherwise
     */
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    /**
     * Checks if the sender is the console
     *
     * @return true if the sender is the console, false otherwise
     */
    public boolean isConsole() {
        return sender instanceof ConsoleCommandSender;
    }

    /**
     * Gets the player if the sender is a player
     *
     * @return The player, or null if the sender is not a player
     */
    public Player getPlayer() {
        return isPlayer() ? (Player) sender : null;
    }

    /**
     * Gets the player's UUID if the sender is a player
     *
     * @return The player's UUID, or null if the sender is not a player
     */
    public UUID getUUID() {
        return isPlayer() ? getPlayer().getUniqueId() : null;
    }

    /**
     * Gets the sender's name
     *
     * @return The sender's name
     */
    public String getName() {
        return sender.getName();
    }

    /**
     * Checks if the sender has a specific permission
     *
     * @param permission The permission to check
     * @return true if the sender has the permission, false otherwise
     */
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    /**
     * Checks if the sender has any of the specified permissions
     *
     * @param permissions The permissions to check
     * @return true if the sender has any of the permissions, false otherwise
     */
    public boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the sender has all of the specified permissions
     *
     * @param permissions The permissions to check
     * @return true if the sender has all of the permissions, false otherwise
     */
    public boolean hasAllPermissions(String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sends a message to the sender
     *
     * @param message The message to send
     */
    public void send(String message) {
        sender.sendMessage(Text.colorize(message));
    }

    /**
     * Sends multiple messages to the sender
     *
     * @param messages The messages to send
     */
    public void send(String... messages) {
        for (String message : messages) {
            send(message);
        }
    }

    /**
     * Sends a list of messages to the sender
     *
     * @param messages The messages to send
     */
    public void send(List<String> messages) {
        for (String message : messages) {
            send(message);
        }
    }

    /**
     * Sends a message to the sender with a prefix
     *
     * @param prefix  The prefix to use
     * @param message The message to send
     */
    public void sendPrefixed(String prefix, String message) {
        send(prefix + " " + message);
    }

    /**
     * Sends a message with a replaceable placeholder
     *
     * @param message     The message with placeholders
     * @param placeholder The placeholder to replace
     * @param replacement The replacement value
     */
    public void sendReplaced(String message, String placeholder, String replacement) {
        send(message.replace(placeholder, replacement));
    }

    /**
     * Sends a message with multiple replaceable placeholders
     *
     * @param message      The message with placeholders
     * @param replacements The placeholders and their replacements (placeholder1, replacement1, placeholder2, replacement2, ...)
     */
    public void sendReplaced(String message, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be in pairs");
        }

        String result = message;
        for (int i = 0; i < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }

        send(result);
    }

    /**
     * Executes a command as the sender
     *
     * @param command The command to execute
     * @return true if the command was executed successfully, false otherwise
     */
    public boolean executeCommand(String command) {
        return Astra.getInstance().getServer().dispatchCommand(sender, command);
    }

    /**
     * Executes a player-only action if the sender is a player
     *
     * @param playerAction The action to execute if the sender is a player
     * @return true if the action was executed, false if the sender is not a player
     */
    public boolean ifPlayer(Consumer<Player> playerAction) {
        if (isPlayer()) {
            playerAction.accept(getPlayer());
            return true;
        }
        return false;
    }

    /**
     * Gives an item to the player if the sender is a player
     *
     * @param item The item to give
     * @return true if the item was given, false if the sender is not a player
     */
    public boolean giveItem(ItemStack item) {
        return ifPlayer(player -> player.getInventory().addItem(item));
    }

    /**
     * Teleports the player to a location if the sender is a player
     *
     * @param location The location to teleport to
     * @return true if the player was teleported, false if the sender is not a player
     */
    public boolean teleport(org.bukkit.Location location) {
        return ifPlayer(player -> player.teleport(location));
    }

    /**
     * Creates a new Sender instance from a CommandSender
     *
     * @param sender The CommandSender to wrap
     * @return A new Sender instance
     */
    public static Sender from(CommandSender sender) {
        return new Sender(sender);
    }

    /**
     * Creates a new Sender instance from a Player
     *
     * @param player The Player to wrap
     * @return A new Sender instance
     */
    public static Sender from(Player player) {
        return new Sender(player);
    }

    /**
     * Creates a new Sender instance for the console
     *
     * @return A new Sender instance for the console
     */
    public static Sender console() {
        return new Sender(Astra.getInstance().getServer().getConsoleSender());
    }
}