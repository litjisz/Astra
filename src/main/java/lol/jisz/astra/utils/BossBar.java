package lol.jisz.astra.utils;

import lol.jisz.astra.api.Implements;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

// TODO: In the official release, this class should be changed to use AstraTasks for
// better task management and better compatibility with the overall system.
// It is not used yet because I have to make some changes in AstraTask.

/**
 * Utility class for creating and managing boss bars.
 * Provides methods for creating, displaying, and manipulating boss bars with various customization options.
 */
public class BossBar {

    private static final Map<String, org.bukkit.boss.BossBar> ACTIVE_BARS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> PLAYER_BARS = new ConcurrentHashMap<>();
    private static final Map<String, BukkitTask> BAR_TASKS = new ConcurrentHashMap<>();

    /**
     * Creates a new boss bar with the specified title and default settings.
     *
     * @param id Unique identifier for the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @return The created boss bar
     */
    public static org.bukkit.boss.BossBar create(String id, String title) {
        return create(id, title, BarColor.PURPLE, BarStyle.SOLID);
    }

    /**
     * Creates a new boss bar with custom color and style.
     *
     * @param id Unique identifier for the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param color Color of the boss bar
     * @param style Style of the boss bar
     * @param flags Optional flags to apply to the boss bar
     * @return The created boss bar
     */
    public static org.bukkit.boss.BossBar create(String id, String title, BarColor color, BarStyle style, BarFlag... flags) {
        remove(id);
        
        String coloredTitle = Text.colorize(title);
        org.bukkit.boss.BossBar bar = Bukkit.createBossBar(coloredTitle, color, style, flags);
        ACTIVE_BARS.put(id, bar);
        return bar;
    }

    /**
     * Shows a boss bar to a specific player.
     *
     * @param id Identifier of the boss bar
     * @param player Player to show the boss bar to
     * @return true if the boss bar was shown, false if it doesn't exist
     */
    public static boolean show(String id, Player player) {
        org.bukkit.boss.BossBar bar = ACTIVE_BARS.get(id);
        if (bar == null || player == null) return false;
        
        bar.addPlayer(player);
        
        PLAYER_BARS.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(id);
        return true;
    }

