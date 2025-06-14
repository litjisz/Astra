package lol.jisz.astra.utils;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.Implements;
import lol.jisz.astra.api.annotations.AutoRegisterModule;
import lol.jisz.astra.api.module.Module;
import lol.jisz.astra.command.AutoRegisterCommand;
import lol.jisz.astra.command.CommandBase;
import lol.jisz.astra.event.AutoRegisterListener;
import lol.jisz.astra.task.AsyncAstraTask;
import lol.jisz.astra.task.AutoRegisterTask;
import lol.jisz.astra.task.SyncAstraTask;
import lol.jisz.astra.task.TaskPriority;
import org.bukkit.event.Listener;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility class for scanning and automatically registering modules, commands, and listeners.
 * This class uses the Reflections library to find classes annotated with specific
 * annotations and registers them with the plugin.
 */
public class ClassScanner {

    private final Astra plugin;

    /**
     * Constructs a new ClassScanner instance.
     *
     * @param plugin The Astra plugin instance that will be used for registration and logging
     */
    public ClassScanner(Astra plugin) {
        this.plugin = plugin;
    }

    /**
     * Scans a package to automatically register commands, modules, listeners, and tasks.
     * This method finds all classes in the specified package, then identifies
     * and registers those that are annotated with the appropriate annotations.
     *
     * @param packageName The fully qualified name of the package to scan
     */
    public void scanPackage(String packageName) {
        try {
            Reflections reflections = new Reflections(packageName, Scanners.TypesAnnotated, Scanners.SubTypes);
            registerModules(reflections);
            registerCommands(reflections);
            registerListeners(reflections);
            registerTasks(reflections);
        } catch (Exception e) {
            plugin.logger().error("Error scanning package: " + packageName, e);
        }
    }

    /**
     * Automatically registers the found modules.
     * This method filters the provided classes to find those that:
     * 1. Extend Module
     * 2. Are annotated with @AutoRegisterModule
     * 3. Are not interfaces or abstract classes
     * Then sorts them by priority and registers them with the plugin.
     *
     * @param reflections Reflections instance to search for modules
     */
    private void registerModules(Reflections reflections) {
        Set<Class<?>> moduleClasses = reflections.getTypesAnnotatedWith(AutoRegisterModule.class);
        List<Class<?>> filteredModules = new ArrayList<>();

        for (Class<?> clazz : moduleClasses) {
            if (Module.class.isAssignableFrom(clazz) &&
                    !clazz.isInterface() &&
                    !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                filteredModules.add(clazz);
            }
        }

        filteredModules.sort((c1, c2) -> {
            AutoRegisterModule a1 = c1.getAnnotation(AutoRegisterModule.class);
            AutoRegisterModule a2 = c2.getAnnotation(AutoRegisterModule.class);
            return Integer.compare(a2.priority(), a1.priority());
        });

        for (Class<?> clazz : filteredModules) {
            try {
                Constructor<?> constructor = null;

                try {
                    constructor = clazz.getConstructor(Astra.class);
                    Module module = (Module) constructor.newInstance(plugin);
                    Implements.register(module);
                    plugin.logger().info("Module automatically registered: " + clazz.getSimpleName());
                } catch (NoSuchMethodException e) {
                    constructor = clazz.getConstructor();
                    Module module = (Module) constructor.newInstance();
                    Implements.register(module);
                    plugin.logger().info("Module automatically registered: " + clazz.getSimpleName());
                }
            } catch (Exception e) {
                plugin.logger().error("Could not register the module: " + clazz.getName(), e);
            }
        }
    }

