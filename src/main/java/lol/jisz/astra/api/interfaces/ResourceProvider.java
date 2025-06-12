package lol.jisz.astra.api.interfaces;

/**
 * Interface for modules that can provide specific resources by identifier
 * @param <T> Type of resource provided
 */
public interface ResourceProvider<T> {
    
    /**
     * Retrieves a specific resource by its identifier
     * @param identifier The identifier of the resource
     * @return The requested resource, or null if not found
     */
    T getResource(String identifier);
}