    /**
     * Shows a boss bar to multiple players.
     *
     * @param id Identifier of the boss bar
     * @param players Collection of players to show the boss bar to
     * @return true if the boss bar was shown, false if it doesn't exist
     */
    public static boolean showToAll(String id, Collection<? extends Player> players) {
        if (players == null || players.isEmpty()) return false;
        
        boolean result = true;
        for (Player player : players) {
            if (!show(id, player)) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Shows a boss bar to all online players.
     *
     * @param id Identifier of the boss bar
     * @return true if the boss bar was shown, false if it doesn't exist
     */
    public static boolean broadcast(String id) {
        return showToAll(id, Bukkit.getOnlinePlayers());
    }

    /**
     * Hides a boss bar from a specific player.
     *
     * @param id Identifier of the boss bar
     * @param player Player to hide the boss bar from
     * @return true if the boss bar was hidden, false if it doesn't exist
     */
    public static boolean hide(String id, Player player) {
        org.bukkit.boss.BossBar bar = ACTIVE_BARS.get(id);
        if (bar == null || player == null) return false;
        
        bar.removePlayer(player);
        
        Set<String> playerBars = PLAYER_BARS.get(player.getUniqueId());
        if (playerBars != null) {
            playerBars.remove(id);
            if (playerBars.isEmpty()) {
                PLAYER_BARS.remove(player.getUniqueId());
            }
        }
        return true;
    }

    /**
     * Hides a boss bar from all players it's currently shown to.
     *
     * @param id Identifier of the boss bar
     * @return true if the boss bar was hidden, false if it doesn't exist
     */
    public static boolean hideFromAll(String id) {
        org.bukkit.boss.BossBar bar = ACTIVE_BARS.get(id);
        if (bar == null) return false;
        
        List<Player> players = new ArrayList<>(bar.getPlayers());
        for (Player player : players) {
            hide(id, player);
        }
        return true;
    }

    /**
     * Removes a boss bar completely.
     *
     * @param id Identifier of the boss bar
     * @return true if the boss bar was removed, false if it doesn't exist
     */
    public static boolean remove(String id) {
        org.bukkit.boss.BossBar bar = ACTIVE_BARS.remove(id);
        if (bar == null) return false;
        
        BukkitTask task = BAR_TASKS.remove(id);
        if (task != null) {
            task.cancel();
        }
        
        List<Player> players = new ArrayList<>(bar.getPlayers());
        for (Player player : players) {
            bar.removePlayer(player);
            
            Set<String> playerBars = PLAYER_BARS.get(player.getUniqueId());
            if (playerBars != null) {
                playerBars.remove(id);
                if (playerBars.isEmpty()) {
                    PLAYER_BARS.remove(player.getUniqueId());
                }
            }
        }
        return true;
    }

    /**
     * Updates the title of a boss bar.
     *
     * @param id Identifier of the boss bar
     * @param title New title text (supports color codes)
     * @return true if the title was updated, false if the boss bar doesn't exist
     */
    public static boolean updateTitle(String id, String title) {
        org.bukkit.boss.BossBar bar = ACTIVE_BARS.get(id);
        if (bar == null) return false;
        
        bar.setTitle(Text.colorize(title));
        return true;
    }

    /**
     * Updates the title of a boss bar with placeholders for a specific player.
     *
     * @param id Identifier of the boss bar
     * @param title New title text with placeholders
     * @param player Player for whom to translate placeholders
     * @return true if the title was updated, false if the boss bar doesn't exist
     */
    public static boolean updateTranslatedTitle(String id, String title, Player player) {
        org.bukkit.boss.BossBar bar = ACTIVE_BARS.get(id);
        if (bar == null || player == null) return false;
        
        bar.setTitle(Text.translate(title, player));
        return true;
    }

    /**
     * Updates the progress of a boss bar.
     *
     * @param id Identifier of the boss bar
     * @param progress Progress value between 0.0 and 1.0
     * @return true if the progress was updated, false if the boss bar doesn't exist
     */
    public static boolean updateProgress(String id, double progress) {
        org.bukkit.boss.BossBar bar = ACTIVE_BARS.get(id);
        if (bar == null) return false;
        
        progress = Math.max(0.0, Math.min(1.0, progress));
        bar.setProgress(progress);
        return true;
    }

    /**
     * Updates the color of a boss bar.
     *
     * @param id Identifier of the boss bar
     * @param color New color for the boss bar
     * @return true if the color was updated, false if the boss bar doesn't exist
     */
    public static boolean updateColor(String id, BarColor color) {
        org.bukkit.boss.BossBar bar = ACTIVE_BARS.get(id);
        if (bar == null || color == null) return false;
        
        bar.setColor(color);
        return true;
    }

    /**
     * Updates the style of a boss bar.
     *
     * @param id Identifier of the boss bar
     * @param style New style for the boss bar
     * @return true if the style was updated, false if the boss bar doesn't exist
     */
    public static boolean updateStyle(String id, BarStyle style) {
        org.bukkit.boss.BossBar bar = ACTIVE_BARS.get(id);
        if (bar == null || style == null) return false;
        
        bar.setStyle(style);
        return true;
    }

    /**
     * Creates a temporary boss bar that will automatically disappear after a specified duration.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param durationTicks Duration in ticks before the boss bar disappears
     * @return true if the temporary boss bar was created and shown, false otherwise
     */
    public static boolean showTemporary(String id, String title, Player player, int durationTicks) {
        return showTemporary(id, title, player, durationTicks, BarColor.PURPLE, BarStyle.SOLID);
    }

    /**
     * Creates a temporary boss bar with custom settings that will automatically disappear after a specified duration.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param durationTicks Duration in ticks before the boss bar disappears
     * @param color Color of the boss bar
     * @param style Style of the boss bar
     * @param flags Optional flags to apply to the boss bar
     * @return true if the temporary boss bar was created and shown, false otherwise
     */
    public static boolean showTemporary(String id, String title, Player player, int durationTicks, 
                                       BarColor color, BarStyle style, BarFlag... flags) {
        if (player == null) return false;

        org.bukkit.boss.BossBar bar = create(id, title, color, style, flags);
        show(id, player);
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                remove(id);
            }
        }.runTaskLater(Implements.getPlugin(), durationTicks);
        
        BAR_TASKS.put(id, task);
        return true;
    }

