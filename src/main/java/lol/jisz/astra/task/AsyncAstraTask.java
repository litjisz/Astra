package lol.jisz.astra.task;

import lol.jisz.astra.Astra;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents an asynchronous task in the Astra plugin framework.
 * This class extends AbstractAstraTask to provide asynchronous execution capabilities
 * using Bukkit's scheduler.
 */
public class AsyncAstraTask extends AbstractAstraTask {

    private final Runnable runnable;
    private final Astra plugin;

    /**
     * Constructs a new asynchronous task with a generated ID.
     *
     * @param plugin   The Astra plugin instance that owns this task
     * @param runnable The runnable code to execute asynchronously
     */
    public AsyncAstraTask(Astra plugin, Runnable runnable) {
        super();
        this.plugin = plugin;
        this.runnable = runnable;
    }

    /**
     * Constructs a new asynchronous task with a specified ID.
     *
     * @param plugin   The Astra plugin instance that owns this task
     * @param id       The unique identifier for this task
     * @param runnable The runnable code to execute asynchronously
     */
    public AsyncAstraTask(Astra plugin, String id, Runnable runnable) {
        super(id);
        this.plugin = plugin;
        this.runnable = runnable;
    }

    /**
     * Executes this task asynchronously immediately.
     * If the task is already scheduled, completed, or cancelled, this method has no effect.
     *
     * @return This task instance for method chaining
     */
    public AsyncAstraTask execute() {
        if (isScheduled() || isCompleted() || isCancelled()) {
            return this;
        }

        bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
     * Schedules this task to execute asynchronously after a specified delay.
     * If the task is already scheduled, completed, or cancelled, this method has no effect.
     *
     * @param delayTicks The delay in server ticks before executing the task
     * @return This task instance for method chaining
     */
    public AsyncAstraTask executeDelayed(long delayTicks) {
        if (isScheduled() || isCompleted() || isCancelled()) {
            return this;
        }

        bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
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
     * Schedules this task to execute asynchronously repeatedly at fixed intervals.
     * If the task is already scheduled, completed, or cancelled, this method has no effect.
     *
     * @param delayTicks  The delay in server ticks before the first execution
     * @param periodTicks The period in server ticks between consecutive executions
     * @return This task instance for method chaining
     */
    public AsyncAstraTask executeRepeating(long delayTicks, long periodTicks) {
        if (isScheduled() || isCompleted() || isCancelled()) {
            return this;
        }

        bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
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

    /**
     * Registers an action to run on the main server thread after this task completes.
     *
     * @param syncAction The action to run synchronously after task completion
     * @return This task instance for method chaining
     */
    public AsyncAstraTask thenRunSync(Runnable syncAction) {
        return (AsyncAstraTask) onComplete(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    syncAction.run();
                } catch (Throwable t) {
                    handleError(t);
                }
            });
        });
    }

    /**
     * Chains another AsyncAstraTask to execute after this task completes.
     *
     * @param nextTask The next task to execute after this one completes
     * @return The next task instance for further chaining
     */
    public AsyncAstraTask thenRun(AsyncAstraTask nextTask) {
        onComplete(() -> {
            if (!nextTask.isCancelled() && !nextTask.isCompleted()) {
                nextTask.execute();
            }
        });
        return nextTask;
    }

    /**
     * Creates and chains a new asynchronous task to execute after this task completes.
     *
     * @param asyncAction The action to run asynchronously after this task completes
     * @return The newly created task instance for further chaining
     */
    public AsyncAstraTask thenRunAsync(Runnable asyncAction) {
        AsyncAstraTask nextTask = new AsyncAstraTask(plugin, asyncAction);
        return thenRun(nextTask);
    }

    /**
     * Sets a timeout for this task, after which it will be cancelled if not completed.
     * Overrides the parent method to provide asynchronous timeout handling.
     *
     * @param timeout The maximum time to allow the task to run
     * @param unit    The time unit of the timeout parameter
     * @return This task instance for method chaining
     */
    @Override
    public AsyncAstraTask setTimeout(long timeout, TimeUnit unit) {
        super.setTimeout(timeout, unit);
        
        if (timeout > 0) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                if (isRunning() && !isCompleted() && !isCancelled()) {
                    cancel();
                    handleError(new TimeoutException("The asynchronous task " + getId() + " exceeded the time limit of " + timeout + " " + unit.name().toLowerCase()));
                }
            }, Math.max(1, unit.toSeconds(timeout) * 20));
        }
        
        return this;
    }

    /**
     * Exception thrown when a task exceeds its specified timeout.
     */
    public static class TimeoutException extends RuntimeException {
        /**
         * Constructs a new timeout exception with the specified detail message.
         *
         * @param message The detail message
         */
        public TimeoutException(String message) {
            super(message);
        }
    }

    /**
     * Creates a new task that executes the specified action on the main server thread
     * while blocking the current asynchronous thread until completion.
     * This is useful for accessing Bukkit API methods that require synchronous execution.
     *
     * @param bukkitAction The action to run on the main server thread
     * @return A new AsyncAstraTask that will execute the wrapped action
     */
    public AsyncAstraTask withBukkitContext(Runnable bukkitAction) {
        Runnable wrappedRunnable = () -> {
            try {
                Object lock = new Object();
                AtomicReference<Throwable> error = new AtomicReference<>();

                synchronized (lock) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            bukkitAction.run();
                        } catch (Throwable t) {
                            error.set(t);
                        } finally {
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    });

                    lock.wait();
                }

                if (error.get() != null) {
                    throw error.get();
                }
            } catch (Throwable t) {
                handleError(t);
            }
        };

        return new AsyncAstraTask(plugin, getId(), wrappedRunnable);
    }
}