package lol.jisz.astra.api;

import lol.jisz.astra.Astra;
import lol.jisz.astra.command.CommandManager;
import lol.jisz.astra.database.registry.DatabaseRegistry;
import lol.jisz.astra.task.TaskManager;
import lol.jisz.astra.utils.ConfigManager;

import java.util.*;

public class Implements {

    private static Astra plugin;
    private static ConfigManager configManager;

    private static final Map<Class<? extends Module>, Module> modules = new HashMap<>();
    private static final List<ModuleLifecycleListener> lifecycleListeners = new ArrayList<>();


    /**
     * Initializes the module system
     * @param plugin Plugin instance
     */
    public static void init(Astra plugin) {
        Implements.plugin = plugin;
        Implements.configManager = new ConfigManager(plugin);
    }

    /**
     * Adds a listener for module lifecycle events
     * @param listener The listener to add
     */
    public static void addLifecycleListener(ModuleLifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    /**
     * Removes a listener for module lifecycle events
     * @param listener The listener to remove
     */
    public static void removeLifecycleListener(ModuleLifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }
    
    /**
     * Registers a module and notifies listeners
     * @param module Module to register
     * @param <T> Module type
     * @return Instance of the registered module
     */
    @SuppressWarnings("all")
    public static <T extends Module> T register(T module) {
        modules.put(module.getClass(), module);
        
        for (ModuleLifecycleListener listener : lifecycleListeners) {
            listener.onModuleRegistered(module);
        }
        
        module.enable();
        
        for (ModuleLifecycleListener listener : lifecycleListeners) {
            listener.onModuleEnabled(module);
        }
        
        return module;
    }

    /**
     * Registers a module with dependencies
     * @param module Module to register
     * @param dependencies Classes of modules this module depends on
     * @param <T> Module type
     * @return Instance of the registered module
     * @throws DependencyException if a dependency is not registered
     */
    @SuppressWarnings("all")
    public static <T extends Module> T registerWithDependencies(T module, Class<? extends Module>... dependencies) 
            throws DependencyException {
        for (Class<? extends Module> dependency : dependencies) {
            if (!isRegistered(dependency)) {
                throw new DependencyException("Missing dependency: " + dependency.getSimpleName() + 
                                             " for module: " + module.getClass().getSimpleName());
            }
        }
        
        modules.put(module.getClass(), module);
        module.enable();
        return module;
    }
    
    /**
     * Registers a module and checks its dependencies declared with @DependsOn
     * @param module Module to register
     * @param <T> Module type
     * @return Instance of the registered module
     * @throws DependencyException if a dependency is not registered
     */
    @SuppressWarnings("all")
    public static <T extends Module> T registerWithAnnotatedDependencies(T module) throws DependencyException {
        Class<?> moduleClass = module.getClass();
        
        if (moduleClass.isAnnotationPresent(DependsOn.class)) {
            DependsOn dependsOn = moduleClass.getAnnotation(DependsOn.class);
            Class<? extends Module>[] dependencies = dependsOn.value();
            
            for (Class<? extends Module> dependency : dependencies) {
                if (!isRegistered(dependency)) {
                    throw new DependencyException("Missing dependency: " + dependency.getSimpleName() + 
                                                 " for module: " + moduleClass.getSimpleName());
                }
            }
        }
        
        return register(module);
    }

    /**
     * Registers a module without enabling it
     * @param module Module to register
     * @param <T> Module type
     * @return Instance of the registered module
     */
    @SuppressWarnings("all")
    public static <T extends Module> T registerOnly(T module) {
        modules.put(module.getClass(), module);

        for (ModuleLifecycleListener listener : lifecycleListeners) {
            listener.onModuleRegistered(module);
        }

        return module;
    }

    /**
     * Registers a list of modules and enables them in order of dependencies
     * @param modulesToRegister List of modules to register
     */
    public static void registerAndEnableInOrder(List<Module> modulesToRegister) {
        for (Module module : modulesToRegister) {
            registerOnly(module);
        }

        enableAllWithDependencies();
    }
    
    /**
     * Retrieves a registered module
     * @param clazz Module class
     * @param <T> Module type
     * @return Instance of the module
     */
    @SuppressWarnings("unchecked")
    public static <T> T fetch(Class<T> clazz) {
        return (T) modules.get(clazz);
    }

    /**
     * Retrieves a registered module by name
     * @param name Name of the module
     * @return Instance of the module or null if not found
     */
    public static Module fetchByName(String name) {
        for (Module module : modules.values()) {
            if (module.getClass().getSimpleName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Retrieves all registered modules that implement a specific interface
     * @param interfaceClass The interface class
     * @param <T> Interface type
     * @return List of modules implementing the specified interface
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> fetchAllImplementing(Class<T> interfaceClass) {
        List<T> result = new ArrayList<>();
        for (Module module : modules.values()) {
            if (interfaceClass.isAssignableFrom(module.getClass())) {
                result.add((T) module);
            }
        }

        if (result.isEmpty()) {
            return null;
        }

        return result;
    }

    /**
     * Retrieves a module that implements a specific interface
     * @param interfaceClass The interface class
     * @param <T> Interface type
     * @return First module found implementing the interface, or null if none found
     */
    @SuppressWarnings("unchecked")
    public static <T> T fetchImplementing(Class<T> interfaceClass) {
        for (Module module : modules.values()) {
            if (interfaceClass.isAssignableFrom(module.getClass())) {
                return (T) module;
            }
        }

        return null;
    }

    /**
     * Retrieves a specific component or resource from a registered module
     * @param clazz Module class
     * @param identifier Identifier for the specific component or resource
     * @param <T> Module type
     * @param <R> Return type
     * @return The requested component or resource, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T, R> R fetch(Class<T> clazz, String identifier) {
        T module = fetch(clazz);
        return switch (module) {
            case null -> null;
            case ConfigManager manager -> (R) configManager.getConfig(identifier);
            case TaskManager taskManager -> (R) taskManager.getTask(identifier);
            case CommandManager commandManager -> (R) commandManager.getCommand(identifier);
            case DatabaseRegistry databaseRegistry -> (R) databaseRegistry.getDatabase(identifier);

            case ResourceProvider resourceProvider -> (R) resourceProvider.getResource(identifier);
            default -> null;
        };
    }

    /**
     * Enables all registered modules.
     * This method iterates through all registered modules and calls their enable() method.
     * If a module fails to enable, an error is logged but the process continues for other modules.
     */
    public static void enableAll() {
        for (Module module : modules.values()) {
            try {
                module.enable();
                plugin.logger().info("Module enabled: " + module.getClass().getSimpleName());
            } catch (Exception e) {
                plugin.logger().error("Error enabling module: " + module.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Enables all registered modules in order of priority.
     * Modules with higher priority values are enabled first.
     */
    public static void enableAllWithPriority() {
        List<Module> sortedModules = new ArrayList<>(modules.values());
        
        sortedModules.sort((m1, m2) -> {
            int p1 = (m1 instanceof PrioritizedModule) ? ((PrioritizedModule) m1).getPriority() : 0;
            int p2 = (m2 instanceof PrioritizedModule) ? ((PrioritizedModule) m2).getPriority() : 0;
            return Integer.compare(p2, p1);
        });
        
        for (Module module : sortedModules) {
            try {
                module.enable();
                plugin.logger().info("Module enabled: " + module.getClass().getSimpleName() + 
                                    (module instanceof PrioritizedModule ? 
                                     " (priority: " + ((PrioritizedModule) module).getPriority() + ")" : ""));
            } catch (Exception e) {
                plugin.logger().error("Error enabling module: " + module.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Enables all registered modules with dependencies.
     * This method ensures that modules are enabled in the correct order based on their dependencies.
     * If a module has unsatisfied dependencies, it will not be enabled.
     */
    public static void enableAllWithDependencies() {
        Set<Class<? extends Module>> enabled = new HashSet<>();
        List<Module> sortedModules = new ArrayList<>(modules.values());

        sortedModules.sort((m1, m2) -> {
            int p1 = (m1 instanceof PrioritizedModule) ? ((PrioritizedModule) m1).getPriority() : 0;
            int p2 = (m2 instanceof PrioritizedModule) ? ((PrioritizedModule) m2).getPriority() : 0;
            return Integer.compare(p2, p1);
        });

        boolean progress;
        do {
            progress = false;
            for (Module module : sortedModules) {
                if (enabled.contains(module.getClass())) {
                    continue;
                }

                boolean dependenciesMet = true;

                if (module.getClass().isAnnotationPresent(DependsOn.class)) {
                    DependsOn dependsOn = module.getClass().getAnnotation(DependsOn.class);
                    for (Class<? extends Module> dependency : dependsOn.value()) {
                        if (!enabled.contains(dependency)) {
                            dependenciesMet = false;
                            break;
                        }
                    }
                }

                if (dependenciesMet) {
                    try {
                        module.enable();

                        for (ModuleLifecycleListener listener : lifecycleListeners) {
                            listener.onModuleEnabled(module);
                        }

                        enabled.add(module.getClass());
                        progress = true;
                        plugin.logger().info("Module enabled: " + module.getClass().getSimpleName());
                    } catch (Exception e) {
                        plugin.logger().error("Error enabling module: " + module.getClass().getSimpleName(), e);
                    }
                }
            }
        } while (progress && enabled.size() < modules.size());

        if (enabled.size() < modules.size()) {
            for (Module module : modules.values()) {
                if (!enabled.contains(module.getClass())) {
                    plugin.logger().error("The module could not be enabled due to unsatisfied dependencies: "
                            + module.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * Detects circular dependencies between modules.
     * This method uses a depth-first search algorithm to check for cycles in the dependency graph.
     * @param moduleClass The module class to check
     * @param visited Set of visited module classes
     * @param path Current path of module classes
     * @return true if a circular dependency is detected, false otherwise
     */
    private static boolean detectCircularDependency(Class<? extends Module> moduleClass,
                                                    Set<Class<? extends Module>> visited,
                                                    List<Class<? extends Module>> path) {
        if (path.contains(moduleClass)) {
            int startIndex = path.indexOf(moduleClass);
            List<String> cycle = new ArrayList<>();
            for (int i = startIndex; i < path.size(); i++) {
                cycle.add(path.get(i).getSimpleName());
            }
            cycle.add(moduleClass.getSimpleName());

            plugin.logger().error("Circular dependency detected: " + String.join(" -> ", cycle));
            return true;
        }

        if (visited.contains(moduleClass)) {
            return false;
        }

        visited.add(moduleClass);
        path.add(moduleClass);

        if (moduleClass.isAnnotationPresent(DependsOn.class)) {
            DependsOn dependsOn = moduleClass.getAnnotation(DependsOn.class);
            for (Class<? extends Module> dependency : dependsOn.value()) {
                if (detectCircularDependency(dependency, visited, path)) {
                    return true;
                }
            }
        }

        path.removeLast();
        return false;
    }

    /**
     * Disables all registered modules.
     * This method iterates through all registered modules and calls their disable() method.
     * If a module fails to disable, an error is logged but the process continues for other modules.
     */
    public static void disableAll() {
        for (Module module : modules.values()) {
            try {
                module.disable();
                
                for (ModuleLifecycleListener listener : lifecycleListeners) {
                    listener.onModuleDisabled(module);
                }
                
                plugin.logger().info("Module disabled: " + module.getClass().getSimpleName());
            } catch (Exception e) {
                plugin.logger().error("Error disabling module: " + module.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Reloads all registered modules.
     * This method iterates through all registered modules and calls their reload() method.
     * If a module fails to reload, an error is logged but the process continues for other modules.
     */
    public static void reloadAll() {
        for (Module module : modules.values()) {
            try {
                module.reload();
                
                for (ModuleLifecycleListener listener : lifecycleListeners) {
                    listener.onModuleReloaded(module);
                }
                
                plugin.logger().info("Module reloaded: " + module.getClass().getSimpleName());
            } catch (Exception e) {
                plugin.logger().error("Error reloading module: " + module.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Checks if a module is registered
     * @param clazz Module class
     * @return true if the module is registered, false otherwise
     */
    public static boolean isRegistered(Class<? extends Module> clazz) {
        return modules.containsKey(clazz);
    }

    /**
     * Retrieves the configuration manager
     * @return Instance of the ConfigManager
     */
    public static ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Retrieves the plugin associated with the module system
     * @return Plugin instance
     */
    public static Astra getPlugin() {
        return plugin;
    }

    /**
     * Enables a specific module
     * @param clazz Module class
     * @return true if the module was enabled, false if it wasn't registered
     */
    public static boolean enable(Class<? extends Module> clazz) {
        Module module = modules.get(clazz);
        if (module != null) {
            try {
                module.enable();
                
                for (ModuleLifecycleListener listener : lifecycleListeners) {
                    listener.onModuleEnabled(module);
                }
                
                plugin.logger().info("Module enabled: " + module.getClass().getSimpleName());
                return true;
            } catch (Exception e) {
                plugin.logger().error("Error enabling module: " + module.getClass().getSimpleName(), e);
            }
        }
        return false;
    }

    /**
     * Disables a specific module
     * @param clazz Module class
     * @return true if the module was disabled, false if it wasn't registered
     */
    public static boolean disable(Class<? extends Module> clazz) {
        Module module = modules.get(clazz);
        if (module != null) {
            try {
                module.disable();
                
                for (ModuleLifecycleListener listener : lifecycleListeners) {
                    listener.onModuleDisabled(module);
                }
                
                plugin.logger().info("Module disabled: " + module.getClass().getSimpleName());
                return true;
            } catch (Exception e) {
                plugin.logger().error("Error disabling module: " + module.getClass().getSimpleName(), e);
            }
        }
        return false;
    }

    /**
     * Reloads a specific module
     * @param clazz Module class
     * @return true if the module was reloaded, false if it wasn't registered
     */
    public static boolean reload(Class<? extends Module> clazz) {
        Module module = modules.get(clazz);
        if (module != null) {
            try {
                module.reload();
                
                for (ModuleLifecycleListener listener : lifecycleListeners) {
                    listener.onModuleReloaded(module);
                }
                
                plugin.logger().info("Module reloaded: " + module.getClass().getSimpleName());
                return true;
            } catch (Exception e) {
                plugin.logger().error("Error reloading module: " + module.getClass().getSimpleName(), e);
            }
        }
        return false;
    }

    /**
     * Exception thrown when a module dependency is not satisfied
     */
    public static class DependencyException extends RuntimeException {
        public DependencyException(String message) {
            super(message);
        }
    }
}