    /**
     * Creates a boss bar with a countdown timer.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param durationTicks Total duration of the countdown in ticks
     * @param onComplete Action to perform when the countdown completes
     * @return true if the countdown boss bar was created and shown, false otherwise
     */
    public static boolean showCountdown(String id, String title, Player player, int durationTicks, Runnable onComplete) {
        return showCountdown(id, title, player, durationTicks, BarColor.GREEN, BarStyle.SOLID, onComplete);
    }

    /**
     * Creates a boss bar with a countdown timer and custom settings.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param durationTicks Total duration of the countdown in ticks
     * @param color Initial color of the boss bar
     * @param style Style of the boss bar
     * @param onComplete Action to perform when the countdown completes
     * @param flags Optional flags to apply to the boss bar
     * @return true if the countdown boss bar was created and shown, false otherwise
     */
    public static boolean showCountdown(String id, String title, Player player, int durationTicks, 
                                       BarColor color, BarStyle style, Runnable onComplete, BarFlag... flags) {
        if (player == null || durationTicks <= 0) return false;
        
        org.bukkit.boss.BossBar bar = create(id, title, color, style, flags);
        show(id, player);
        bar.setProgress(1.0);
        
        BukkitTask task = new BukkitRunnable() {
            private int ticksLeft = durationTicks;
            
            @Override
            public void run() {
                ticksLeft--;
                
                double progress = (double) ticksLeft / durationTicks;
                bar.setProgress(Math.max(0.0, progress));
                
                if (progress <= 0.25) {
                    bar.setColor(BarColor.RED);
                } else if (progress <= 0.5) {
                    bar.setColor(BarColor.YELLOW);
                }
                
                if (ticksLeft <= 0) {
                    remove(id);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    cancel();
                }
            }
        }.runTaskTimer(Implements.getPlugin(), 0, 1);
        
        BAR_TASKS.put(id, task);
        return true;
    }

    /**
     * Creates a boss bar that pulses by changing its progress value.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param pulseTicks Duration of one complete pulse cycle in ticks
     * @return true if the pulsing boss bar was created and shown, false otherwise
     */
    public static boolean showPulsing(String id, String title, Player player, int pulseTicks) {
        return showPulsing(id, title, player, pulseTicks, BarColor.PURPLE, BarStyle.SOLID);
    }

    /**
     * Creates a boss bar that pulses by changing its progress value with custom settings.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param pulseTicks Duration of one complete pulse cycle in ticks
     * @param color Color of the boss bar
     * @param style Style of the boss bar
     * @param flags Optional flags to apply to the boss bar
     * @return true if the pulsing boss bar was created and shown, false otherwise
     */
    public static boolean showPulsing(String id, String title, Player player, int pulseTicks, 
                                     BarColor color, BarStyle style, BarFlag... flags) {
        if (player == null || pulseTicks <= 0) return false;
        
        org.bukkit.boss.BossBar bar = create(id, title, color, style, flags);
        show(id, player);
        
        BukkitTask task = new BukkitRunnable() {
            private double direction = -0.01;
            private double progress = 1.0;
            
            @Override
            public void run() {
                progress += direction;
                
                if (progress >= 1.0 || progress <= 0.0) {
                    direction *= -1;
                }
                
                bar.setProgress(progress);
            }
        }.runTaskTimer(Implements.getPlugin(), 0, pulseTicks / 100);
        
        BAR_TASKS.put(id, task);
        return true;
    }

    /**
     * Creates a boss bar with a countdown timer that performs calculations asynchronously.
     * Note: The actual updates to the boss bar are still performed on the main thread for thread safety.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param durationTicks Total duration of the countdown in ticks
     * @param onComplete Action to perform when the countdown completes
     * @return true if the countdown boss bar was created and shown, false otherwise
     */
    public static boolean showAsyncCountdown(String id, String title, Player player, int durationTicks, Runnable onComplete) {
        return showAsyncCountdown(id, title, player, durationTicks, BarColor.GREEN, BarStyle.SOLID, onComplete);
    }

