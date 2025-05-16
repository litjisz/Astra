package lol.jisz.astra.database;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for database providers that defines common database operations.
 */
public interface DatabaseProvider {
    
    /**
     * Initializes the database connection.
     * 
     * @throws Exception if initialization fails
     */
    void initialize() throws Exception;
    
    /**
     * Gets a connection to the database.
     * 
     * @return A database connection
     * @throws Exception if connection cannot be established
     */
    Connection getConnection() throws Exception;
    
    /**
     * Closes the database connection.
     * 
     * @throws Exception if closing fails
     */
    void close() throws Exception;
    
    /**
     * Checks if the connection is valid.
     * 
     * @return True if the connection is valid, false otherwise
     */
    boolean isValid();
    
    /**
     * Gets the type of this database provider.
     * 
     * @return The database type
     */
    DatabaseType getType();
    
    /**
     * Creates a new collection/table if it doesn't exist.
     * 
     * @param name The name of the collection/table
     * @return True if created successfully or already exists, false otherwise
     */
    boolean createCollection(String name);
    
    /**
     * Inserts a document/row into a collection/table.
     * 
     * @param collection The collection/table name
     * @param data The data to insert
     * @return True if inserted successfully, false otherwise
     */
    boolean insert(String collection, Map<String, Object> data);
    
    /**
     * Asynchronously inserts a document/row into a collection/table.
     * 
     * @param collection The collection/table name
     * @param data The data to insert
     * @return A CompletableFuture that will complete with the result
     */
    CompletableFuture<Boolean> insertAsync(String collection, Map<String, Object> data);
    
    /**
     * Updates documents/rows in a collection/table that match the filter.
     * 
     * @param collection The collection/table name
     * @param filter The filter to match documents/rows
     * @param updates The updates to apply
     * @return The number of documents/rows updated
     */
    int update(String collection, Map<String, Object> filter, Map<String, Object> updates);
    
    /**
     * Asynchronously updates documents/rows in a collection/table that match the filter.
     * 
     * @param collection The collection/table name
     * @param filter The filter to match documents/rows
     * @param updates The updates to apply
     * @return A CompletableFuture that will complete with the number of documents/rows updated
     */
    CompletableFuture<Integer> updateAsync(String collection, Map<String, Object> filter, Map<String, Object> updates);
    
    /**
     * Finds documents/rows in a collection/table that match the filter.
     * 
     * @param collection The collection/table name
     * @param filter The filter to match documents/rows
     * @return A list of matching documents/rows
     */
    List<Map<String, Object>> find(String collection, Map<String, Object> filter);
    
    /**
     * Asynchronously finds documents/rows in a collection/table that match the filter.
     * 
     * @param collection The collection/table name
     * @param filter The filter to match documents/rows
     * @return A CompletableFuture that will complete with a list of matching documents/rows
     */
    CompletableFuture<List<Map<String, Object>>> findAsync(String collection, Map<String, Object> filter);
    
    /**
     * Deletes documents/rows from a collection/table that match the filter.
     * 
     * @param collection The collection/table name
     * @param filter The filter to match documents/rows
     * @return The number of documents/rows deleted
     */
    int delete(String collection, Map<String, Object> filter);
    
    /**
     * Asynchronously deletes documents/rows from a collection/table that match the filter.
     * 
     * @param collection The collection/table name
     * @param filter The filter to match documents/rows
     * @return A CompletableFuture that will complete with the number of documents/rows deleted
     */
    CompletableFuture<Integer> deleteAsync(String collection, Map<String, Object> filter);
}