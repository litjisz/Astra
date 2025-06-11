package lol.jisz.astra.utils;

import lol.jisz.astra.api.Implements;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Utility class for sending titles and action bars to players.
 * Provides methods for creating and sending titles with various customization options.
 */
public class Title {

    private static final int DEFAULT_FADE_IN = 10;
    private static final int DEFAULT_STAY = 70;
    private static final int DEFAULT_FADE_OUT = 20;

    /**
     * Sends a title and subtitle to a player with default timing values.
     *
     * @param player The player to send the title to
     * @param title The main title text (can include color codes)
     * @param subtitle The subtitle text (can include color codes)
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    /**
     * Sends a title and subtitle to a player with custom timing values.
     *
     * @param player The player to send the title to
     * @param title The main title text (can include color codes)
     * @param subtitle The subtitle text (can include color codes)
     * @param fadeIn Time in ticks for the title to fade in
     * @param stay Time in ticks for the title to stay on screen
     * @param fadeOut Time in ticks for the title to fade out
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        
        String coloredTitle = Text.colorize(title);
        String coloredSubtitle = Text.colorize(subtitle);
        
        try {
            player.sendTitle(coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut);
        } catch (Exception e) {
            Implements.getPlugin().logger().error("Failed to send title to player " + player.getName(), e);
        }
    }

    /**
     * Sends a title and subtitle with placeholders to a player with default timing values.
     *
     * @param player The player to send the title to
     * @param title The main title text (can include color codes and placeholders)
     * @param subtitle The subtitle text (can include color codes and placeholders)
     */
    public static void sendTranslatedTitle(Player player, String title, String subtitle) {
        sendTranslatedTitle(player, title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    /**
     * Sends a title and subtitle with placeholders to a player with custom timing values.
     *
     * @param player The player to send the title to
     * @param title The main title text (can include color codes and placeholders)
     * @param subtitle The subtitle text (can include color codes and placeholders)
     * @param fadeIn Time in ticks for the title to fade in
     * @param stay Time in ticks for the title to stay on screen
     * @param fadeOut Time in ticks for the title to fade out
     */
    public static void sendTranslatedTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        
        String translatedTitle = Text.translate(title, player);
        String translatedSubtitle = Text.translate(subtitle, player);
        
        try {
            player.sendTitle(translatedTitle, translatedSubtitle, fadeIn, stay, fadeOut);
        } catch (Exception e) {
            Implements.getPlugin().logger().error("Failed to send translated title to player " + player.getName(), e);
        }
    }

    /**
     * Sends a title and subtitle to multiple players with default timing values.
     *
     * @param players Collection of players to send the title to
     * @param title The main title text (can include color codes)
     * @param subtitle The subtitle text (can include color codes)
     */
    public static void sendTitleToAll(Collection<? extends Player> players, String title, String subtitle) {
        sendTitleToAll(players, title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    /**
     * Sends a title and subtitle to multiple players with custom timing values.
     *
     * @param players Collection of players to send the title to
     * @param title The main title text (can include color codes)
     * @param subtitle The subtitle text (can include color codes)
     * @param fadeIn Time in ticks for the title to fade in
     * @param stay Time in ticks for the title to stay on screen
     * @param fadeOut Time in ticks for the title to fade out
     */
    public static void sendTitleToAll(Collection<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (players == null || players.isEmpty()) return;
        
        String coloredTitle = Text.colorize(title);
        String coloredSubtitle = Text.colorize(subtitle);
        
        for (Player player : players) {
            try {
                player.sendTitle(coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut);
            } catch (Exception e) {
                Implements.getPlugin().logger().error("Failed to send title to player " + player.getName(), e);
            }
        }
    }

    /**
     * Sends a title and subtitle with placeholders to multiple players with default timing values.
     * Each player will see the placeholders replaced with their own values.
     *
     * @param players Collection of players to send the title to
     * @param title The main title text (can include color codes and placeholders)
     * @param subtitle The subtitle text (can include color codes and placeholders)
     */
    public static void sendTranslatedTitleToAll(Collection<? extends Player> players, String title, String subtitle) {
        sendTranslatedTitleToAll(players, title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    /**
     * Sends a title and subtitle with placeholders to multiple players with custom timing values.
     * Each player will see the placeholders replaced with their own values.
     *
     * @param players Collection of players to send the title to
     * @param title The main title text (can include color codes and placeholders)
     * @param subtitle The subtitle text (can include color codes and placeholders)
     * @param fadeIn Time in ticks for the title to fade in
     * @param stay Time in ticks for the title to stay on screen
     * @param fadeOut Time in ticks for the title to fade out
     */
    public static void sendTranslatedTitleToAll(Collection<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (players == null || players.isEmpty()) return;
        
        for (Player player : players) {
            sendTranslatedTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Sends a title and subtitle to all online players with default timing values.
     *
     * @param title The main title text (can include color codes)
     * @param subtitle The subtitle text (can include color codes)
     */
    public static void broadcastTitle(String title, String subtitle) {
        broadcastTitle(title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    /**
     * Sends a title and subtitle to all online players with custom timing values.
     *
     * @param title The main title text (can include color codes)
     * @param subtitle The subtitle text (can include color codes)
     * @param fadeIn Time in ticks for the title to fade in
     * @param stay Time in ticks for the title to stay on screen
     * @param fadeOut Time in ticks for the title to fade out
     */
    public static void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        sendTitleToAll(Bukkit.getOnlinePlayers(), title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Sends a title and subtitle with placeholders to all online players with default timing values.
     * Each player will see the placeholders replaced with their own values.
     *
     * @param title The main title text (can include color codes and placeholders)
     * @param subtitle The subtitle text (can include color codes and placeholders)
     */
    public static void broadcastTranslatedTitle(String title, String subtitle) {
        broadcastTranslatedTitle(title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    /**
     * Sends a title and subtitle with placeholders to all online players with custom timing values.
     * Each player will see the placeholders replaced with their own values.
     *
     * @param title The main title text (can include color codes and placeholders)
     * @param subtitle The subtitle text (can include color codes and placeholders)
     * @param fadeIn Time in ticks for the title to fade in
     * @param stay Time in ticks for the title to stay on screen
     * @param fadeOut Time in ticks for the title to fade out
     */
    public static void broadcastTranslatedTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        sendTranslatedTitleToAll(Bukkit.getOnlinePlayers(), title, subtitle, fadeIn, stay, fadeOut);
    }
}