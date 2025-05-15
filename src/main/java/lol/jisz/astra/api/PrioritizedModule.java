package lol.jisz.astra.api;

/**
 * Interface for modules that have a priority value.
 * Higher priority modules are initialized before lower priority ones.
 */
public interface PrioritizedModule extends Module {
    
    /**
     * Gets the priority of this module.
     * Higher values indicate higher priority.
     * 
     * @return The priority value
     */
    int getPriority();
}