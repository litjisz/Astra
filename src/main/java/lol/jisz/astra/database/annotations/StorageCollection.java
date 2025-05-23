package lol.jisz.astra.database.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the collection/table name for a class.
 * If not present, the class simple name in lowercase will be used.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StorageCollection {
    /**
     * The name of the collection/table
     * @return Collection/table name
     */
    String value();
}