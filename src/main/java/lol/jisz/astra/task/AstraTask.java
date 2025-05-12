package lol.jisz.astra.task;

import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Represents a task that can be scheduled and managed within the Astra task system.
 * This interface provides methods for task identification, state management, dependency handling,
 * timeout configuration, and completion/error callbacks.
 */
public interface AstraTask {
    /**
     * Gets the unique identifier for this task.
     *
     * @return The task's unique identifier
     */
    String getId();
    
    /**
     * Gets the underlying Bukkit task associated with this Astra task.
     *
     * @return The Bukkit task instance
     */
    BukkitTask getBukkitTask();
    
    /**
     * Attempts to cancel this task.
     *
     * @return true if the task was successfully cancelled, false otherwise
     */
    boolean cancel();
    
    /**
     * Checks if this task has been cancelled.
     *
     * @return true if the task is cancelled, false otherwise
     */
    boolean isCancelled();
    
    /**
     * Checks if this task has been scheduled for execution.
     *
     * @return true if the task is scheduled, false otherwise
     */
    boolean isScheduled();
    
    /**
     * Checks if this task is currently running.
     *
     * @return true if the task is running, false otherwise
     */
    boolean isRunning();
    
    /**
     * Checks if this task has completed its execution.
     *
     * @return true if the task has completed, false otherwise
     */
    boolean isCompleted();
    
    /**
     * Gets the priority level of this task.
     *
     * @return The task's priority
     */
    TaskPriority getPriority();
    
    /**
     * Sets the priority level for this task.
     *
     * @param priority The priority to set for this task
     * @return This task instance for method chaining
     */
    AstraTask setPriority(TaskPriority priority);
    
    /**
     * Gets the set of task IDs that this task depends on.
     *
     * @return A set of task IDs representing dependencies
     */
    Set<String> getDependencies();
    
    /**
     * Adds a dependency to this task.
     *
     * @param taskId The ID of the task to add as a dependency
     * @return This task instance for method chaining
     */
    AstraTask addDependency(String taskId);
    
    /**
     * Removes a dependency from this task.
     *
     * @param taskId The ID of the task to remove from dependencies
     * @return This task instance for method chaining
     */
    AstraTask removeDependency(String taskId);
    
    /**
     * Sets a timeout for this task's execution.
     *
     * @param timeout The timeout duration
     * @param unit The time unit for the timeout duration
     * @return This task instance for method chaining
     */
    AstraTask setTimeout(long timeout, TimeUnit unit);
    
    /**
     * Sets a callback to be executed when this task completes successfully.
     *
     * @param runnable The callback to execute on completion
     * @return This task instance for method chaining
     */
    AstraTask onComplete(Runnable runnable);
    
    /**
     * Sets an error handler to be called if this task encounters an error.
     *
     * @param errorHandler The handler for task errors
     * @return This task instance for method chaining
     */
    AstraTask onError(ErrorHandler errorHandler);

    /**
     * Functional interface for handling errors that occur during task execution.
     */
    @FunctionalInterface
    interface ErrorHandler {
        /**
         * Handles an error that occurred during task execution.
         *
         * @param throwable The throwable representing the error
         */
        void handle(Throwable throwable);
    }
}