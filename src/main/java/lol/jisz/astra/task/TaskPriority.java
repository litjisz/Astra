package lol.jisz.astra.task;

/**
 * Represents the priority levels for tasks in the system.
 * Tasks can be assigned different priority levels to indicate their importance and urgency.
 * Lower numerical values represent higher priorities.
 */
public enum TaskPriority {
    /**
     * Highest priority level for urgent tasks requiring immediate attention.
     */
    CRITICAL(0),
    
    /**
     * Important tasks that should be addressed soon after critical tasks.
     */
    HIGH(1),
    
    /**
     * Standard priority level for regular tasks.
     */
    NORMAL(2),
    
    /**
     * Tasks that can be delayed if higher priority tasks exist.
     */
    LOW(3),
    
    /**
     * Lowest priority level for tasks that can be addressed last.
     */
    MINIMAL(4);
    
    private final int value;
    
    /**
     * Constructs a TaskPriority with the specified numerical value.
     *
     * @param value The numerical value representing the priority level (lower values indicate higher priority)
     */
    TaskPriority(int value) {
        this.value = value;
    }

    /**
     * Returns the numerical value of this priority level.
     *
     * @return The integer value representing this priority level (lower values indicate higher priority)
     */
    public int getValue() {
        return value;
    }
}