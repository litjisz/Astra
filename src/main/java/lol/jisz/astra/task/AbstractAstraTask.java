package lol.jisz.astra.task;

import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base implementation of the AstraTask interface that provides common functionality
 * for task management in the Astra task system. This class handles task lifecycle, dependencies,
 * priority management, and error handling.
 */
public abstract class AbstractAstraTask implements AstraTask {

    protected final String id;
    protected BukkitTask bukkitTask;
    protected TaskPriority priority;
    protected final Set<String> dependencies;
    protected long timeoutMillis;
    protected Runnable onCompleteAction;
    protected ErrorHandler errorHandler;
    protected final AtomicBoolean cancelled;
    protected final AtomicBoolean running;
    protected final AtomicBoolean completed;
    
    /**
     * Creates a new task with a randomly generated UUID as its identifier.
     */
    public AbstractAstraTask() {
        this(UUID.randomUUID().toString());
    }
    
    /**
     * Creates a new task with the specified identifier.
     *
     * @param id The unique identifier for this task
     */
    public AbstractAstraTask(String id) {
        this.id = id;
        this.priority = TaskPriority.NORMAL;
        this.dependencies = new HashSet<>();
        this.timeoutMillis = 0;
        this.cancelled = new AtomicBoolean(false);
        this.running = new AtomicBoolean(false);
        this.completed = new AtomicBoolean(false);
    }

    /**
     * Returns the unique identifier of this task.
     *
     * @return The task's unique identifier
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Returns the underlying Bukkit task if this task has been scheduled.
     *
     * @return The Bukkit task, or null if not scheduled
     */
    @Override
    public BukkitTask getBukkitTask() {
        return bukkitTask;
    }

    /**
     * Attempts to cancel this task if it's not already cancelled or completed.
     *
     * @return true if the task was cancelled, false if it was already cancelled or completed
     */
    @Override
    public boolean cancel() {
        if (cancelled.get() || completed.get()) {
            return false;
        }

        cancelled.set(true);
        if (bukkitTask != null) {
            bukkitTask.cancel();
        }
        return true;
    }

    /**
     * Checks if this task has been cancelled.
     *
     * @return true if the task has been cancelled, false otherwise
     */
    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Checks if this task is currently scheduled and waiting to run or running.
     *
     * @return true if the task is scheduled and not cancelled or completed
     */
    @Override
    public boolean isScheduled() {
        return bukkitTask != null && !cancelled.get() && !completed.get();
    }

    /**
     * Checks if this task is currently running.
     *
     * @return true if the task is currently executing, false otherwise
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Checks if this task has completed execution.
     *
     * @return true if the task has completed, false otherwise
     */
    @Override
    public boolean isCompleted() {
        return completed.get();
    }

    /**
     * Gets the priority level of this task.
     *
     * @return The task's priority
     */
    @Override
    public TaskPriority getPriority() {
        return priority;
    }

    /**
     * Sets the priority level for this task.
     *
     * @param priority The priority to set
     * @return This task instance for method chaining
     */
    @Override
    public AstraTask setPriority(TaskPriority priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Gets a copy of the set of task IDs that this task depends on.
     *
     * @return A new set containing the IDs of tasks that must complete before this one
     */
    @Override
    public Set<String> getDependencies() {
        return new HashSet<>(dependencies);
    }

    /**
     * Adds a dependency to this task. The task will not run until all dependencies have completed.
     *
     * @param taskId The ID of the task to add as a dependency
     * @return This task instance for method chaining
     */
    @Override
    public AstraTask addDependency(String taskId) {
        if (taskId != null && !taskId.isEmpty()) {
            dependencies.add(taskId);
        }
        return this;
    }

    /**
     * Removes a dependency from this task.
     *
     * @param taskId The ID of the task to remove from dependencies
     * @return This task instance for method chaining
     */
    @Override
    public AstraTask removeDependency(String taskId) {
        dependencies.remove(taskId);
        return this;
    }

    /**
     * Sets a timeout for this task. If the task does not complete within the specified time,
     * it may be cancelled or marked as failed depending on implementation.
     *
     * @param timeout The timeout duration
     * @param unit The time unit of the timeout parameter
     * @return This task instance for method chaining
     */
    @Override
    public AstraTask setTimeout(long timeout, TimeUnit unit) {
        this.timeoutMillis = unit.toMillis(timeout);
        return this;
    }

    /**
     * Sets an action to run when this task completes successfully.
     *
     * @param runnable The action to run on completion
     * @return This task instance for method chaining
     */
    @Override
    public AstraTask onComplete(Runnable runnable) {
        this.onCompleteAction = runnable;
        return this;
    }

    /**
     * Sets an error handler to handle any exceptions thrown during task execution.
     *
     * @param errorHandler The error handler to use
     * @return This task instance for method chaining
     */
    @Override
    public AstraTask onError(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }
    
    /**
     * Marks this task as currently running.
     * Should be called at the beginning of task execution.
     */
    protected void markAsRunning() {
        running.set(true);
    }
    
    /**
     * Marks this task as completed and executes the completion callback if one is set.
     * Any exceptions in the completion callback are passed to the error handler.
     */
    protected void markAsCompleted() {
        running.set(false);
        completed.set(true);
        if (onCompleteAction != null) {
            try {
                onCompleteAction.run();
            } catch (Exception e) {
                handleError(e);
            }
        }
    }
    
    /**
     * Handles an error that occurred during task execution by passing it to the
     * registered error handler if one exists.
     *
     * @param throwable The error that occurred
     */
    protected void handleError(Throwable throwable) {
        if (errorHandler != null) {
            errorHandler.handle(throwable);
        }
    }
}