package lol.jisz.astra.api.interfaces;

/**
 * Represents a module within the Astra system.
 * <p>
 * A module is a component that can be enabled, disabled, and reloaded
 * during runtime, allowing for dynamic functionality management.
 */
public interface Module {
    /**
     * Enables this module.
     * <p>
     * This method is called when the module should start its functionality.
     * Any initialization or resource allocation should be performed here.
     */
    void enable();
    
    /**
     * Disables this module.
     * <p>
     * This method is called when the module should stop its functionality.
     * Any cleanup or resource deallocation should be performed here.
     */
    void disable();
    
    /**
     * Reloads this module.
     * <p>
     * This method is called when the module should refresh its state or configuration.
     * Typically involves disabling and then re-enabling with updated parameters.
     */
    void reload();
}