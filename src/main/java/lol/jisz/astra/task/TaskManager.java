package lol.jisz.astra.task;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.AbstractModule;
import lol.jisz.astra.utils.Logger;
import lol.jisz.astra.utils.Text;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Centralized task manager for the Astra framework.
 * Implements a priority and dependency system for task execution.
 */
public class TaskManager extends AbstractModule {
    private Astra plugin;
    private Logger logger;

    private PriorityBlockingQueue<AstraTask> pendingTasks;

    private Map<String, AstraTask> taskRegistry;

    private AtomicInteger runningAsyncTasks;
    private int maxConcurrentAsyncTasks;

    private Map<TaskPriority, Integer> tasksByPriority;
    private AtomicInteger completedTasks;
    private AtomicInteger failedTasks;

    private ReentrantLock processingLock;

    private double serverLoadThreshold;
    private boolean adaptiveConcurrency;

    @Override
    public void enable() {
        this.plugin = getPlugin();
        this.logger = new Logger(plugin, Text.gradient("AstraTask", "9863E7", "C69FFF") + " &8| &r");

        this.pendingTasks = new PriorityBlockingQueue<>(32,
                (t1, t2) -> Integer.compare(t1.getPriority().getValue(), t2.getPriority().getValue()));
        this.taskRegistry = new ConcurrentHashMap<>();

        this.runningAsyncTasks = new AtomicInteger(0);
        this.maxConcurrentAsyncTasks = 10;

        this.tasksByPriority = new ConcurrentHashMap<>();
        for (TaskPriority priority : TaskPriority.values()) {
            tasksByPriority.put(priority, 0);
        }
        this.completedTasks = new AtomicInteger(0);
        this.failedTasks = new AtomicInteger(0);

        this.processingLock = new ReentrantLock();

        this.serverLoadThreshold = 0.7;
        this.adaptiveConcurrency = true;

        startResourceMonitoring();

        logger.info("Task system initialized with limit of " + maxConcurrentAsyncTasks + " concurrent async tasks");
    }

    @Override
    public void disable() {
        logger.info("Shutting down task system...");
        cancelAllTasks();
    }

    /**
     * Registers a task in the task registry without scheduling it.
     * @param task Task to register
     * @return The registered task
     */
    public AstraTask registerTask(AstraTask task) {
        if (task == null) return null;

        taskRegistry.put(task.getId(), task);
        return task;
    }

    /**
     * Registers and schedules a task for execution.
     * @param task Task to schedule
     * @param schedule Whether to schedule the task immediately
     * @return The registered task
     */
    public AstraTask registerTask(AstraTask task, boolean schedule) {
        if (task == null) return null;

        taskRegistry.put(task.getId(), task);
        return schedule ? scheduleTask(task) : task;
    }

    /**
     * Registers and schedules a task for execution.
     * @param task Task to schedule
     * @return The registered task
     */
    public AstraTask scheduleTask(AstraTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        taskRegistry.put(task.getId(), task);

        TaskPriority priority = task.getPriority();
        tasksByPriority.put(priority, tasksByPriority.get(priority) + 1);

        configureTaskCallbacks(task);

        if (!hasPendingDependencies(task)) {
            pendingTasks.add(task);
            logger.debug("Task " + task.getId() + " added to queue with priority " + task.getPriority());
        } else {
            logger.debug("Task " + task.getId() + " has pending dependencies, waiting...");
        }

        processNextTasks();

        return task;
    }

