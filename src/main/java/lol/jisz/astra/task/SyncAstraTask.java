package lol.jisz.astra.task;

import lol.jisz.astra.Astra;
import org.bukkit.Bukkit;

/**
 * A synchronous task implementation for the Astra plugin.
 * This class handles tasks that need to be executed on the main server thread.
 */
public class SyncAstraTask extends AbstractAstraTask {

    private final Runnable runnable;
    private final Astra plugin;

    /**
     * Constructs a new synchronous task with the given plugin and runnable.
     *
     * @param plugin   The Astra plugin instance
     * @param runnable The task to be executed
     */
    public SyncAstraTask(Astra plugin, Runnable runnable) {
        super();
        this.plugin = plugin;
        this.runnable = runnable;
    }

    /**
     * Constructs a new synchronous task with the given plugin, ID, and runnable.
     *
     * @param plugin   The Astra plugin instance
     * @param id       The unique identifier for this task
     * @param runnable The task to be executed
     */
    public SyncAstraTask(Astra plugin, String id, Runnable runnable) {
        super(id);
        this.plugin = plugin;
        this.runnable = runnable;
    }

    /**
     * Executes the task immediately on the main server thread.
     * If the task is already scheduled, completed, or cancelled, this method has no effect.
     *
     * @return This task instance for method chaining
     */
    public SyncAstraTask execute() {
        if (isScheduled() || isCompleted() || isCancelled()) {
            return this;
        }

        bukkitTask = Bukkit.getScheduler().runTask(plugin, () -> {
            if (cancelled.get()) return;

            try {
                markAsRunning();
                runnable.run();
                markAsCompleted();
            } catch (Throwable t) {
                handleError(t);
            }
        });

        return this;
    }

    /**
     * Schedules the task to be executed after a specified delay on the main server thread.
     * If the task is already scheduled, completed, or cancelled, this method has no effect.
     *
     * @param delayTicks The number of server ticks to wait before executing the task
     * @return This task instance for method chaining
     */
    public SyncAstraTask executeDelayed(long delayTicks) {
        if (isScheduled() || isCompleted() || isCancelled()) {
            return this;
        }

        bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (cancelled.get()) return;

            try {
                markAsRunning();
                runnable.run();
                markAsCompleted();
            } catch (Throwable t) {
                handleError(t);
            }
        }, delayTicks);

        return this;
    }

    /**
     * Schedules the task to be executed repeatedly on the main server thread.
     * The task will first run after the specified delay and then repeatedly at the specified interval.
     * If the task is already scheduled, completed, or cancelled, this method has no effect.
     *
     * @param delayTicks  The number of server ticks to wait before the first execution
     * @param periodTicks The number of server ticks between subsequent executions
     * @return This task instance for method chaining
     */
    public SyncAstraTask executeRepeating(long delayTicks, long periodTicks) {
        if (isScheduled() || isCompleted() || isCancelled()) {
            return this;
        }

        bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (cancelled.get()) {
                if (bukkitTask != null) {
                    bukkitTask.cancel();
                }
                return;
            }

            try {
                markAsRunning();
                runnable.run();
                running.set(false);
            } catch (Throwable t) {
                handleError(t);
                if (bukkitTask != null) {
                    bukkitTask.cancel();
                }
            }
        }, delayTicks, periodTicks);

        return this;
    }
}