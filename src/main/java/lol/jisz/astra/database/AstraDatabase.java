package lol.jisz.astra.database;

import lol.jisz.astra.api.AbstractModule;
import lol.jisz.astra.database.interfaces.StorageConstructor;
import lol.jisz.astra.database.interfaces.StorageKey;
import lol.jisz.astra.database.interfaces.StorageObject;
import lol.jisz.astra.utils.AstraExecutor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Abstract base class for database implementations in Astra.
 * Provides common functionality for different database types.
 */
public abstract class AstraDatabase extends AbstractModule {

    private final Map<Class<?>, Function<Collection<?>, Collection<?>>> collectionConverters = new ConcurrentHashMap<>();
    private final Map<Class<?>, AstraExecutor.ThrowingCreator<Map<Object, Object>>> mapCreators = new ConcurrentHashMap<>();
    private final Map<Class<?>, Function<String, ?>> valueConverters = new ConcurrentHashMap<>();
    
    private final Map<Class<?>, Boolean> complexObjectCache = new ConcurrentHashMap<>();

    @Override
    public void enable() {
        logger().info("Initializing database converters");
        registerDefaultConverters();
        logger().info("Database system ready");
    }

    /**
     * Registers the default type converters.
     */
    private void registerDefaultConverters() {
        // String types
        valueConverters.put(String.class, s -> s);
        
        // Numeric primitive types
        valueConverters.put(Integer.class, Integer::parseInt);
        valueConverters.put(int.class, Integer::parseInt);
        valueConverters.put(Double.class, Double::parseDouble);
        valueConverters.put(double.class, Double::parseDouble);
        valueConverters.put(Float.class, Float::parseFloat);
        valueConverters.put(float.class, Float::parseFloat);
        valueConverters.put(Long.class, Long::parseLong);
        valueConverters.put(long.class, Long::parseLong);
        valueConverters.put(Byte.class, Byte::parseByte);
        valueConverters.put(byte.class, Byte::parseByte);
        valueConverters.put(Short.class, Short::parseShort);
        valueConverters.put(short.class, Short::parseShort);
        
        // Boolean types
        valueConverters.put(Boolean.class, Boolean::parseBoolean);
        valueConverters.put(boolean.class, Boolean::parseBoolean);
        
        // Character types
        valueConverters.put(Character.class, s -> s.isEmpty() ? '\0' : s.charAt(0));
        valueConverters.put(char.class, s -> s.isEmpty() ? '\0' : s.charAt(0));
        
        // Big number types
        valueConverters.put(BigInteger.class, BigInteger::new);
        valueConverters.put(BigDecimal.class, BigDecimal::new);
        
        // Other common types
        valueConverters.put(UUID.class, UUID::fromString);

        registerCollectionConverters();
        registerMapCreators();
    }
    
    /**
     * Registers the default collection converters.
     */
    private void registerCollectionConverters() {
        collectionConverters.put(List.class, ArrayList::new);
        collectionConverters.put(ArrayList.class, ArrayList::new);
        collectionConverters.put(LinkedList.class, LinkedList::new);
        collectionConverters.put(Set.class, HashSet::new);
        collectionConverters.put(HashSet.class, HashSet::new);
        collectionConverters.put(TreeSet.class, TreeSet::new);
        collectionConverters.put(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new);
    }
    
    /**
     * Registers the default map creators.
     */
    private void registerMapCreators() {
        mapCreators.put(HashMap.class, HashMap::new);
        mapCreators.put(Map.class, HashMap::new);
        mapCreators.put(ConcurrentHashMap.class, ConcurrentHashMap::new);
        mapCreators.put(TreeMap.class, TreeMap::new);
        mapCreators.put(LinkedHashMap.class, LinkedHashMap::new);
    }

    /**
     * Creates a new map instance of the specified class type.
     * Falls back to HashMap if no specific creator is registered for the given class.
     *
     * @param clazz The class type of map to create
     * @return A new map instance of the specified type
     */
    public Map<Object, Object> createMap(Class<?> clazz) {
        if (clazz == null) {
            return new HashMap<>();
        }
        
        AstraExecutor.ThrowingCreator<Map<Object, Object>> creator = mapCreators.getOrDefault(clazz, HashMap::new);
        return AstraExecutor.createUnchecked(
            creator, 
            e -> logError("Error creating map of type: " + clazz.getName(), e), 
            HashMap::new
        );
    }

