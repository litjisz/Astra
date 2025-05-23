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

    @Override
    public String getType() {
        return "MongoDB";
    }

    @Override
    public <T extends StorageObject> CompletableFuture<Optional<T>> findById(Class<T> clazz, String id) {
        return CompletableFuture.supplyAsync(() -> findByIdSync(clazz, id));
    }

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

    @Override
    public <T extends StorageObject> CompletableFuture<Set<T>> findAll(Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> findAllSync(clazz));
    }

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

    @Override
    public <T extends StorageObject> CompletableFuture<Void> save(T object) {
        return CompletableFuture.runAsync(() -> saveSync(object));
    }

    @Override
    public <T extends StorageObject> void saveSync(T object) {
        ensureDatabaseConnected();

        Document document = createDocumentFromObject(object);
        String id = object.getId();
        document.put("_id", id);

        String collectionName = getCollectionName(object.getClass());
        MongoCollection<Document> collection = database.getCollection(collectionName);
        
        collection.replaceOne(eq("_id", id), document, UPSERT_OPTIONS);
    }

    @Override
    public <T extends StorageObject> CompletableFuture<Void> delete(Class<T> clazz, String id) {
        return CompletableFuture.runAsync(() -> deleteSync(clazz, id));
    }

    @Override
    public <T extends StorageObject> void deleteSync(Class<T> clazz, String id) {
        ensureDatabaseConnected();

        String collectionName = getCollectionName(clazz);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.deleteOne(eq("_id", id));
    }

    private String getCollectionName(Class<?> clazz) {
        return collectionNameCache.computeIfAbsent(clazz, c -> {
            try {
                Method method = c.getDeclaredMethod("getCollectionName");
                method.setAccessible(true);
                Object result = method.invoke(null);
                if (result instanceof String) {
                    return (String) result;
                }
            } catch (Exception ignored) {
                plugin.logger().error("Failed to get collection name for class: " + c.getName());
            }
            return c.getSimpleName().toLowerCase();
        });
    }

    private Document createDocumentFromObject(Object obj) {
        Document document = new Document();
        Class<?> objClass = obj.getClass();
        
        List<Field> fields = getClassFields(objClass);
        
        for (Field field : fields) {
            String fieldName = field.getName();
            
            try {
                Object value = field.get(obj);
                
                if (value == null) {
                    continue;
                }
                
                processFieldValue(document, fieldName, value);
            } catch (IllegalAccessException e) {
                plugin.logger().error("Failed to access field: " + fieldName, e);
            }
        }
        
        return document;
    }
    
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

    private List<Field> getClassFields(Class<?> clazz) {
        return classFieldsCache.computeIfAbsent(clazz, this::getAllFields);
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return fields;
    }

    private <T> T instantiateObject(Class<T> clazz, Document document) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();
            
            List<Field> fields = getClassFields(clazz);
            
            for (Field field : fields) {
                String fieldName = field.getName();
                
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
                    }
                    field.set(instance, value);
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