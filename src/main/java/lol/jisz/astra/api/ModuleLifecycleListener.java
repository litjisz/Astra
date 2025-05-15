package lol.jisz.astra.api;

/**
 * Interface for listening to module lifecycle events
 */
public interface ModuleLifecycleListener {
    
    /**
     * Called when a module is registered
     * @param module The registered module
     */
    void onModuleRegistered(Module module);
    
    /**
     * Called when a module is enabled
     * @param module The enabled module
     */
    void onModuleEnabled(Module module);
    
    /**
     * Called when a module is disabled
     * @param module The disabled module
     */
    void onModuleDisabled(Module module);
    
    /**
     * Called when a module is reloaded
     * @param module The reloaded module
     */
    void onModuleReloaded(Module module);
}