    /**
     * Converts a string value to the specified class type using registered converters.
     * Handles primitive types, common Java classes, and enum values.
     *
     * @param clazz The target class type for conversion
     * @param value The string value to convert
     * @return The converted object, or the original string if no converter is found
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object convertValue(Class<?> clazz, String value) {
        if (value == null) {
            return null;
        }
        
        if (clazz == null) {
            return value;
        }
        
        Function<String, ?> converter = valueConverters.get(clazz);
        if (converter != null) {
            try {
                return converter.apply(value);
            } catch (Exception e) {
                logError("Error converting value '" + value + "' to " + clazz.getSimpleName(), e);
                return null;
            }
        }

        if (clazz.isEnum()) {
            return AstraExecutor.ofUnchecked(
                () -> Enum.valueOf((Class<Enum>) clazz, value),
                e -> logError("Can't find enum value for class: " + clazz.getSimpleName() + " value: " + value, e),
                () -> null
            );
        }
        
        return value;
    }

    /**
     * Converts a collection to the specified collection type using registered converters.
     *
     * @param clazz The target collection class type
     * @param value The source collection to convert
     * @return The converted collection, or a new ArrayList if no converter is found
     */
    @SuppressWarnings("unchecked")
    public <T extends Collection<?>> T convertCollection(Class<T> clazz, Collection<?> value) {
        if (value == null) {
            return null;
        }
        
        if (clazz == null) {
            return (T) new ArrayList<>(value);
        }
        
        Function<Collection<?>, Collection<?>> converter = collectionConverters.get(clazz);
        if (converter != null) {
            return (T) converter.apply(value);
        }

        return (T) new ArrayList<>(value);
    }

    /**
     * Registers a custom converter function for a specific class type.
     *
     * @param key The class type to register the converter for
     * @param converter The function that converts a string to the specified type
     */
    public void registerDefaultValueConverter(Class<?> key, Function<String, ?> converter) {
        if (key != null && converter != null) {
            valueConverters.put(key, converter);
        }
    }

    /**
     * Registers a custom collection converter function for a specific collection class.
     *
     * @param key The collection class type to register the converter for
     * @param converter The function that converts a collection to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T extends Collection<?>> void registerCollectionConverter(Class<T> key, Function<Collection<?>, T> converter) {
        if (key != null && converter != null) {
            collectionConverters.put(key, (Function<Collection<?>, Collection<?>>) converter);
        }
    }

    /**
     * Registers a custom map creator for a specific map class.
     * 
     * @param key The map class type to register the creator for
     * @param creator The creator function that creates a new map instance
     */
    public <T extends Map<Object, Object>> void registerMapCreator(Class<T> key, AstraExecutor.ThrowingCreator<T> creator) {
        if (key != null && creator != null) {
            mapCreators.put(key, (AstraExecutor.ThrowingCreator<Map<Object, Object>>) creator);
        }
    }

    /**
     * Unregisters a previously registered value converter for a class type.
     *
     * @param clazz The class type to unregister the converter for
     */
    public void unregisterDefaultValueConverter(Class<?> clazz) {
        if (clazz != null) {
            valueConverters.remove(clazz);
        }
    }

    /**
     * Unregisters a previously registered collection converter for a collection class.
     *
     * @param clazz The collection class type to unregister the converter for
     */
    public void unregisterCollectionConverter(Class<? extends Collection<?>> clazz) {
        if (clazz != null) {
            collectionConverters.remove(clazz);
        }
    }

    /**
     * Unregisters a previously registered map creator for a map class.
     * 
     * @param clazz The map class type to unregister the creator for
     */
    public void unregisterMapCreator(Class<? extends Map<?, ?>> clazz) {
        if (clazz != null) {
            mapCreators.remove(clazz);
        }
    }

    /**
     * Logs an error message with an associated exception.
     *
     * @param message The error message to log
     * @param e The exception that caused the error
     */
    protected void logError(String message, Exception e) {
        logger().error(message, e);
    }

