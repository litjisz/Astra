package lol.jisz.astra.database.providers;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lol.jisz.astra.Astra;
import lol.jisz.astra.database.DatabaseProvider;
import lol.jisz.astra.database.DatabaseType;
import lol.jisz.astra.utils.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MongoDB implementation of the DatabaseProvider interface.
 * Provides functionality to interact with a MongoDB database.
 */
public class MongoDBProvider implements DatabaseProvider {
    private final Logger logger;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useAuth;

    private MongoClient client;
    private MongoDatabase db;

    /**
     * Constructs a new MongoDB provider with the specified connection parameters.
     *
     * @param plugin    The Astra plugin instance providing the logger
     * @param host      The hostname or IP address of the MongoDB server
     * @param port      The port number on which the MongoDB server is listening
     * @param database  The name of the database to connect to
     * @param username  The username for authentication (can be null for no authentication)
     * @param password  The password for authentication (can be null for no authentication)
     */
    public MongoDBProvider(Astra plugin, String host, int port, String database, String username, String password) {
        this.logger = plugin.logger();
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useAuth = username != null && !username.isEmpty() && password != null;
    }

    /**
     * Initializes the MongoDB connection using the provided configuration.
     * Creates a connection with or without authentication based on the provided credentials.
     */
    @Override
    public void initialize() {
        try {
            if (useAuth) {
                MongoCredential credential = MongoCredential.createCredential(
                        username, database, password.toCharArray());

                MongoClientSettings settings = MongoClientSettings.builder()
                        .credential(credential)
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress(host, port))))
                        .build();