    /**
     * Configures task callbacks for monitoring and management.
     * @param task Task to configure
     */
    private void configureTaskCallbacks(AstraTask task) {
        Runnable originalOnComplete = getOriginalCallback(task, "onCompleteAction");
        AstraTask.ErrorHandler originalOnError = getOriginalErrorHandler(task);

        task.onComplete(() -> {
            if (originalOnComplete != null) {
                try {
                    originalOnComplete.run();
                } catch (Exception e) {
                    logger.error("Error in original task callback " + task.getId(), e);
                }
            }

            completedTasks.incrementAndGet();
            TaskPriority priority = task.getPriority();
            tasksByPriority.put(priority, Math.max(0, tasksByPriority.get(priority) - 1));

            if (task instanceof AsyncAstraTask && task.isRunning()) {
                runningAsyncTasks.decrementAndGet();
            }

            checkDependentTasks(task.getId());

            processNextTasks();

            logger.debug("Task " + task.getId() + " completed successfully");
        });

        task.onError(throwable -> {
            if (originalOnError != null) {
                try {
                    originalOnError.handle(throwable);
                } catch (Exception e) {
                    logger.error("Error in original error handler for task " + task.getId(), e);
                }
            }

            failedTasks.incrementAndGet();
            TaskPriority priority = task.getPriority();
            tasksByPriority.put(priority, Math.max(0, tasksByPriority.get(priority) - 1));

            if (task instanceof AsyncAstraTask && task.isRunning()) {
                runningAsyncTasks.decrementAndGet();
            }

            processNextTasks();

            logger.error("Task " + task.getId() + " failed with error: " + throwable.getMessage(), throwable);
        });
    }