    /**
     * Determines if a class represents a complex object that requires special handling.
     * A complex object is one that is not a primitive type, not in the java.lang package,
     * and has either fields annotated with @StorageKey or constructors annotated with @StorageConstructor.
     *
     * @param clazz The class to check
     * @return true if the class represents a complex object, false otherwise
     */
    protected boolean isComplexObject(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        
        return complexObjectCache.computeIfAbsent(clazz, cls -> {
            if (cls.isPrimitive() || cls.getName().startsWith("java.lang")) {
                return false;
            }
            
            boolean annotatedFields = Arrays.stream(cls.getDeclaredFields()).anyMatch(
                    field -> field.isAnnotationPresent(StorageKey.class)
            );
            boolean annotatedConstructor = Arrays.stream(cls.getDeclaredConstructors()).anyMatch(
                    constructor -> constructor.isAnnotationPresent(StorageConstructor.class)
            );
            return annotatedFields || annotatedConstructor;
        });
    }

    /**
     * Loads an object by its identifier asynchronously.
     *
     * @param clazz      The class type of the object.
     * @param identifier The unique identifier of the object.
     * @param <T>        The type of the storage object.
     * @return A CompletableFuture containing an Optional of the loaded object.
     */
    public <T extends StorageObject> CompletableFuture<Optional<T>> loadByIdAsync(Class<T> clazz, String identifier) {
        return findById(clazz, identifier);
    }

    /**
     * Loads an object by its identifier synchronously.
     *
     * @param clazz      The class type of the object.
     * @param identifier The unique identifier of the object.
     * @param <T>        The type of the storage object.
     * @return An Optional containing the loaded object.
     */
    public <T extends StorageObject> Optional<T> loadByIdSync(Class<T> clazz, String identifier) {
        return findByIdSync(clazz, identifier);
    }

    /**
     * Initializes the database connection and prepares it for use.
     *
     * @throws Exception If an error occurs during initialization.
     */
    public abstract void initialize() throws Exception;

    /**
     * Closes the database connection and releases any resources.
     */
    public abstract void close();

    /**
     * Gets the type of this database.
     *
     * @return The database type
     */
    public abstract String getType();

    /**
     * Asynchronously finds an object by its identifier.
     *
     * @param clazz      The class type of the object.
     * @param id         The unique identifier of the object.
     * @param <T>        The type of the storage object.
     * @return A CompletableFuture containing an Optional of the found object.
     */
    public abstract <T extends StorageObject> CompletableFuture<Optional<T>> findById(Class<T> clazz, String id);

    /**
     * Synchronously finds an object by its identifier.
     *
     * @param clazz      The class type of the object.
     * @param id         The unique identifier of the object.
     * @param <T>        The type of the storage object.
     * @return An Optional containing the found object.
     */
    public abstract <T extends StorageObject> Optional<T> findByIdSync(Class<T> clazz, String id);

    /**
     * Asynchronously finds all objects of a specific class type.
     *
     * @param clazz The class type of the objects to find.
     * @param <T>   The type of the storage object.
     * @return A CompletableFuture containing a Set of found objects.
     */
    public abstract <T extends StorageObject> CompletableFuture<Set<T>> findAll(Class<T> clazz);

    /**
     * Synchronously finds all objects of a specific class type.
     *
     * @param clazz The class type of the objects to find.
     * @param <T>   The type of the storage object.
     * @return A Set of found objects.
     */
    public abstract <T extends StorageObject> Set<T> findAllSync(Class<T> clazz);

    /**
     * Asynchronously saves an object to the database.
     *
     * @param object The object to save.
     * @param <T>    The type of the storage object.
     * @return A CompletableFuture indicating the completion of the save operation.
     */
    public abstract <T extends StorageObject> CompletableFuture<Void> save(T object);

    /**
     * Synchronously saves an object to the database.
     *
     * @param object The object to save.
     * @param <T>    The type of the storage object.
     */
    public abstract <T extends StorageObject> void saveSync(T object);

    /**
     * Asynchronously deletes an object from the database by its identifier.
     *
     * @param clazz The class type of the object.
     * @param id    The unique identifier of the object to delete.
     * @param <T>   The type of the storage object.
     * @return A CompletableFuture indicating the completion of the delete operation.
     */
    public abstract <T extends StorageObject> CompletableFuture<Void> delete(Class<T> clazz, String id);

    /**
     * Synchronously deletes an object from the database by its identifier.
     *
     * @param clazz The class type of the object.
     * @param id    The unique identifier of the object to delete.
     * @param <T>   The type of the storage object.
     */
    public abstract <T extends StorageObject> void deleteSync(Class<T> clazz, String id);
}