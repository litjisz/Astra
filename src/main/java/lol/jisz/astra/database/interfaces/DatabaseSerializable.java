package lol.jisz.astra.database.interfaces;

/**
 * Interface for objects that want to customize their database serialization/deserialization.
 * Each database provider will need to implement specific methods to handle this interface.
 */
public interface DatabaseSerializable {
    
    /**
     * Convert this object to a format suitable for the database
     * @return Object representation for database storage
     */
    Object toDbObject();
    
    /**
     * Update this object from a database object
     * @param dbObject Database object to update from
     */
    void fromDbObject(Object dbObject);
}