    /**
     * Gets the original completion callback of a task.
     * @param task Task to get the callback from
     * @param fieldName Name of the field containing the callback
     * @return Original callback or null if it doesn't exist
     */
    private Runnable getOriginalCallback(AstraTask task, String fieldName) {
        try {
            Field field = AbstractAstraTask.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Runnable) field.get(task);
        } catch (Exception e) {
            logger.debug("Could not access original callback: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the original error handler of a task.
     * @param task Task to get the handler from
     * @return Original handler or null if it doesn't exist
     */
    private AstraTask.ErrorHandler getOriginalErrorHandler(AstraTask task) {
        try {
            Field field = AbstractAstraTask.class.getDeclaredField("errorHandler");
            field.setAccessible(true);
            return (AstraTask.ErrorHandler) field.get(task);
        } catch (Exception e) {
            logger.debug("Could not access original error handler: " + e.getMessage());
            return null;
        }
    }

    private boolean hasPendingDependencies(AstraTask task) {
        Set<String> dependencies = task.getDependencies();
        if (dependencies.isEmpty()) {
            return false;
        }

        Set<String> visited = new HashSet<>();
        visited.add(task.getId());

        return hasPendingDependenciesRecursive(task, visited);
    }

    /**
     * Checks if the given task has any pending dependencies recursively.
     *
     * @param task The task to check for pending dependencies.
     * @param visited A set to keep track of visited tasks to avoid circular dependencies.
     * @return true if the task or any of its dependencies have pending dependencies, false otherwise.
     */
    private boolean hasPendingDependenciesRecursive(AstraTask task, Set<String> visited) {
        for (String dependencyId : task.getDependencies()) {
            if (visited.contains(dependencyId)) {
                logger.error("Circular dependency detected involving task " + task.getId() +
                            " and " + dependencyId);
                return true;
            }

            AstraTask dependency = taskRegistry.get(dependencyId);
            if (dependency == null) {
                logger.warning("Task " + task.getId() + " depends on a non-existent task: " +
                              dependencyId + ". Marking as failed.");
                task.onError((AstraTask.ErrorHandler) new IllegalStateException("Dependency not found: " + dependencyId));
                return true;
            }

            if (!dependency.isCompleted()) {
                visited.add(dependencyId);
                if (hasPendingDependenciesRecursive(dependency, visited)) {
                    return true;
                }
                visited.remove(dependencyId);
            }
        }

        return false;
    }

    /**
     * Checks which dependent tasks can now be executed after a task has completed.
     * @param completedTaskId ID of the completed task
     */
    private void checkDependentTasks(String completedTaskId) {
        for (AstraTask task : taskRegistry.values()) {
            if (!task.isScheduled() && !task.isCompleted() && !task.isCancelled() &&
                    task.getDependencies().contains(completedTaskId)) {

                if (!hasPendingDependencies(task)) {
                    pendingTasks.add(task);
                    logger.debug("Task " + task.getId() + " ready to execute after completing dependency " + completedTaskId);
                }
            }
        }
    }

    /**
     * Processes the next pending tasks according to priority and available resources.
     */
    private void processNextTasks() {
        if (!processingLock.tryLock()) {
            return;
        }

        try {
            double currentLoad = getServerLoad();
            int availableAsyncSlots = maxConcurrentAsyncTasks - runningAsyncTasks.get();

            if (adaptiveConcurrency) {
                adjustConcurrencyLimits(currentLoad);
            }

            while (!pendingTasks.isEmpty() && (currentLoad < serverLoadThreshold)) {
                AstraTask nextTask = pendingTasks.peek();
                if (nextTask instanceof AsyncAstraTask && availableAsyncSlots <= 0) {
                    logger.debug("Async task limit reached (" + maxConcurrentAsyncTasks + "), waiting...");
                    break;
                }

                nextTask = pendingTasks.poll();
                if (nextTask == null) continue;

                if (nextTask.isScheduled() || nextTask.isCompleted() || nextTask.isCancelled()) {
                    continue;
                }

                if (hasPendingDependencies(nextTask)) {
                    logger.debug("Task " + nextTask.getId() + " has pending dependencies, returning to queue");
                    pendingTasks.add(nextTask);
                    continue;
                }

                if (nextTask instanceof AsyncAstraTask) {
                    runningAsyncTasks.incrementAndGet();
                    availableAsyncSlots--;
                    ((AsyncAstraTask) nextTask).execute();
                    logger.debug("Executing async task " + nextTask.getId() + " with priority " + nextTask.getPriority());
                } else if (nextTask instanceof SyncAstraTask) {
                    ((SyncAstraTask) nextTask).execute();
                    logger.debug("Executing sync task " + nextTask.getId() + " with priority " + nextTask.getPriority());
                }

                currentLoad = getServerLoad();
            }
        } finally {
            processingLock.unlock();
        }
    }

    /**
     * Gets the current server load.
     * @return Value between 0.0 and 1.0 representing server load
     */
    private double getServerLoad() {
        try {
            java.lang.management.OperatingSystemMXBean osBean =
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                return sunOsBean.getSystemCpuLoad();
            }
            
            double tps = Bukkit.getServer().getTPS()[0];
            return Math.max(0.0, Math.min(1.0, (20.0 - tps) / 20.0));
        } catch (Exception e) {
            logger.debug("Error getting server load: " + e.getMessage());
            return 0.5;
        }
    }

    /**
     * Adjusts the concurrency limits for asynchronous tasks based on the current server load.
     *
     * @param currentLoad The current server load, represented as a value between 0.0 and 1.0.
     *
     * The method calculates a new limit for the maximum number of concurrent asynchronous tasks
     * based on the current load and a predefined range. The new limit is then clamped to ensure it falls
     * within the specified range. If the new limit is higher than the current limit, the limit is increased
     * by 2. If the new limit is lower than the current limit, the limit is decreased by 2.
     *
     * The method logs a debug message indicating the adjusted limit and the current server load.
     */
    private void adjustConcurrencyLimits(double currentLoad) {
        int minTasks = 2;
        int maxTasks = 20;

        int newLimit = (int) Math.round(maxTasks - (currentLoad * (maxTasks - minTasks)));
        newLimit = Math.max(minTasks, Math.min(maxTasks, newLimit));

        if (newLimit > maxConcurrentAsyncTasks) {
            maxConcurrentAsyncTasks = Math.min(newLimit, maxConcurrentAsyncTasks + 2);
        } else if (newLimit < maxConcurrentAsyncTasks) {
            maxConcurrentAsyncTasks = Math.max(newLimit, maxConcurrentAsyncTasks - 2);
        }

        logger.debug("Adjusted concurrent async tasks limit to " + maxConcurrentAsyncTasks +
                    " (server load: " + String.format("%.2f", currentLoad * 100) + "%)");

    }
    
    /**
     * Starts periodic server resource monitoring.
     */
    private void startResourceMonitoring() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            double load = getServerLoad();
            if (load > serverLoadThreshold) {
                logger.debug("High server load: " + String.format("%.2f", load * 100) + "%, limiting tasks");
            }
            
            cleanupTasks();
            
        }, 20L * 60, 20L * 60);
    }
    
    /**
     * Cleans up completed or cancelled tasks from the registry.
     */
    private void cleanupTasks() {
        Set<String> taskIds = new HashSet<>(taskRegistry.keySet());
        int removed = 0;
        for (String taskId : taskIds) {
            AstraTask task = taskRegistry.get(taskId);
            if (task == null) continue;

            if (task.isCompleted() || task.isCancelled()) {
                boolean hasDependents = false;
                for (AstraTask otherTask : taskRegistry.values()) {
                    if (!otherTask.isCompleted() && !otherTask.isCancelled() &&
                        otherTask.getDependencies().contains(taskId)) {
                        hasDependents = true;
                        break;
                    }
                }

                if (!hasDependents) {
                    taskRegistry.remove(taskId);
                    removed++;
                }
            }
        }

        if (removed > 0) {
            logger.debug("Task cleanup: " + removed + " tasks removed from registry");
        }
    }
    
    /**
     * Cancels all pending and running tasks.
     */
    public void cancelAllTasks() {
        processingLock.lock();
        try {
            for (AstraTask task : pendingTasks) {
                task.cancel();
            }
            pendingTasks.clear();
            
            for (AstraTask task : taskRegistry.values()) {
                if (task.isScheduled() || task.isRunning()) {
                    task.cancel();
                }
            }
            
            logger.info("All tasks have been cancelled");
        } finally {
            processingLock.unlock();
        }
    }
    
    /**
     * Gets a task by its ID.
     * @param taskId Task ID
     * @return The task or null if it doesn't exist
     */
    public AstraTask getTask(String taskId) {
        return taskRegistry.get(taskId);
    }

    /**
     * Executes a registered task by its ID.
     * This method retrieves a task from the registry and executes it immediately,
     * regardless of its dependencies or scheduling status.
     *
     * @param taskId The unique identifier of the task to execute
     * @return The executed task if found and executed successfully, or null if the task wasn't found
     */
    public AstraTask executeTask(String taskId) {
        AstraTask task = taskRegistry.get(taskId);
        if (task != null) {
            if (task instanceof AsyncAstraTask) {
                ((AsyncAstraTask) task).execute();
                logger.debug("Executing async task " + task.getId() + " with priority " + task.getPriority());
            } else if (task instanceof SyncAstraTask) {
                ((SyncAstraTask) task).execute();
                logger.debug("Executing sync task " + task.getId() + " with priority " + task.getPriority());
            }
            configureTaskCallbacks(task);

            return task;
        }
        return null;
    }

    /**
     * Executes a task immediately, regardless of its dependencies or scheduling status.
     * This method will execute the provided task directly, incrementing the async task counter
     * if the task is asynchronous.
     *
     * @param task The task to execute. Can be either a synchronous or asynchronous task.
     * @return The executed task if it was successfully executed, or null if the task was null
     */
    public AstraTask executeTask(AstraTask task) {
        if (task != null) {
            if (task instanceof AsyncAstraTask) {
                ((AsyncAstraTask) task).execute();
                logger.debug("Executing async task " + task.getId() + " with priority " + task.getPriority());
            } else if (task instanceof SyncAstraTask) {
                ((SyncAstraTask) task).execute();
                logger.debug("Executing sync task " + task.getId() + " with priority " + task.getPriority());
            }
            configureTaskCallbacks(task);

            return task;
        }
        return null;
    }

    /**
     * Cancels a specific task.
     * @param taskId ID of the task to cancel
     * @return true if the task was cancelled, false if it didn't exist or was already cancelled
     */
    public boolean cancelTask(String taskId) {
        AstraTask task = taskRegistry.get(taskId);
        if (task != null) {
            return task.cancel();
        }
        return false;
    }
    
    /**
     * Gets task system statistics.
     * @return Map with statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("completedTasks", completedTasks.get());
        stats.put("failedTasks", failedTasks.get());
        stats.put("pendingTasks", pendingTasks.size());
        stats.put("runningAsyncTasks", runningAsyncTasks.get());
        stats.put("maxConcurrentAsyncTasks", maxConcurrentAsyncTasks);
        stats.put("tasksByPriority", new ConcurrentHashMap<>(tasksByPriority));
        stats.put("serverLoad", String.format("%.2f%%", getServerLoad() * 100));
        
        return stats;
    }
    
    /**
     * Sets the server load threshold.
     * @param threshold New threshold (0.0 - 1.0)
     */
    public void setServerLoadThreshold(double threshold) {
        this.serverLoadThreshold = Math.max(0.1, Math.min(0.9, threshold));
        logger.info("Server load threshold set to " + String.format("%.2f%%", serverLoadThreshold * 100));
    }
    
    /**
     * Sets whether to use adaptive concurrency.
     * @param adaptive true to enable, false to disable
     */
    public void setAdaptiveConcurrency(boolean adaptive) {
        this.adaptiveConcurrency = adaptive;
        logger.info("Adaptive concurrency " + (adaptive ? "enabled" : "disabled"));
    }
    
    /**
     * Sets the maximum number of concurrent asynchronous tasks.
     * @param max Maximum number of tasks
     */
    public void setMaxConcurrentAsyncTasks(int max) {
        this.maxConcurrentAsyncTasks = Math.max(1, max);
        logger.info("Concurrent asynchronous task limit set to " + maxConcurrentAsyncTasks);
    }

    /**
     * Creates a new synchronous task.
     * @param runnable Action to execute
     * @return New synchronous task
     */
    public SyncAstraTask createSyncTask(Runnable runnable) {
        SyncAstraTask task = new SyncAstraTask(plugin, runnable);
        return task;
    }
    
    /**
     * Creates a new asynchronous task.
     * @param runnable Action to execute
     * @return New asynchronous task
     */
    public AsyncAstraTask createAsyncTask(Runnable runnable) {
        AsyncAstraTask task = new AsyncAstraTask(plugin, runnable);
        return task;
    }
    
    /**
     * Creates a new synchronous task with a specific ID.
     * @param id Unique ID for the task
     * @param runnable Action to execute
     * @return New synchronous task
     */
    public SyncAstraTask createSyncTask(String id, Runnable runnable) {
        SyncAstraTask task = new SyncAstraTask(plugin, id, runnable);
        return task;
    }
    
    /**
     * Creates a new asynchronous task with a specific ID.
     * @param id Unique ID for the task
     * @param runnable Action to execute
     * @return New asynchronous task
     */
    public AsyncAstraTask createAsyncTask(String id, Runnable runnable) {
        AsyncAstraTask task = new AsyncAstraTask(plugin, id, runnable);
        return task;
    }
    
    /**
     * Executes a synchronous task immediately and registers it.
     * @param runnable Action to execute
     * @return Scheduled task
     */
    public SyncAstraTask runSync(Runnable runnable) {
        SyncAstraTask task = createSyncTask(runnable);
        scheduleTask(task);
        return task;
    }
    
    /**
     * Executes an asynchronous task immediately and registers it.
     * @param runnable Action to execute
     * @return Scheduled task
     */
    public AsyncAstraTask runAsync(Runnable runnable) {
        AsyncAstraTask task = createAsyncTask(runnable);
        scheduleTask(task);
        return task;
    }
    
    /**
     * Executes a synchronous task after a delay and registers it.
     * @param runnable Action to execute
     * @param delayTicks Delay in ticks
     * @return Scheduled task
     */
    public SyncAstraTask runSyncDelayed(Runnable runnable, long delayTicks) {
        SyncAstraTask task = createSyncTask(runnable);
        task.executeDelayed(delayTicks);
        scheduleTask(task);
        return task;
    }
    
    /**
     * Executes an asynchronous task after a delay and registers it.
     * @param runnable Action to execute
     * @param delayTicks Delay in ticks
     * @return Scheduled task
     */
    public AsyncAstraTask runAsyncDelayed(Runnable runnable, long delayTicks) {
        AsyncAstraTask task = createAsyncTask(runnable);
        task.executeDelayed(delayTicks);
        scheduleTask(task);
        return task;
    }

    /**
     * Executes a synchronous task repeatedly and registers it.
     * @param runnable Action to execute
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     * @return Scheduled task
     */
    public SyncAstraTask runSyncRepeating(Runnable runnable, long delayTicks, long periodTicks) {
        SyncAstraTask task = createSyncTask(runnable);
        task.executeRepeating(delayTicks, periodTicks);
        scheduleTask(task);
        return task;
    }
    
    /**
     * Executes an asynchronous task repeatedly and registers it.
     * @param runnable Action to execute
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     * @return Scheduled task
     */
    public AsyncAstraTask runAsyncRepeating(Runnable runnable, long delayTicks, long periodTicks) {
        AsyncAstraTask task = createAsyncTask(runnable);
        task.executeRepeating(delayTicks, periodTicks);
        scheduleTask(task);
        return task;
    }
}