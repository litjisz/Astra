package lol.jisz.astra.database.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface StorageConstructor {
    /**
     * The name of the storage provider.
     * <p>
     * This is used to identify the storage provider in the database.
     *
     * @return The name of the storage provider.
     */
    String value() default "";
}