                client = MongoClients.create(settings);
            } else {
                client = MongoClients.create("mongodb://" + host + ":" + port);
            }

            db = client.getDatabase(database);
            logger.info("Connected to MongoDB database: " + database);
        } catch (Exception e) {
            logger.error("Failed to connect to MongoDB", e);
        }
    }

    /**
     * MongoDB does not support JDBC connections.
     * This method is implemented to satisfy the DatabaseProvider interface but will throw an exception if called.
     *
     * @return Never returns a connection
     * @throws Exception Always throws UnsupportedOperationException
     */
    @Override
    public Connection getConnection() throws Exception {
        if (!isValid()) {
            throw new SQLException("No active MongoDB connection available");
        }

        throw new UnsupportedOperationException("MongoDB does not support JDBC connections. Use the MongoDB client directly.");
    }

    /**
     * Closes the MongoDB client connection if it exists.
     */
    @Override
    public void close() {
        if (client != null) {
            client.close();
            logger.info("MongoDB connection closed");
        }
    }

    /**
     * Checks if the MongoDB connection is valid by sending a ping command.
     *
     * @return true if the connection is valid, false otherwise
     */
    @Override
    public boolean isValid() {
        try {
            if (client != null && db != null) {
                db.runCommand(new Document("ping", 1));
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.debug("MongoDB connection is not valid: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the database type of this provider.
     *
     * @return The DatabaseType enum value representing MongoDB
     */
    @Override
    public DatabaseType getType() {
        return DatabaseType.MONGODB;
    }

    /**
     * Creates a new collection in the MongoDB database if it doesn't already exist.
     *
     * @param name The name of the collection to create
     * @return true if the collection was created or already exists, false if an error occurred
     */
    @Override
    public boolean createCollection(String name) {
        try {
            if (!collectionExists(name)) {
                db.createCollection(name);
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to create collection: " + name, e);
            return false;
        }
    }

    /**
     * Checks if a collection with the specified name exists in the database.
     *
     * @param name The name of the collection to check
     * @return true if the collection exists, false otherwise
     */
    private boolean collectionExists(String name) {
        for (String collectionName : db.listCollectionNames()) {
            if (collectionName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts a new document into the specified collection.
     *
     * @param collection The name of the collection to insert into
     * @param data A map containing the document data to insert
     * @return true if the insertion was successful, false otherwise
     */
    @Override
    public boolean insert(String collection, Map<String, Object> data) {
        try {
            MongoCollection<Document> coll = db.getCollection(collection);
            Document doc = new Document(data);
            coll.insertOne(doc);
            return true;
        } catch (Exception e) {
            logger.error("Failed to insert document into collection: " + collection, e);
            return false;
        }
    }

    /**
     * Asynchronously inserts a new document into the specified collection.
     *
     * @param collection The name of the collection to insert into
     * @param data A map containing the document data to insert
     * @return A CompletableFuture that will resolve to true if the insertion was successful, false otherwise
     */
    @Override
    public CompletableFuture<Boolean> insertAsync(String collection, Map<String, Object> data) {
        return CompletableFuture.supplyAsync(() -> insert(collection, data));
    }

    /**
     * Updates documents in the specified collection that match the given filter with the provided updates.
     *
     * @param collection The name of the collection to update documents in
     * @param filter A map containing the filter criteria to match documents
     * @param updates A map containing the fields to update and their new values
     * @return The number of documents that were modified
     */
    @Override
    public int update(String collection, Map<String, Object> filter, Map<String, Object> updates) {
        try {
            MongoCollection<Document> coll = db.getCollection(collection);
            Bson filterDoc = createFilter(filter);
            Bson updateDoc = createUpdate(updates);
            UpdateResult result = coll.updateMany(filterDoc, updateDoc);
            return (int) result.getModifiedCount();
        } catch (Exception e) {
            logger.error("Failed to update documents in collection: " + collection, e);
            return 0;
        }
    }

    /**
     * Asynchronously updates documents in the specified collection that match the given filter.
     *
     * @param collection The name of the collection to update documents in
     * @param filter A map containing the filter criteria to match documents
     * @param updates A map containing the fields to update and their new values
     * @return A CompletableFuture that will resolve to the number of documents that were modified
     */
    @Override
    public CompletableFuture<Integer> updateAsync(String collection, Map<String, Object> filter, Map<String, Object> updates) {
        return CompletableFuture.supplyAsync(() -> update(collection, filter, updates));
    }

    /**
     * Finds documents in the specified collection that match the given filter.
     *
     * @param collection The name of the collection to search in
     * @param filter A map containing the filter criteria to match documents
     * @return A list of maps, each representing a document that matched the filter
     */
    @Override
    public List<Map<String, Object>> find(String collection, Map<String, Object> filter) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            MongoCollection<Document> coll = db.getCollection(collection);
            Bson filterDoc = createFilter(filter);

            for (Document doc : coll.find(filterDoc)) {
                Map<String, Object> map = new HashMap<>();
                for (String key : doc.keySet()) {
                    map.put(key, doc.get(key));
                }
                results.add(map);
            }
        } catch (Exception e) {
            logger.error("Failed to find documents in collection: " + collection, e);
        }
        return results;
    }

    /**
     * Asynchronously finds documents in the specified collection that match the given filter.
     *
     * @param collection The name of the collection to search in
     * @param filter A map containing the filter criteria to match documents
     * @return A CompletableFuture that will resolve to a list of maps, each representing a document that matched the filter
     */
    @Override
    public CompletableFuture<List<Map<String, Object>>> findAsync(String collection, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> find(collection, filter));
    }

    /**
     * Deletes documents from the specified collection that match the given filter.
     *
     * @param collection The name of the collection to delete documents from
     * @param filter A map containing the filter criteria to match documents for deletion
     * @return The number of documents that were deleted
     */
    @Override
    public int delete(String collection, Map<String, Object> filter) {
        try {
            MongoCollection<Document> coll = db.getCollection(collection);
            Bson filterDoc = createFilter(filter);
            DeleteResult result = coll.deleteMany(filterDoc);
            return (int) result.getDeletedCount();
        } catch (Exception e) {
            logger.error("Failed to delete documents from collection: " + collection, e);
            return 0;
        }
    }

    /**
     * Asynchronously deletes documents from the specified collection that match the given filter.
     *
     * @param collection The name of the collection to delete documents from
     * @param filter A map containing the filter criteria to match documents for deletion
     * @return A CompletableFuture that will resolve to the number of documents that were deleted
     */
    @Override
    public CompletableFuture<Integer> deleteAsync(String collection, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> delete(collection, filter));
    }

    /**
     * Creates a MongoDB filter document from a map of filter criteria.
     *
     * @param filter A map containing the filter criteria where keys are field names and values are the values to match
     * @return A Bson object representing the filter criteria
     */
    private Bson createFilter(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return new Document();
        }

        List<Bson> conditions = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            conditions.add(Filters.eq(entry.getKey(), entry.getValue()));
        }

        return conditions.size() == 1 ? conditions.getFirst() : Filters.and(conditions);
    }

    /**
     * Creates a MongoDB update document from a map of field updates.
     *
     * @param updates A map containing the fields to update where keys are field names and values are the new values
     * @return A Bson object representing the update operations
     */
    private Bson createUpdate(Map<String, Object> updates) {
        List<Bson> updateOperations = new ArrayList<>();

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            updateOperations.add(Updates.set(entry.getKey(), entry.getValue()));
        }

        return Updates.combine(updateOperations);
    }
}