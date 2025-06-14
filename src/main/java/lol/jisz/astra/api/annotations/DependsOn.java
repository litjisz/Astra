package lol.jisz.astra.api.annotations;

import lol.jisz.astra.api.module.Module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare module dependencies
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DependsOn {
    /**
     * Classes of modules this module depends on
     */
    Class<? extends Module>[] value();
}