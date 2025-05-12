package lol.jisz.astra.task;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to automatically register tasks in the task system.
 * Classes annotated with this will be detected and registered as tasks
 * during the application startup process.
 *
 * The annotated class must implement Runnable or extend AbstractAstraTask.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegisterTask {

    /**
     * The unique ID for this task. If empty, a random UUID will be generated.
     * @return Task ID
     */
    String id() default "";

    /**
     * The priority of this task.
     * @return Task priority
     */
    TaskPriority priority() default TaskPriority.NORMAL;

    /**
     * Whether this task should run asynchronously.
     * @return true for async task, false for sync task
     */
    boolean async() default true;

    /**
     * Delay in ticks before the task is executed.
     * @return Delay in ticks
     */
    long delay() default 0;

    /**
     * Period in ticks between repeated executions.
     * If set to 0 or less, the task will run only once.
     * @return Period in ticks
     */
    long period() default 0;

    /**
     * IDs of tasks that must complete before this task can run.
     * @return Array of task IDs
     */
    String[] dependencies() default {};

    /**
     * Whether this task should be executed automatically on plugin startup.
     * @return true to execute on startup, false otherwise
     */
    boolean executeOnStartup() default false;

    /**
     * Description of what this task does.
     * @return Task description
     */
    String description() default "";
}