    /**
     * Automatically registers the found commands.
     * This method filters the provided classes to find those that:
     * 1. Extend CommandBase
     * 2. Are annotated with @AutoRegisterCommand
     * 3. Are not interfaces or abstract classes
     * Then instantiates and registers them with the plugin's command manager.
     *
     * @param reflections Reflections instance to search for commands
     */
    private void registerCommands(Reflections reflections) {
        Set<Class<?>> commandClasses = reflections.getTypesAnnotatedWith(AutoRegisterCommand.class);

        for (Class<?> clazz : commandClasses) {
            if (CommandBase.class.isAssignableFrom(clazz) &&
                    !clazz.isInterface() &&
                    !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {

                try {
                    AutoRegisterCommand annotation = clazz.getAnnotation(AutoRegisterCommand.class);
                    String name = annotation.name();
                    String permission = annotation.permission();
                    boolean playerOnly = annotation.playerOnly();
                    String[] aliasesArray = annotation.aliases();
                    List<String> aliases = aliasesArray.length > 0 ? Arrays.asList(aliasesArray) : new ArrayList<>();

                    Constructor<?> constructor = null;
                    CommandBase command = null;

                    try {
                        constructor = clazz.getConstructor(Astra.class);
                        command = (CommandBase) constructor.newInstance(plugin);
                    } catch (NoSuchMethodException e1) {
                        try {
                            constructor = clazz.getConstructor();
                            command = (CommandBase) constructor.newInstance();
                        } catch (NoSuchMethodException e2) {
                            constructor = clazz.getConstructor(String.class, String.class, boolean.class, List.class);
                            command = (CommandBase) constructor.newInstance(name, permission, playerOnly, aliases);
                        }
                    }

                    if (command != null) {
                        plugin.getCommandManager().registerCommand(command);
                        plugin.logger().info("Command automatically registered: " + name);
                    }
                } catch (Exception e) {
                    plugin.logger().error("Could not register the command: " + clazz.getName(), e);
                }
            }
        }
    }

    /**
     * Automatically registers the found event listeners.
     * This method filters the provided classes to find those that:
     * 1. Implement Listener
     * 2. Are annotated with @AutoRegisterListener
     * 3. Are not interfaces or abstract classes
     * Then instantiates and registers them with the plugin's event manager.
     *
     * @param reflections Reflections instance to search for listeners
     */
    private void registerListeners(Reflections reflections) {
        Set<Class<?>> listenerClasses = reflections.getTypesAnnotatedWith(AutoRegisterListener.class);
        List<Class<?>> filteredListeners = listenerClasses.stream()
                .filter(clazz -> Listener.class.isAssignableFrom(clazz) &&
                        !clazz.isInterface() &&
                        !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers()))
                .sorted(Comparator.comparing(clazz ->
                        -clazz.getAnnotation(AutoRegisterListener.class).priority()))
                .collect(Collectors.toList());

        for (Class<?> clazz : filteredListeners) {
            try {
                Constructor<?> constructor = null;
                Listener listener = null;

                try {
                    constructor = clazz.getConstructor(Astra.class);
                    listener = (Listener) constructor.newInstance(plugin);
                } catch (NoSuchMethodException e1) {
                    try {
                        constructor = clazz.getConstructor();
                        listener = (Listener) constructor.newInstance();
                    } catch (Exception e2) {
                        plugin.logger().error("Could not find a suitable constructor for listener: " + clazz.getName());
                        continue;
                    }
                }

                if (listener != null) {
                    plugin.getServer().getPluginManager().registerEvents(listener, plugin);
                    plugin.logger().info("Listener automatically registered: " + clazz.getSimpleName());
                }
            } catch (Exception e) {
                plugin.logger().error("Could not register the listener: " + clazz.getName(), e);
            }
        }
    }

    /**
     * Automatically registers the found tasks.
     * This method filters the provided classes to find those that:
     * 1. Are annotated with @AutoRegisterTask
     * 2. Are not interfaces or abstract classes
     * Then instantiates and registers them with the plugin's task system.
     *
     * @param reflections Reflections instance to search for tasks
     */
    private void registerTasks(Reflections reflections) {
        Set<Class<?>> taskClasses = reflections.getTypesAnnotatedWith(AutoRegisterTask.class);
        
        for (Class<?> clazz : taskClasses) {
            if (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                try {
                    AutoRegisterTask annotation = clazz.getAnnotation(AutoRegisterTask.class);
                    String id = annotation.id();
                    boolean async = annotation.async();
                    boolean executeOnStartup = annotation.executeOnStartup();
                    long delay = annotation.delay();
                    long period = annotation.period();
                    TaskPriority priority = annotation.priority();
                    
                    Object taskInstance = null;
                    
                    try {
                        Constructor<?> constructor = clazz.getConstructor(Astra.class);
                        taskInstance = constructor.newInstance(plugin);
                    } catch (NoSuchMethodException e) {
                        try {
                            Constructor<?> constructor = clazz.getConstructor();
                            taskInstance = constructor.newInstance();
                        } catch (NoSuchMethodException e2) {
                            plugin.logger().error("Could not find a suitable constructor for task: " + clazz.getName());
                            continue;
                        }
                    }
                    
                    if (taskInstance != null) {
                        if (executeOnStartup) {
                            if (period > 0) {
                                // Repeating task
                                if (async && taskInstance instanceof AsyncAstraTask task) {
                                    task.setPriority(priority);
                                    task.executeRepeating(delay, period);
                                    plugin.logger().info("Async repeating task automatically registered and started: " + id);
                                } else if (!async && taskInstance instanceof SyncAstraTask task) {
                                    task.setPriority(priority);
                                    task.executeRepeating(delay, period);
                                    plugin.logger().info("Sync repeating task automatically registered and started: " + id);
                                }
                            } else if (delay > 0) {
                                // Delayed task
                                if (async && taskInstance instanceof AsyncAstraTask task) {
                                    task.setPriority(priority);
                                    task.executeDelayed(delay);
                                    plugin.logger().info("Async delayed task automatically registered and scheduled: " + id);
                                } else if (!async && taskInstance instanceof SyncAstraTask task) {
                                    task.setPriority(priority);
                                    task.executeDelayed(delay);
                                    plugin.logger().info("Sync delayed task automatically registered and scheduled: " + id);
                                }
                            } else {
                                // Immediate task
                                if (async && taskInstance instanceof AsyncAstraTask task) {
                                    task.setPriority(priority);
                                    task.execute();
                                    plugin.logger().info("Async task automatically registered and executed: " + id);
                                } else if (!async && taskInstance instanceof SyncAstraTask task) {
                                    task.setPriority(priority);
                                    task.execute();
                                    plugin.logger().info("Sync task automatically registered and executed: " + id);
                                }
                            }
                        } else {
                            plugin.logger().info("Task automatically registered (not started): " + id);
                        }
                    }
                } catch (Exception e) {
                    plugin.logger().error("Could not register the task: " + clazz.getName(), e);
                }
            }
        }
    }
}