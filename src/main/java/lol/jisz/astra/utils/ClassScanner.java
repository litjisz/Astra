package lol.jisz.astra.utils;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.AutoRegisterModule;
import lol.jisz.astra.api.Implements;
import lol.jisz.astra.command.AutoRegisterCommand;
import lol.jisz.astra.command.CommandBase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A utility class for scanning and automatically registering modules and commands.
 * This class searches through packages to find classes annotated with specific
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
     * Scans a package to automatically register commands and modules.
     * This method finds all classes in the specified package, then identifies
     * and registers those that are annotated as auto-registerable modules or commands.
     * 
     * @param packageName The fully qualified name of the package to scan
     */
    public void scanPackage(String packageName) {
        try {
            List<Class<?>> classes = getClasses(packageName);
            registerModules(classes);
            registerCommands(classes);
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
     * @param classes List of classes to search for modules
     */
    private void registerModules(List<Class<?>> classes) {
        List<Class<?>> moduleClasses = new ArrayList<>();

        for (Class<?> clazz : classes) {
            if (Module.class.isAssignableFrom(clazz) &&
                    clazz.isAnnotationPresent(AutoRegisterModule.class) &&
                    !clazz.isInterface() &&
                    !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                moduleClasses.add(clazz);
            }
        }

        moduleClasses.sort((c1, c2) -> {
            AutoRegisterModule a1 = c1.getAnnotation(AutoRegisterModule.class);
            AutoRegisterModule a2 = c2.getAnnotation(AutoRegisterModule.class);
            return Integer.compare(a2.priority(), a1.priority());
        });

        for (Class<?> clazz : moduleClasses) {
            try {
                Constructor<?> constructor = null;

                try {
                    constructor = clazz.getConstructor(Astra.class);
                    lol.jisz.astra.api.Module module = (lol.jisz.astra.api.Module) constructor.newInstance(plugin);
                    Implements.register(module);
                    plugin.logger().info("Module automatically registered: " + clazz.getSimpleName());
                } catch (NoSuchMethodException e) {
                    constructor = clazz.getConstructor();
                    lol.jisz.astra.api.Module module = (lol.jisz.astra.api.Module) constructor.newInstance();
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
     * @param classes List of classes to search for commands
     */
    private void registerCommands(List<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            if (CommandBase.class.isAssignableFrom(clazz) &&
                    clazz.isAnnotationPresent(AutoRegisterCommand.class) &&
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
     * Retrieves all classes from a package.
     * This method scans the specified package and all its subpackages to find
     * all class files, then loads them into Class objects.
     * It supports both JAR files and directory-based class files.
     * 
     * @param packageName The fully qualified name of the package to scan
     * @return A list of Class objects found in the package
     * @throws ClassNotFoundException If a class cannot be loaded
     * @throws IOException If there is an error reading the package resources
     */
    private List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');

        Enumeration<URL> resources = plugin.getClass().getClassLoader().getResources(path);

        URL resource = null;
        if (resources.hasMoreElements()) {
            resource = resources.nextElement();
        }

        if (resource == null) {
            return classes;
        }

        String protocol = resource.getProtocol();

        if (protocol.equals("jar")) {
            String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
            try (JarFile jar = new JarFile(new File(new URL(jarPath).toURI()))) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(path) && name.endsWith(".class")) {
                        String className = name.substring(0, name.length() - 6).replace('/', '.');
                        classes.add(Class.forName(className));
                    }
                }
            } catch (Exception e) {
                plugin.logger().error("Error scanning JAR", e);
            }
        } else if (protocol.equals("file")) {
            File directory = new File(resource.getFile());
            if (directory.exists()) {
                String[] files = directory.list();
                if (files != null) {
                    for (String file : files) {
                        if (file.endsWith(".class")) {
                            String className = packageName + '.' + file.substring(0, file.length() - 6);
                            classes.add(Class.forName(className));
                        } else {
                            File subdir = new File(directory, file);
                            if (subdir.isDirectory()) {
                                classes.addAll(getClasses(packageName + '.' + file));
                            }
                        }
                    }
                }
            }
        }

        return classes;
    }
}