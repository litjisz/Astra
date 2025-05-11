package lol.jisz.astra.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to automatically register commands in the system.
 * Classes annotated with this will be detected and registered as commands
 * during the application startup process.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegisterCommand {

    /**
     * Defines the primary name of the command.
     * This name will be used to invoke the command.
     * 
     * @return the command name
     */
    String name();
    
    /**
     * Specifies the permission required to execute this command.
     * If empty, no specific permission is required.
     * 
     * @return the permission string
     */
    String permission() default "";
    
    /**
     * Determines if the command can only be executed by players.
     * If true, console and other non-player command senders cannot use this command.
     * 
     * @return true if the command is player-only, false otherwise
     */
    boolean playerOnly() default false;
    
    /**
     * Defines alternative names (aliases) for the command.
     * These can be used to invoke the command in addition to the primary name.
     * 
     * @return an array of command aliases
     */
    String[] aliases() default {};
}