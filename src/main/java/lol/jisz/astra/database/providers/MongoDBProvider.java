package lol.jisz.astra.database.providers;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import lol.jisz.astra.Astra;
import lol.jisz.astra.database.AstraDatabase;
import lol.jisz.astra.database.annotations.StorageCollection;
import lol.jisz.astra.database.annotations.StorageField;
import lol.jisz.astra.database.annotations.StorageId;
import lol.jisz.astra.database.annotations.StorageIgnore;
import lol.jisz.astra.database.interfaces.StorageObject;
import lol.jisz.astra.task.AsyncAstraTask;
import org.bson.Document;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.mongodb.client.model.Filters.eq;

/**
 * MongoDB provider for Astra database
 * This class provides methods to interact with a MongoDB database.
 * It allows for saving, finding, and deleting objects in the database.
 */
public class MongoDBProvider extends AstraDatabase {

    private MongoClient mongoClient;
    private MongoDatabase database;

    private final String host;
    private final int port;
    private final String databaseName;
    private final String username;
    private final String password;
    private final Astra plugin;

    private final Map<Class<?>, String> collectionNameCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> classFieldsCache = new ConcurrentHashMap<>();
    private static final ReplaceOptions UPSERT_OPTIONS = new ReplaceOptions().upsert(true);

