package lol.jisz.astra.database;

import lol.jisz.astra.Astra;
import lol.jisz.astra.database.annotations.StorageCollection;
import lol.jisz.astra.database.annotations.StorageField;
import lol.jisz.astra.database.annotations.StorageId;
import lol.jisz.astra.database.annotations.StorageIgnore;
import lol.jisz.astra.database.interfaces.StorageObject;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for processing database annotations.
 * This class provides methods for extracting metadata from annotated classes.
 */
public class AnnotationProcessor {

    private final Astra plugin;
    private final Map<Class<?>, String> collectionNameCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<java.lang.reflect.Field>> classFieldsCache = new ConcurrentHashMap<>();
    private final Map<java.lang.reflect.Field, String> fieldNameMappingCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, java.lang.reflect.Field> idFieldCache = new ConcurrentHashMap<>();

    /**
     * Creates a new AnnotationProcessor
     * @param plugin The Astra plugin instance
     */
    public AnnotationProcessor(Astra plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the collection name for a class
     * @param clazz The class to get the collection name for
     * @return The collection name
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
                java.lang.reflect.Method method = c.getDeclaredMethod("getCollectionName");
                if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == String.class) {
                    method.setAccessible(true);
                    Object result = method.invoke(null);
                    if (result instanceof String) {
                        return (String) result;
                    }
                }
            } catch (Exception ignored) {
                // Method not found or not accessible, continue with default naming
            }
            
            return c.getSimpleName().toLowerCase();
        });
    }

    /**
     * Gets all fields for a class, including inherited fields
     * @param clazz The class to get fields for
     * @return List of fields
     */
    public List<java.lang.reflect.Field> getClassFields(Class<?> clazz) {
        return classFieldsCache.computeIfAbsent(clazz, c -> {
            List<java.lang.reflect.Field> fields = new ArrayList<>();
            Class<?> currentClass = c;
            
            while (currentClass != null && currentClass != Object.class) {
                for (java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) ||
                        Modifier.isTransient(field.getModifiers()) ||
                        field.isAnnotationPresent(StorageIgnore.class)) {
                        continue;
                    }
                    
                    field.setAccessible(true);
                    fields.add(field);
                }
                currentClass = currentClass.getSuperclass();
            }
            
            return fields;
        });
    }

    /**
     * Gets the database field name for a Java field
     * @param field The Java field
     * @return The database field name
     */
    public String getDbFieldName(java.lang.reflect.Field field) {
        return fieldNameMappingCache.computeIfAbsent(field, f -> {
            if (f.isAnnotationPresent(StorageField.class)) {
                String name = f.getAnnotation(StorageField.class).name();
                if (!name.isEmpty()) {
                    return name;
                }
            }
            
            return f.getName();
        });
    }

    /**
     * Gets the ID field for a class
     * @param clazz The class to get the ID field for
     * @return The ID field, or null if not found
     */
    public java.lang.reflect.Field getIdField(Class<?> clazz) {
        return idFieldCache.computeIfAbsent(clazz, c -> {
            for (java.lang.reflect.Field field : getClassFields(c)) {
                if (field.isAnnotationPresent(StorageId.class)) {
                    return field;
                }
            }
            
            for (java.lang.reflect.Field field : getClassFields(c)) {
                String fieldName = field.getName().toLowerCase();
                if (fieldName.equals("id") || fieldName.equals("_id")) {
                    return field;
                }
            }
            
            return null;
        });
    }

    /**
     * Gets the ID value for an object
     * @param object The object to get the ID for
     * @return The ID value as a string, or null if not found
     */
    public String getIdValue(Object object) {
        if (object == null) {
            return null;
        }
        
        if (object instanceof StorageObject) {
            return ((StorageObject) object).getId();
        }
        
        java.lang.reflect.Field idField = getIdField(object.getClass());
        if (idField != null) {
            try {
                Object idValue = idField.get(object);
                return idValue != null ? idValue.toString() : null;
            } catch (IllegalAccessException ex) {
                plugin.logger().error("Could not access ID field in " + object.getClass().getName(), ex);
                return null;
            }
        }
        
        try {
            java.lang.reflect.Method getIdMethod = object.getClass().getMethod("getId");
            Object result = getIdMethod.invoke(object);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            plugin.logger().error("Could not determine ID for object of class " + object.getClass().getName());
            return null;
        }
    }
    
    /**
     * Checks if a field is indexed
     * @param field The field to check
     * @return true if the field should be indexed
     */
    public boolean isFieldIndexed(java.lang.reflect.Field field) {
        if (field.isAnnotationPresent(StorageField.class)) {
            return field.getAnnotation(StorageField.class).indexed();
        }
        return false;
    }
    
    /**
     * Checks if a field is required (not null)
     * @param field The field to check
     * @return true if the field is required
     */
    public boolean isFieldRequired(java.lang.reflect.Field field) {
        if (field.isAnnotationPresent(StorageField.class)) {
            return field.getAnnotation(StorageField.class).required();
        }
        return false;
    }
    
    /**
     * Gets the maximum length for a string field
     * @param field The field to check
     * @return The maximum length, or 0 if not specified
     */
    public int getFieldMaxLength(java.lang.reflect.Field field) {
        if (field.isAnnotationPresent(StorageField.class)) {
            return field.getAnnotation(StorageField.class).maxLength();
        }
        return 0;
    }
    
    /**
     * Clears all caches
     */
    public void clearCaches() {
        collectionNameCache.clear();
        classFieldsCache.clear();
        fieldNameMappingCache.clear();
        idFieldCache.clear();
    }
}