    /**
     * Creates a boss bar with a countdown timer that performs calculations asynchronously with custom settings.
     * Note: The actual updates to the boss bar are still performed on the main thread for thread safety.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param durationTicks Total duration of the countdown in ticks
     * @param color Initial color of the boss bar
     * @param style Style of the boss bar
     * @param onComplete Action to perform when the countdown completes
     * @param flags Optional flags to apply to the boss bar
     * @return true if the countdown boss bar was created and shown, false otherwise
     */
    public static boolean showAsyncCountdown(String id, String title, Player player, int durationTicks, 
                                   BarColor color, BarStyle style, Runnable onComplete, BarFlag... flags) {
        if (player == null || durationTicks <= 0) return false;
    
        org.bukkit.boss.BossBar bar = create(id, title, color, style, flags);
        show(id, player);
        bar.setProgress(1.0);
    
        final UUID playerUuid = player.getUniqueId();
        final int totalTicks = durationTicks;
    
        AtomicInteger ticksLeft = new AtomicInteger(durationTicks);
    
        BukkitTask asyncTask = new BukkitRunnable() {
            @Override
            public void run() {
                int currentTicks = ticksLeft.decrementAndGet();
                final double progress = (double) currentTicks / totalTicks;
            
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        org.bukkit.boss.BossBar currentBar = ACTIVE_BARS.get(id);
                        Player currentPlayer = Bukkit.getPlayer(playerUuid);
                    
                        if (currentBar == null || currentPlayer == null || !currentPlayer.isOnline()) {
                            remove(id);
                            cancel();
                            return;
                        }
                    
                        currentBar.setProgress(Math.max(0.0, progress));
                    
                        if (progress <= 0.25) {
                            currentBar.setColor(BarColor.RED);
                        } else if (progress <= 0.5) {
                            currentBar.setColor(BarColor.YELLOW);
                        }
                    
                        if (currentTicks <= 0) {
                            remove(id);
                            if (onComplete != null) {
                                onComplete.run();
                            }
                            cancel();
                        }
                    }
                }.runTask(Implements.getPlugin());
            }
        }.runTaskTimerAsynchronously(Implements.getPlugin(), 0, 1);
    
        BAR_TASKS.put(id, asyncTask);
        return true;
    }

    /**
     * Creates a boss bar that pulses asynchronously by changing its progress value.
     * Note: The actual updates to the boss bar are still performed on the main thread for thread safety.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param pulseTicks Duration of one complete pulse cycle in ticks
     * @return true if the pulsing boss bar was created and shown, false otherwise
     */
    public static boolean showAsyncPulsing(String id, String title, Player player, int pulseTicks) {
        return showAsyncPulsing(id, title, player, pulseTicks, BarColor.PURPLE, BarStyle.SOLID);
    }

    /**
     * Creates a boss bar that pulses asynchronously with custom settings.
     * Note: The actual updates to the boss bar are still performed on the main thread for thread safety.
     *
     * @param id Identifier of the boss bar
     * @param title Title text for the boss bar (supports color codes)
     * @param player Player to show the boss bar to
     * @param pulseTicks Duration of one complete pulse cycle in ticks
     * @param color Color of the boss bar
     * @param style Style of the boss bar
     * @param flags Optional flags to apply to the boss bar
     * @return true if the pulsing boss bar was created and shown, false otherwise
     */
    public static boolean showAsyncPulsing(String id, String title, Player player, int pulseTicks, 
                                 BarColor color, BarStyle style, BarFlag... flags) {
        if (player == null || pulseTicks <= 0) return false;
    
        org.bukkit.boss.BossBar bar = create(id, title, color, style, flags);
        show(id, player);
    
        final UUID playerUuid = player.getUniqueId();
    
        AtomicReference<Double> direction = new AtomicReference<>(-0.01);
        AtomicReference<Double> progress = new AtomicReference<>(1.0);
    
        BukkitTask asyncTask = new BukkitRunnable() {
            @Override
            public void run() {
                double currentProgress = progress.get();
                double currentDirection = direction.get();
            
                currentProgress += currentDirection;
            
                if (currentProgress >= 1.0 || currentProgress <= 0.0) {
                    currentDirection *= -1;
                    direction.set(currentDirection);
                }
            
                progress.set(currentProgress);
            
                final double finalProgress = currentProgress;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        org.bukkit.boss.BossBar currentBar = ACTIVE_BARS.get(id);
                        Player currentPlayer = Bukkit.getPlayer(playerUuid);
                    
                        if (currentBar == null || currentPlayer == null || !currentPlayer.isOnline()) {
                            remove(id);
                            cancel();
                            return;
                        }
                    
                        currentBar.setProgress(finalProgress);
                    }
                }.runTask(Implements.getPlugin());
            }
        }.runTaskTimerAsynchronously(Implements.getPlugin(), 0, pulseTicks / 100);
    
        BAR_TASKS.put(id, asyncTask);
        return true;
    }
}