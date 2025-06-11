package lol.jisz.astra.utils;

import lol.jisz.astra.api.Implements;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// TODO: In the official release, this class should be changed to use AstraTasks for
// better task management and better compatibility with the overall system.
// It is not used yet because I have to make some changes in AstraTask.

/**
 * Utility class for sending action bar messages to players.
 * Provides methods for creating and sending action bars with various customization options.
 */
public class ActionBar {

    private static final Map<UUID, BukkitTask> persistentBars = new HashMap<>();
    private static final int DEFAULT_DURATION = 60;
    private static final int DEFAULT_UPDATE_INTERVAL = 20;

    /**
     * Sends an action bar message to a player.
     *
     * @param player The player to send the action bar to
     * @param message The message to display (can include color codes)
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        
        String coloredMessage = Text.colorize(message);
        
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredMessage));
        } catch (Exception e) {
            Implements.getPlugin().logger().error("Failed to send action bar to player " + player.getName(), e);
        }
    }

    /**
     * Sends an action bar message with placeholders to a player.
     *
     * @param player The player to send the action bar to
     * @param message The message to display (can include color codes and placeholders)
     */
    public static void sendTranslatedActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        
        String translatedMessage = Text.translate(message, player);
        
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(translatedMessage));
        } catch (Exception e) {
            Implements.getPlugin().logger().error("Failed to send translated action bar to player " + player.getName(), e);
        }
    }

    /**
     * Sends an action bar message to a player for a specific duration.
     *
     * @param player The player to send the action bar to
     * @param message The message to display (can include color codes)
     * @param durationTicks How long to display the message in ticks (20 ticks = 1 second)
     */
    public static void sendTemporaryActionBar(Player player, String message, int durationTicks) {
        if (player == null || !player.isOnline()) return;
        
        cancelPersistentActionBar(player);
        sendActionBar(player, message);
        
        BukkitTask task = new BukkitRunnable() {
            private int counter = 0;
            
            @Override
            public void run() {
                counter += 2;
                if (counter >= durationTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                sendActionBar(player, message);
            }
        }.runTaskTimerAsynchronously(Implements.getPlugin(), 2L, 2L);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                task.cancel();
            }
        }.runTaskLaterAsynchronously(Implements.getPlugin(), durationTicks);
    }

    /**
     * Sends an action bar message with placeholders to a player for a specific duration.
     *
     * @param player The player to send the action bar to
     * @param message The message to display (can include color codes and placeholders)
     * @param durationTicks How long to display the message in ticks (20 ticks = 1 second)
     */
    public static void sendTemporaryTranslatedActionBar(Player player, String message, int durationTicks) {
        if (player == null || !player.isOnline()) return;
        
        cancelPersistentActionBar(player);
        sendTranslatedActionBar(player, message);
        
        BukkitTask task = new BukkitRunnable() {
            private int counter = 0;
            
            @Override
            public void run() {
                counter += 2;
                if (counter >= durationTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                sendTranslatedActionBar(player, message);
            }
        }.runTaskTimerAsynchronously(Implements.getPlugin(), 2L, 2L);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                task.cancel();
            }
        }.runTaskLaterAsynchronously(Implements.getPlugin(), durationTicks);
    }

    /**
     * Sends a persistent action bar message to a player that stays until canceled.
     *
     * @param player The player to send the action bar to
     * @param message The message to display (can include color codes)
     * @return The BukkitTask handling this persistent action bar
     */
    public static BukkitTask sendPersistentActionBar(Player player, String message) {
        return sendPersistentActionBar(player, message, DEFAULT_UPDATE_INTERVAL);
    }

    /**
     * Sends a persistent action bar message to a player that stays until canceled,
     * with a custom update interval.
     *
     * @param player The player to send the action bar to
     * @param message The message to display (can include color codes)
     * @param updateIntervalTicks How often to refresh the action bar in ticks
     * @return The BukkitTask handling this persistent action bar
     */
    public static BukkitTask sendPersistentActionBar(Player player, String message, int updateIntervalTicks) {
        if (player == null) return null;
        
        cancelPersistentActionBar(player);
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    persistentBars.remove(player.getUniqueId());
                    return;
                }
                sendActionBar(player, message);
            }
        }.runTaskTimerAsynchronously(Implements.getPlugin(), 0L, Math.max(1, updateIntervalTicks));
        
        persistentBars.put(player.getUniqueId(), task);
        
        return task;
    }

    /**
     * Sends a persistent action bar message with placeholders to a player that stays until canceled.
     *
     * @param player The player to send the action bar to
     * @param message The message to display (can include color codes and placeholders)
     * @return The BukkitTask handling this persistent action bar
     */
    public static BukkitTask sendPersistentTranslatedActionBar(Player player, String message) {
        return sendPersistentTranslatedActionBar(player, message, DEFAULT_UPDATE_INTERVAL);
    }

    /**
     * Sends a persistent action bar message with placeholders to a player that stays until canceled,
     * with a custom update interval.
     *
     * @param player The player to send the action bar to
     * @param message The message to display (can include color codes and placeholders)
     * @param updateIntervalTicks How often to refresh the action bar in ticks
     * @return The BukkitTask handling this persistent action bar
     */
    public static BukkitTask sendPersistentTranslatedActionBar(Player player, String message, int updateIntervalTicks) {
        if (player == null) return null;
        
        cancelPersistentActionBar(player);
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    persistentBars.remove(player.getUniqueId());
                    return;
                }
                sendTranslatedActionBar(player, message);
            }
        }.runTaskTimerAsynchronously(Implements.getPlugin(), 0L, Math.max(1, updateIntervalTicks));
        
        persistentBars.put(player.getUniqueId(), task);
        
        return task;
    }

    /**
     * Cancels a persistent action bar for a player.
     *
     * @param player The player whose persistent action bar should be canceled
     */
    public static void cancelPersistentActionBar(Player player) {
        if (player == null) return;
        
        BukkitTask task = persistentBars.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Sends an action bar message to multiple players.
     *
     * @param players Collection of players to send the action bar to
     * @param message The message to display (can include color codes)
     */
    public static void sendActionBarToAll(Collection<? extends Player> players, String message) {
        if (players == null || players.isEmpty()) return;
        
        String coloredMessage = Text.colorize(message);
        
        for (Player player : players) {
            try {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredMessage));
            } catch (Exception e) {
                Implements.getPlugin().logger().error("Failed to send action bar to player " + player.getName(), e);
            }
        }
    }

    /**
     * Sends an action bar message with placeholders to multiple players.
     * Each player will see the placeholders replaced with their own values.
     *
     * @param players Collection of players to send the action bar to
     * @param message The message to display (can include color codes and placeholders)
     */
    public static void sendTranslatedActionBarToAll(Collection<? extends Player> players, String message) {
        if (players == null || players.isEmpty()) return;
        
        for (Player player : players) {
            sendTranslatedActionBar(player, message);
        }
    }

    /**
     * Sends an action bar message to all online players.
     *
     * @param message The message to display (can include color codes)
     */
    public static void broadcastActionBar(String message) {
        sendActionBarToAll(Bukkit.getOnlinePlayers(), message);
    }
}