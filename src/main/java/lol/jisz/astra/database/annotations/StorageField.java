package lol.jisz.astra.database.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to customize how a field is stored in the database.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StorageField {
    /**
     * The name to use for this field in the database.
     * If empty, the field's actual name will be used.
     * @return Field name in database
     */
    String name() default "";
    
    /**
     * Whether this field should be indexed in the database
     * @return true if the field should be indexed
     */
    boolean indexed() default false;
    
    /**
     * Whether this field is required (not null)
     * @return true if the field is required
     */
    boolean required() default false;
    
    /**
     * Maximum length for string fields (used in SQL databases)
     * @return maximum length, or 0 for default
     */
    int maxLength() default 0;
}