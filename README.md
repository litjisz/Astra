# 🚀 Astra Framework

## 📋 Overview
Astra is a comprehensive framework designed to streamline and optimize the Minecraft plugin development process for Paper servers. Built with modularity and efficiency in mind, Astra provides developers with powerful tools to create robust plugins while minimizing boilerplate code.

---

## ✨ Features

### 🧩 Modular Architecture
- **Module System**: Create independent, reusable modules with automatic lifecycle management
- **Auto-Registration**: Use annotations to automatically register modules and commands
- **Dependency Injection**: Access registered modules through the `Implements` system

### 🎮 Command Framework
- **Simplified Command Creation**: Extend `CommandBase` for quick command implementation
- **Permission Management**: Built-in permission handling
- **Tab Completion**: Easy-to-implement argument suggestions
- **Argument Validation**: Tools for validating and parsing command arguments

### ⚙️ Configuration Management
- **YAML Support**: Comprehensive API for working with YAML configurations
- **Default Configurations**: Automatic handling of default configuration values
- **Type-Safe Access**: Methods for retrieving specific data types from configuration

### 🛠️ Development Utilities
- **Class Scanning**: Automatic discovery of modules and commands in specified packages
- **Logging System**: Structured logging with multiple severity levels
- **Plugin Lifecycle Management**: Simplified enable, disable, and reload operations

---

## 🚦 Getting Started

### 📋 Requirements
- Java 17 or higher
- Paper server (latest version recommended)
- Maven

### 📥 Installation

Add Astra as a dependency in your `pom.xml`:

```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
```xml
	<dependency>
	    <groupId>com.github.litjisz</groupId>
	    <artifactId>Astra</artifactId>
	    <version>Tag</version>
	</dependency>
```
[![](https://jitpack.io/v/litjisz/Astra.svg)](https://jitpack.io/#litjisz/Astra)

### 🔨 Creating Your First Astra Plugin

#### 1. Create a main plugin class that extends `Astra`:

```java
package com.example.myplugin;

import lol.jisz.astra.Astra;

public class MyPlugin extends Astra {
    @Override
    protected void onInitialize() {
        // Your initialization code here
        logger().info("MyPlugin has been initialized!");
        
        // Scan for modules and commands in your package
        getClassScanner().scanPackage("com.example.myplugin");
    }
}
```

#### 2. Create a module:

```java
package com.example.myplugin.modules;

import lol.jisz.astra.api.module.AbstractModule;
import lol.jisz.astra.api.annotations.AutoRegisterModule;

@AutoRegisterModule
public class ExampleModule extends AbstractModule {
    @Override
    public void onEnable() {
        logger().info("ExampleModule enabled!");
    }

    @Override
    public void onDisable() {
        logger().info("ExampleModule disabled!");
    }

    public void doSomething() {
        // Module functionality
    }
}
```

#### 3. Create a command:

```java
package com.example.myplugin.commands;

import lol.jisz.astra.command.CommandBase;
import org.bukkit.command.CommandSender;

public class ExampleCommand extends CommandBase {
    public ExampleCommand() {
        super("example", "Example command", "/example", "myplugin.command.example");
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        sender.sendMessage("Example command executed!");
        return true;
    }
}
```

#### 4. Register your command in your main plugin class:

```java
@Override
protected void onInitialize() {
    // Previous code...
    
    // Register command manually
    getPluginHelper().getCommandManager().register(new ExampleCommand());
}
```

---

## 💡 Best Practices

- 📁 Organize your code into modules based on functionality
- 🔄 Use the `Implements` system to access modules instead of static references
- ⚡ Take advantage of automatic registration with annotations
- 📝 Utilize the configuration system for all plugin settings
- 🔄 Implement proper reload functionality in your modules

---

## 📜 License

Copyright © 2025. All rights reserved.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

<p align="center">
  <b>Built with ❤️ by the Astra Team</b><br>
  <i>Making Minecraft plugin development easier, one module at a time.</i>
</p>
