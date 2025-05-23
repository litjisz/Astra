package lol.jisz.astra.database.interfaces;

/**
 * Interface for objects that can be stored in a database.
 */
public interface StorageObject {
    
    /**
     * Gets the unique identifier for this object.
     * This method can be overridden by using the @Id annotation on a field.
     * 
     * @return The unique identifier
     */
    default String getId() {
        throw new UnsupportedOperationException(
            "getId() not implemented and no @Id annotation found in class: " + 
            this.getClass().getName());
    }
}