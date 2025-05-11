package lol.jisz.astra.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark a class for automatic event listener registration.
 * Classes annotated with this will be automatically registered as event listeners
 * during the application's initialization process.
 * <p>
 * This annotation should be applied to classes that implement the Listener interface
 * and contain event handler methods annotated with @EventHandler.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegisterListener {
    /**
     * Defines the priority of the listener during registration.
     * Listeners with higher priority values will be registered before
     * listeners with lower priority values.
     *
     * @return the priority value of the listener, default is 0
     */
    int priority() default 0;
    
    /**
     * Determines whether the listener should be registered even if its
     * containing module is disabled.
     *
     * @return true if the listener should always be registered, false otherwise
     */
    boolean alwaysRegister() default false;
}