    /**
     * Creates a MongoDB provider
     *
     * @param plugin The Astra plugin instance
     * @param host The MongoDB host
     * @param port The MongoDB port
     * @param databaseName The database name
     * @param username The username (can be empty)
     * @param password The password (can be empty)
     */
    public MongoDBProvider(Astra plugin, String host, int port, String databaseName, String username, String password) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }

    /**
     * Creates a MongoDB provider using configuration values from a FileConfiguration.
     * This constructor extracts MongoDB connection parameters from the provided configuration
     * using predefined keys.
     *
     * @param plugin The Astra plugin instance that will be used for logging and task management
     * @param config The FileConfiguration containing MongoDB connection parameters with the following keys:
     *              - mongodb.host: The hostname or IP address of the MongoDB server
     *              - mongodb.port: The port number on which MongoDB is running
     *              - mongodb.database: The name of the database to connect to
     *              - mongodb.username: The username for authentication (can be empty)
     *              - mongodb.password: The password for authentication (can be empty)
     */
    public MongoDBProvider(Astra plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.host = config.getString("mongodb.host");
        this.port = config.getInt("mongodb.port");
        this.databaseName = config.getString("mongodb.database");
        this.username = config.getString("mongodb.username");
        this.password = config.getString("mongodb.password");
    }

    /**
     * Initializes the MongoDB connection
     *
     * @throws Exception if connection fails
     */
    public void initialize() throws Exception {
        String uri;
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            uri = String.format("mongodb://%s:%s@%s:%d/%s", username, password, host, port, databaseName);
        } else {
            uri = String.format("mongodb://%s:%d/%s", host, port, databaseName);
        }

        AsyncAstraTask connectionTask = new AsyncAstraTask(plugin, "mongodb-connection", () -> {
            try {
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(uri))
                        .applicationName(plugin.getDescription().getName())
                        .build();

                mongoClient = MongoClients.create(settings);
                database = mongoClient.getDatabase(databaseName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to connect to MongoDB: " + e.getMessage(), e);
            }
        });

        connectionTask.execute();
        plugin.logger().info("Connected to MongoDB database: " + databaseName);
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.logger().info("MongoDB connection closed");
        }
    }

    /**
     * Returns the database provider type identifier.
     * This method identifies this provider as a MongoDB implementation
     * when the database system needs to determine the storage backend type.
     *
     * @return The string "MongoDB" indicating this is a MongoDB database provider
     */
    @Override
    public String getType() {
        return "MongoDB";
    }

    /**
     * Asynchronously finds an object in the database by its ID.
     * This method performs a database lookup operation in a non-blocking manner
     * by delegating to the synchronous implementation via a CompletableFuture.
     *
     * @param <T>   the type of object to find, must implement StorageObject
     * @param clazz the class of the object to find, used to determine the collection
     * @param id    the unique identifier of the object to find
     * @return a CompletableFuture that will complete with an Optional containing the found object,
     *         or an empty Optional if no object with the given ID exists
     */
    @Override
    public <T extends StorageObject> CompletableFuture<Optional<T>> findById(Class<T> clazz, String id) {
        return CompletableFuture.supplyAsync(() -> findByIdSync(clazz, id));
    }

    /**
     * Synchronously finds an object in the database by its ID.
     * This method performs a direct database lookup operation to retrieve an object
     * with the specified ID from the appropriate collection.
     *
     * @param <T>   the type of object to find, must implement StorageObject
     * @param clazz the class of the object to find, used to determine the collection
     * @param id    the unique identifier of the object to find
     * @return an Optional containing the found object if it exists in the database,
     *         or an empty Optional if no object with the given ID exists
     */
    @Override
    public <T extends StorageObject> Optional<T> findByIdSync(Class<T> clazz, String id) {
        ensureDatabaseConnected();

        String collectionName = getCollectionName(clazz);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Document document = collection.find(eq("_id", id)).first();

        if (document == null) {
            return Optional.empty();
        }

        T object = instantiateObject(clazz, document);
        return Optional.ofNullable(object);
    }

    /**
     * Asynchronously retrieves all objects of a specified class from the database.
     * This method performs a database query operation in a non-blocking manner
     * by delegating to the synchronous implementation via a CompletableFuture.
     *
     * @param <T>   the type of objects to retrieve, must implement StorageObject
     * @param clazz the class of the objects to retrieve, used to determine the collection
     * @return a CompletableFuture that will complete with a Set containing all objects of the specified class
     *         stored in the database, or an empty Set if no objects exist
     */
    @Override
    public <T extends StorageObject> CompletableFuture<Set<T>> findAll(Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> findAllSync(clazz));
    }

    /**
     * Synchronously retrieves all objects of a specified class from the database.
     * This method performs a direct database query operation to retrieve all objects
     * of the specified class from the appropriate collection.
     *
     * @param <T>   the type of objects to retrieve, must implement StorageObject
     * @param clazz the class of the objects to retrieve, used to determine the collection
     * @return a Set containing all objects of the specified class stored in the database,
     *         or an empty Set if no objects exist
     */
    @Override
    public <T extends StorageObject> Set<T> findAllSync(Class<T> clazz) {
        ensureDatabaseConnected();

        Set<T> results = new HashSet<>();
        String collectionName = getCollectionName(clazz);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        for (Document document : collection.find()) {
            T object = instantiateObject(clazz, document);
            if (object != null) {
                results.add(object);
            }
        }

        return results;
    }

    /**
     * Asynchronously saves an object to the database.
     * This method performs a database save operation in a non-blocking manner
     * by delegating to the synchronous implementation via a CompletableFuture.
     *
     * @param <T>    the type of object to save, must implement StorageObject
     * @param object the object to save to the database
     * @return a CompletableFuture that will complete when the save operation is finished
     */
    @Override
    public <T extends StorageObject> CompletableFuture<Void> save(T object) {
        return CompletableFuture.runAsync(() -> saveSync(object));
    }

    /**
     * Synchronously saves an object to the MongoDB database.
     * This method converts the provided object to a MongoDB document and either inserts it as a new document
     * or replaces an existing document with the same ID. If the object's ID is null, it will be inserted
     * as a new document.
     *
     * @param <T>    the type of object to save, must implement StorageObject
     * @param object the object to save to the database, containing the data to be stored
     * @throws IllegalStateException if the database connection is not established
     */
    @Override
    public <T extends StorageObject> void saveSync(T object) {
        ensureDatabaseConnected();

        Document document = createDocumentFromObject(object);
        String id = object.getId();
        document.put("_id", id);

        String collectionName = getCollectionName(object.getClass());
        MongoCollection<Document> collection = database.getCollection(collectionName);
        
        if (id == null) {
            collection.insertOne(document);
            return;
        }
        
        collection.replaceOne(eq("_id", id), document, UPSERT_OPTIONS);
    }

    /**
     * Asynchronously deletes an object from the database by its ID.
     * This method performs a database deletion operation in a non-blocking manner
     * by delegating to the synchronous implementation via a CompletableFuture.
     *
     * @param <T>   the type of object to delete, must implement StorageObject
     * @param clazz the class of the object to delete, used to determine the collection
     * @param id    the unique identifier of the object to delete
     * @return a CompletableFuture that will complete when the deletion operation is finished
     */
    @Override
    public <T extends StorageObject> CompletableFuture<Void> delete(Class<T> clazz, String id) {
        return CompletableFuture.runAsync(() -> deleteSync(clazz, id));
    }

    /**
     * Synchronously deletes an object from the MongoDB database by its ID.
     * This method performs a direct database deletion operation to remove an object
     * with the specified ID from the appropriate collection.
     *
     * @param <T>   the type of object to delete, must implement StorageObject
     * @param clazz the class of the object to delete, used to determine the collection
     * @param id    the unique identifier of the object to delete
     * @throws IllegalStateException if the database connection is not established
     */
    @Override
    public <T extends StorageObject> void deleteSync(Class<T> clazz, String id) {
        ensureDatabaseConnected();

        String collectionName = getCollectionName(clazz);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.deleteOne(eq("_id", id));
    }

    /**
     * Gets the collection name for a class, checking for @Collection annotation first,
     * then static method, then defaulting to lowercase class name
     */
    public String getCollectionName(Class<?> clazz) {
        return collectionNameCache.computeIfAbsent(clazz, c -> {
            if (c.isAnnotationPresent(StorageCollection.class)) {
                String name = c.getAnnotation(StorageCollection.class).value();
                if (!name.isEmpty()) {
                    return name;
                }
            }

            try {
                Method method = c.getDeclaredMethod("getCollectionName");
                method.setAccessible(true);
                Object result = method.invoke(null);
                if (result instanceof String) {
                    return (String) result;
                }
            } catch (Exception ignored) {
                // Ignorar excepciones, continuar con el siguiente paso
            }

            return c.getSimpleName().toLowerCase();
        });
    }

    /**
     * Gets the field name for database storage, checking for @Field and @Id annotations
     */
    private String getFieldName(Field field) {
        if (field.isAnnotationPresent(StorageField.class)) {
            StorageField annotation = field.getAnnotation(StorageField.class);
            String customName = annotation.name();
            if (!customName.isEmpty()) {
                return customName;
            }
        }

        if (field.isAnnotationPresent(StorageId.class)) {
            return "_id";
        }

        return field.getName();
    }

    /**
     * Converts a Java object into a MongoDB Document.
     * This method recursively processes all fields of the given object and transforms them
     * into a format suitable for MongoDB storage. It handles nested objects, collections,
     * maps, arrays, and primitive types.
     *
     * @param obj The Java object to convert to a MongoDB Document. This can be any object
     *            whose fields should be stored in the database.
     * @return A MongoDB Document containing all the non-null field values from the object.
     *         If the object implements StorageObject, its ID will be stored as "_id" in the document.
     */
    private Document createDocumentFromObject(Object obj) {
        Document document = new Document();
        Class<?> objClass = obj.getClass();

        List<Field> fields = getClassFields(objClass);

        for (Field field : fields) {
            try {
                Object value = field.get(obj);

                if (value == null) {
                    continue;
                }

                String fieldName = getFieldName(field);

                processFieldValue(document, fieldName, value);
            } catch (IllegalAccessException e) {
                plugin.logger().error("Failed to access field: " + field.getName(), e);
            }
        }

        if (obj instanceof StorageObject storageObject) {
            String id = storageObject.getId();
            if (id != null) {
                document.put("_id", id);
            }
        }

        return document;
    }

    /**
     * Processes a field value and adds it to the document.
     * This method handles nested objects, collections, maps, arrays, and primitive types.
     *
     * @param document The MongoDB Document to which the field value will be added.
     * @param fieldName The name of the field in the document.
     * @param value The value of the field to be processed.
     */
    private void processFieldValue(Document document, String fieldName, Object value) {
        if (value instanceof StorageObject) {
            document.put(fieldName, createDocumentFromObject(value));
        } else if (value instanceof Map<?, ?> map) {
            Document mapDoc = new Document();
            map.forEach((k, v) -> {
                if (v instanceof StorageObject) {
                    mapDoc.put(k.toString(), createDocumentFromObject(v));
                } else {
                    mapDoc.put(k.toString(), v);
                }
            });
            document.put(fieldName, mapDoc);
        } else if (value instanceof Collection<?> collection) {
            List<Object> list = new ArrayList<>(collection.size());
            for (Object item : collection) {
                if (item instanceof StorageObject) {
                    list.add(createDocumentFromObject(item));
                } else {
                    list.add(item);
                }
            }
            document.put(fieldName, list);
        } else if (value.getClass().isArray()) {
            processArrayValue(document, fieldName, value);
        } else if (value instanceof Enum<?>) {
            document.put(fieldName, ((Enum<?>) value).name());
        } else {
            document.put(fieldName, value);
        }
    }

    /**
     * Processes an array value and adds it to the document.
     * This method handles arrays of objects, including nested StorageObjects.
     *
     * @param document The MongoDB Document to which the array value will be added.
     * @param fieldName The name of the field in the document.
     * @param array The array value to be processed.
     */
    private void processArrayValue(Document document, String fieldName, Object array) {
        int length = Array.getLength(array);
        List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            Object item = Array.get(array, i);
            if (item instanceof StorageObject) {
                list.add(createDocumentFromObject(item));
            } else {
                list.add(item);
            }
        }
        document.put(fieldName, list);
    }

    /**
     * Gets all fields from a class, including inherited fields
     * and respecting @Ignore annotation
     */
    public List<Field> getClassFields(Class<?> clazz) {
        return classFieldsCache.computeIfAbsent(clazz, this::getAllFields);
    }

    /**
     * Gets all fields from a class hierarchy, respecting @Ignore annotation
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) &&
                        !Modifier.isTransient(field.getModifiers()) &&
                        !field.isAnnotationPresent(StorageIgnore.class)) {

                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    /**
     * Instantiates an object of the specified class and populates its fields from a MongoDB Document.
     * This method uses reflection to create an instance of the class and set its fields based on the
     * values in the provided document. It handles nested objects, collections, maps, and arrays.
     *
     * @param <T> The type of object to instantiate
     * @param clazz The class of the object to instantiate
     * @param document The MongoDB Document containing field values
     * @return An instance of the specified class with fields populated from the document,
     *         or null if instantiation fails
     */
    private <T> T instantiateObject(Class<T> clazz, Document document) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();

            List<Field> fields = getClassFields(clazz);

            for (Field field : fields) {
                String fieldName = getFieldName(field);

                if (!document.containsKey(fieldName)) {
                    continue;
                }

                Object value = document.get(fieldName);

                if (value == null) {
                    continue;
                }

                Class<?> fieldType = field.getType();
                setFieldValue(instance, field, fieldType, value);
            }
            return instance;
        } catch (Exception e) {
            plugin.logger().error("Failed to instantiate object of class: " + clazz.getName(), e);
            return null;
        }
    }

    /**
     * Sets the value of a field in the given instance.
     * This method handles nested objects, collections, maps, and arrays.
     *
     * @param <T> The type of object to set the field value for
     * @param instance The object instance to set the field value on
     * @param field The field to set the value for
     * @param fieldType The type of the field
     * @param value The value to set in the field
     */
    private <T> void setFieldValue(T instance, Field field, Class<?> fieldType, Object value) throws IllegalAccessException {
        try {
            switch (value) {
                case Document document when StorageObject.class.isAssignableFrom(fieldType) -> {
                    Object nestedObject = instantiateObject(fieldType, document);
                    field.set(instance, nestedObject);
                }
                case List<?> list when StorageObject.class.isAssignableFrom(fieldType.getComponentType()) -> {
                    List<Object> nestedObjects = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Document doc) {
                            nestedObjects.add(instantiateObject(fieldType.getComponentType(), doc));
                        } else {
                            nestedObjects.add(item);
                        }
                    }
                    field.set(instance, nestedObjects);
                }
                case Map<?, ?> map when StorageObject.class.isAssignableFrom(fieldType) -> {
                    Map<Object, Object> nestedMap = new HashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getValue() instanceof Document doc) {
                            nestedMap.put(entry.getKey(), instantiateObject(fieldType, doc));
                        } else {
                            nestedMap.put(entry.getKey(), entry.getValue());
                        }
                    }
                    field.set(instance, nestedMap);
                }
                case null, default -> {
                    if (Collection.class.isAssignableFrom(fieldType)) {
                        Type genericType = field.getGenericType();
                        if (genericType instanceof ParameterizedType) {
                            Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                            if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?> itemType) {
                                List<Object> list = new ArrayList<>();

                                assert value != null;
                                for (Object item : (Collection<?>) value) {
                                    if (item instanceof Document doc) {
                                        list.add(instantiateObject(itemType, doc));
                                    } else {
                                        list.add(item);
                                    }
                                }

                                field.set(instance, list);
                            } else {
                                field.set(instance, value);
                            }
                        }
                    } else {
                        field.set(instance, value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            plugin.logger().error("Failed to set field value for field: " + field.getName(), e);
        }
    }

    /**
     * Ensures that the database connection is established
     * @throws IllegalStateException if the database connection is not established
     */
    private void ensureDatabaseConnected() {
        if (mongoClient == null || database == null) {
            try {
                initialize();
            } catch (Exception e) {
                throw new IllegalStateException("Database connection is not established", e);
            }
        }
    }
}