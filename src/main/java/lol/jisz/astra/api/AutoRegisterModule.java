package lol.jisz.astra.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark a class for automatic module registration.
 * Classes annotated with this will be automatically registered as modules
 * during the application's initialization process.
 * <p>
 * This annotation should be applied to classes that implement the module
 * interface or extend the module base class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegisterModule {
    /**
     * Defines the priority of the module during registration.
     * Modules with higher priority values will be registered before
     * modules with lower priority values.
     *
     * @return the priority value of the module, default is 0
     */
    int priority() default 0;
}