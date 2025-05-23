package lol.jisz.astra.database.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark fields or parameters that should be stored in or retrieved from a storage system.
 * This annotation helps in mapping Java objects to storage keys and provides options for handling missing values.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface StorageKey {

    /**
     * Specifies the key name to be used in the storage system.
     * If not provided, the field or parameter name will typically be used.
     *
     * @return The storage key name
     */
    String key() default "";

    /**
     * Indicates whether this field or parameter is optional.
     * If true, the absence of this key in storage won't cause errors.
     *
     * @return true if the key is optional, false otherwise
     */
    boolean optional() default false;

    /**
     * Specifies a default value to use when the key is not found in storage.
     * This is particularly useful for optional keys.
     *
     * @return The default value as a string
     */
    String defaultValue() default "";

}