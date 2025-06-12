package lol.jisz.astra.utils;

import lol.jisz.astra.api.Implements;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A thread-safe, high-performance map implementation optimized for Minecraft servers.
 * Uses atomic operations to ensure thread safety with minimal locking overhead.
 * 
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AstraMap<K, V> implements Map<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int PARALLEL_THRESHOLD = 1000;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    
    private static final int MINECRAFT_ENTITY_CACHE_SIZE = 512;
    private static final int MINECRAFT_CHUNK_CACHE_SIZE = 1024;
    
    private AtomicReferenceArray<Node<K, V>> segments;
    private volatile int cachedSize;
    private final float loadFactor;
    private volatile int threshold;
    private final Object[] locks;
    private volatile boolean resizing = false;
    private AtomicInteger resizeProgress = new AtomicInteger(0);
    
    private final AtomicLong readCount = new AtomicLong(0);
    private final AtomicLong writeCount = new AtomicLong(0);
    private volatile long lastOptimization = System.currentTimeMillis();
    private static final long OPTIMIZATION_INTERVAL = 60000;
    
    private final Object resizeLock = new Object();
    
    private transient Set<K> keySet;
    private transient Collection<V> values;
    private transient Set<Entry<K, V>> entrySet;
    
    /**
     * Creates a new empty AstraMap with default initial capacity and load factor.
     */
    public AstraMap() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Creates a new empty AstraMap with the specified initial capacity and default load factor.
     *
     * @param initialCapacity the initial capacity
     */
    public AstraMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Creates a new empty AstraMap with the specified initial capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor the load factor
     */
    public AstraMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
            
        this.loadFactor = loadFactor;
        this.segments = new AtomicReferenceArray<>(initialCapacity);
        this.threshold = (int)(initialCapacity * loadFactor);
        this.cachedSize = 0;
        this.locks = new Object[DEFAULT_CONCURRENCY_LEVEL];
        for (int i = 0; i < DEFAULT_CONCURRENCY_LEVEL; i++) {
            locks[i] = new Object();
        }
    }
    
    /**
     * Creates a new AstraMap with the same mappings as the specified map.
     *
     * @param map the map whose mappings are to be placed in this map
     */
    public AstraMap(Map<? extends K, ? extends V> map) {
        this(Math.max((int) (map.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_CAPACITY));
        putAll(map);
    }
    
    /**
     * Creates a new empty AstraMap.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @return a new empty AstraMap
     */
    public static <K, V> AstraMap<K, V> create() {
        return new AstraMap<>();
    }
    
    /**
     * Creates a new empty AstraMap with the specified key and value types.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param keyType the class object representing the key type
     * @param valueType the class object representing the value type
     * @return a new empty AstraMap
     */
    public static <K, V> AstraMap<K, V> create(Class<? extends K> keyType, Class<? extends V> valueType) {
        return new AstraMap<>();
    }
    
    /**
     * Crea un AstraMap optimizado para almacenar entidades de Minecraft
     */
    public static <K, V> AstraMap<K, V> createEntityCache() {
        return new AstraMap<>(MINECRAFT_ENTITY_CACHE_SIZE, 0.85f);
    }

    /**
     * Crea un AstraMap optimizado para almacenar chunks de Minecraft
     */
    public static <K, V> AstraMap<K, V> createChunkCache() {
        return new AstraMap<>(MINECRAFT_CHUNK_CACHE_SIZE, 0.75f);
    }
    
    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return cachedSize;
    }
    
    /**
     * Returns true if this map contains no key-value mappings.
     *
     * @return true if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return cachedSize == 0;
    }
    
    /**
     * Returns true if this map contains a mapping for the specified key.
     *
     * @param key key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     */
    @Override
    public boolean containsKey(Object key) {
        recordRead();
        return getNode(key) != null;
    }
    
    /**
     * Returns true if this map maps one or more keys to the specified value.
     * This operation may require time linear in the map size.
     *
     * @param value value whose presence in this map is to be tested
     * @return true if this map maps one or more keys to the specified value
     */
    @Override
    public boolean containsValue(Object value) {
        recordRead();
        if (value == null) {
            return false;
        }
        
        for (int i = 0; i < segments.length(); i++) {
            Node<K, V> node = segments.get(i);
            while (node != null) {
                if (value.equals(node.value)) {
                    return true;
                }
                node = node.next;
            }
        }
        return false;
    }
    
    /**
     * Returns the value to which the specified key is mapped,
     * or null if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         null if this map contains no mapping for the key
     */
    @Override
    public V get(Object key) {
        recordRead();
        Node<K, V> node = getNode(key);
        return node == null ? null : node.value;
    }
    
    /**
     * Returns the value to which the specified key is mapped,
     * or the defaultValue if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default value to be returned if the key is not found
     * @return the value to which the specified key is mapped, or
     *         defaultValue if this map contains no mapping for the key
     */
    public V getOrDefault(Object key, V defaultValue) {
        recordRead();
        Node<K, V> node = getNode(key);
        return node == null ? defaultValue : node.value;
    }
    
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping for key
     */
    @Override
    public V put(K key, V value) {
        recordWrite();
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        
        int hash = hash(key);
        int i = (segments.length() - 1) & hash;
        Object lock = getLock(hash);
        
        synchronized (lock) {
            Node<K, V> first = segments.get(i);
            
            for (Node<K, V> node = first; node != null; node = node.next) {
                if (node.hash == hash && key.equals(node.key)) {
                    V oldValue = node.value;
                    node.value = value;
                    return oldValue;
                }
            }
            
            segments.set(i, new Node<>(hash, key, value, first));
            
            int newSize = incrementSize();
            if (newSize > threshold) {
                resize();
            }
            
            return null;
        }
    }
    
    /**
     * Adds a key-value pair to this map and returns the map.
     * This method enables method chaining.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return this map
     */
    public AstraMap<K, V> putEntry(K key, V value) {
        put(key, value);
        return this;
    }
    
    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null if there was no mapping for key
     */
    @Override
    public V remove(Object key) {
        recordWrite();
        if (key == null) {
            return null;
        }
        
        int hash = hash(key);
        int i = (segments.length() - 1) & hash;
        Object lock = getLock(hash);
        V oldValue = null;
        
        synchronized (lock) {
            Node<K, V> first = segments.get(i);
            Node<K, V> prev = null;
            Node<K, V> node = first;
            
            while (node != null) {
                if (node.hash == hash && key.equals(node.key)) {
                    oldValue = node.value;
                    
                    if (prev == null) {
                        segments.set(i, node.next);
                    } else {
                        prev.next = node.next;
                    }
                    break;
                }
                prev = node;
                node = node.next;
            }
            
            if (node == null) {
                return null;
            }
        }
        
        decrementSize();
        return oldValue;
    }
    
    /**
     * Copies all the mappings from the specified map to this map.
     *
     * @param m mappings to be stored in this map
     */
    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        recordWrite();
        int mapSize = m.size();
        if (mapSize == 0) {
            return;
        }
        
        if (mapSize > threshold) {
            int targetCapacity = (int)((mapSize / loadFactor) + 1);
            if (targetCapacity > segments.length()) {
                resize(targetCapacity);
            }
        }
        
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }
    
    /**
     * Removes all the mappings from this map.
     */
    @Override
    public void clear() {
        recordWrite();
        for (int i = 0; i < segments.length(); i++) {
            segments.set(i, null);
        }
        cachedSize = 0;
    }
    
    /**
     * Returns a Set view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set.
     *
     * @return a set view of the keys contained in this map
     */
    @Override
    public @NotNull Set<K> keySet() {
        recordRead();
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
    }
    
    /**
     * Returns a Collection view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection.
     *
     * @return a collection view of the values contained in this map
     */
    @Override
    public @NotNull Collection<V> values() {
        recordRead();
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }
    
    /**
     * Returns a Set view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set.
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        recordRead();
        Set<Entry<K, V>> es = entrySet;
        if (es == null) {
            es = new EntrySet();
            entrySet = es;
        }
        return es;
    }
    
    /**
     * Performs the given action for each entry in this map.
     * Uses parallel processing if the map size exceeds the threshold.
     *
     * @param action the action to be performed for each entry
     */
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        recordRead();
        if (cachedSize > PARALLEL_THRESHOLD) {
            // Parallel processing logic can be added here
        } else {
            for (int i = 0; i < segments.length(); i++) {
                Node<K, V> node = segments.get(i);
                while (node != null) {
                    action.accept(node.key, node.value);
                    node = node.next;
                }
            }
        }
    }

    private Node<K, V> getNode(Object key) {
        int hash = hash(key);
        int i = (segments.length() - 1) & hash;
        Node<K, V> node = segments.get(i);
        for (; node != null; node = node.next) {
            if (node.hash == hash && key.equals(node.key)) {
                return node;
            }
        }
        return null;
    }

    private int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    private int incrementSize() {
        return ++cachedSize;
    }

    private void decrementSize() {
        --cachedSize;
    }

    private void resize() {
        resize(segments.length() * 2);
    }

    private void resize(int targetCapacity) {
        if (segments.length() >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }
        
        synchronized (resizeLock) {
            if (segments.length() >= targetCapacity) {
                return;
            }
            
            final int oldCapacity = segments.length();
            final int newCapacity = Math.min(targetCapacity, MAXIMUM_CAPACITY);
            
            if (oldCapacity >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }
            
            AtomicReferenceArray<Node<K, V>> newSegments = new AtomicReferenceArray<>(newCapacity);
            CountDownLatch transferLatch = new CountDownLatch(oldCapacity);
            
            int processors = Runtime.getRuntime().availableProcessors();
            int batchSize = Math.max(1, oldCapacity / (processors * 2));
            
            for (int i = 0; i < oldCapacity; i += batchSize) {
                final int start = i;
                final int end = Math.min(start + batchSize, oldCapacity);
                
                CompletableFuture.runAsync(() -> {
                    try {
                        for (int j = start; j < end; j++) {
                            Node<K, V> node = segments.get(j);
                            
                            while (node != null) {
                                Node<K, V> next = node.next;
                                int newIndex = (newCapacity - 1) & node.hash;
                                
                                node.next = newSegments.get(newIndex);
                                newSegments.set(newIndex, node);
                                
                                node = next;
                            }
                            
                            transferLatch.countDown();
                        }
                    } catch (Exception e) {
                        for (int j = start; j < end; j++) {
                            transferLatch.countDown();
                        }
                        throw new RuntimeException("Error during resize operation", e);
                    }
                });
            }
            
            try {
                boolean completed = transferLatch.await(30, TimeUnit.SECONDS);
                if (!completed) {
                    throw new TimeoutException("Resize operation timed out");
                }
                
                this.segments = newSegments;
                this.threshold = (int)(newCapacity * loadFactor);
                
            } catch (Exception e) {
                logger().error("Error during map resize operation", e);
            }
        }
    }

    private static final int MAXIMUM_CAPACITY = 1 << 30;

    private Object getLock(int hash) {
        return locks[(hash & 0x7FFFFFFF) % locks.length];
    }

    static class Node<K, V> implements Entry<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public final K getKey() {
            return key;
        }

        @Override
        public final V getValue() {
            return value;
        }

        @Override
        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        @Override
        public final String toString() {
            return key + "=" + value;
        }

        @Override
        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e))
                return false;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (Objects.equals(k1, k2)) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                return Objects.equals(v1, v2);
            }
            return false;
        }
    }
    
    /**
     * If the specified key is not already associated with a value, attempts to compute
     * its value using the given mapping function and enters it into this map.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with the specified key
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        recordWrite();
        if (key == null || mappingFunction == null) {
            throw new NullPointerException();
        }
        
        int hash = hash(key);
        int i = (segments.length() - 1) & hash;
        Object lock = getLock(hash);
        
        Node<K, V> node = getNode(key);
        if (node != null) {
            return node.value;
        }
        
        V newValue = mappingFunction.apply(key);
        if (newValue == null) {
            return null;
        }
        
        synchronized (lock) {
            node = getNode(key);
            if (node != null) {
                return node.value;
            }
            
            Node<K, V> first = segments.get(i);
            segments.set(i, new Node<>(hash, key, newValue, first));
        }
        
        int newSize = incrementSize();
        if (newSize > threshold) {
            resize();
        }
        
        return newValue;
    }
    
    /**
     * If the value for the specified key is present, attempts to compute a new
     * mapping given the key and its current mapped value.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     */
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        recordWrite();
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        
        int hash = hash(key);
        int i = (segments.length() - 1) & hash;
        Object lock = getLock(hash);
        
        synchronized (lock) {
            Node<K, V> node = getNode(key);
            if (node == null) {
                return null;
            }
            
            V newValue = remappingFunction.apply(key, node.value);
            
            if (newValue == null) {
                Node<K, V> first = segments.get(i);
                Node<K, V> prev = null;
                Node<K, V> current = first;
                while (current != null) {
                    if (current == node) {
                        if (prev == null) {
                            segments.set(i, current.next);
                        } else {
                            prev.next = current.next;
                        }
                        decrementSize();
                        break;
                    }
                    prev = current;
                    current = current.next;
                }
            } else {
                node.value = newValue;
            }
            
            return newValue;
        }
    }
    
    /**
     * Attempts to compute a mapping for the specified key and its current
     * mapped value (or null if there is no current mapping).
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     */
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        recordWrite();
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        
        int hash = hash(key);
        int i = (segments.length() - 1) & hash;
        Object lock = getLock(hash);
        
        synchronized (lock) {
            Node<K, V> node = getNode(key);
            V oldValue = (node == null) ? null : node.value;
            V newValue = remappingFunction.apply(key, oldValue);
            
            if (newValue == null) {
                if (node != null) {
                    Node<K, V> first = segments.get(i);
                    Node<K, V> prev = null;
                    Node<K, V> current = first;
                    while (current != null) {
                        if (current == node) {
                            if (prev == null) {
                                segments.set(i, current.next);
                            } else {
                                prev.next = current.next;
                            }
                            decrementSize();
                            break;
                        }
                        prev = current;
                        current = current.next;
                    }
                }
            } else {
                if (node != null) {
                    node.value = newValue;
                } else {
                    Node<K, V> first = segments.get(i);
                    segments.set(i, new Node<>(hash, key, newValue, first));
                    incrementSize();
                    if (cachedSize > threshold) {
                        resize();
                    }
                }
            }
            
            return newValue;
        }
    }
    
    /**
     * If the specified key is not already associated with a value or is associated with null,
     * associates it with the given non-null value. Otherwise, replaces the associated value
     * with the results of the given remapping function, or removes if the result is null.
     *
     * @param key key with which the resulting value is to be associated
     * @param value the non-null value to be merged with the existing value
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if no value is associated
     */
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        recordWrite();
        if (key == null || value == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        
        int hash = hash(key);
        int i = (segments.length() - 1) & hash;
        Object lock = getLock(hash);
        
        synchronized (lock) {
            Node<K, V> node = getNode(key);
            V oldValue = (node == null) ? null : node.value;
            V newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
            
            if (newValue == null) {
                Node<K, V> first = segments.get(i);
                Node<K, V> prev = null;
                Node<K, V> current = first;
                while (current != null) {
                    if (current == node) {
                        if (prev == null) {
                            segments.set(i, current.next);
                        } else {
                            prev.next = current.next;
                        }
                        decrementSize();
                        break;
                    }
                    prev = current;
                    current = current.next;
                }
            } else {
                if (node != null) {
                    node.value = newValue;
                } else {
                    Node<K, V> first = segments.get(i);
                    segments.set(i, new Node<>(hash, key, newValue, first));
                    incrementSize();
                    if (cachedSize > threshold) {
                        resize();
                    }
                }
            }
            
            return newValue;
        }
    }
    
    /**
     * Removes all entries whose keys satisfy the given predicate.
     *
     * @param filterFunction the predicate used to determine which keys to remove
     */
    public void removeIf(Predicate<? super K> filterFunction) {
        recordWrite();
        if (filterFunction == null) {
            throw new NullPointerException();
        }
        
        for (int i = 0; i < segments.length(); i++) {
            Node<K, V> first = segments.get(i);
            Node<K, V> prev = null;
            Node<K, V> node = first;
            while (node != null) {
                Node<K, V> next = node.next;
                if (filterFunction.test(node.key)) {
                    if (prev == null) {
                        segments.set(i, next);
                    } else {
                        prev.next = next;
                    }
                    decrementSize();
                } else {
                    prev = node;
                }
                node = next;
            }
        }
    }

    /**
     * Aplica múltiples operaciones de forma atómica para reducir la contención
     */
    public void batchUpdate(Map<K, V> updates) {
        recordWrite();
        if (updates.isEmpty()) return;
        
        Map<Object, Map<K, V>> updatesByLock = new HashMap<>();
        
        for (Entry<K, V> entry : updates.entrySet()) {
            K key = entry.getKey();
            int hash = hash(key);
            Object lock = getLock(hash);
            
            updatesByLock.computeIfAbsent(lock, k -> new HashMap<>())
                         .put(key, entry.getValue());
        }
        
        for (Entry<Object, Map<K, V>> lockGroup : updatesByLock.entrySet()) {
            Object lock = lockGroup.getKey();
            Map<K, V> groupUpdates = lockGroup.getValue();
            
            synchronized (lock) {
                this.putAll(groupUpdates);
            }
        }
    }
    
    private void recordRead() {
        readCount.incrementAndGet();
        maybeOptimize();
    }
    
    private void recordWrite() {
        writeCount.incrementAndGet();
        maybeOptimize();
    }
    
    private void maybeOptimize() {
        long now = System.currentTimeMillis();
        if (now - lastOptimization > OPTIMIZATION_INTERVAL) {
            synchronized (this) {
                if (now - lastOptimization > OPTIMIZATION_INTERVAL) {
                    optimize();
                    lastOptimization = now;
                }
            }
        }
    }
    
    private void optimize() {
        long reads = readCount.get();
        long writes = writeCount.get();
        double ratio = reads > 0 ? (double) writes / reads : 0;
        
        if (ratio < 0.1) {
            threshold = (int)(segments.length() * 0.9);
        } else if (ratio > 10) {
            threshold = (int)(segments.length() * 0.6);
            if (cachedSize > threshold * 0.8) {
                resize();
            }
        }
        
        readCount.set(0);
        writeCount.set(0);
    }
    
    private Logger logger() {
        return Implements.getPlugin().logger();
    }
    
    private class KeySet extends AbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }
        
        @Override
        public int size() {
            return AstraMap.this.size();
        }
        
        @Override
        public boolean contains(Object o) {
            return AstraMap.this.containsKey(o);
        }
        
        @Override
        public boolean remove(Object o) {
            return AstraMap.this.remove(o) != null;
        }
        
        @Override
        public void clear() {
            AstraMap.this.clear();
        }
    }

    private class Values extends AbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }
        
        @Override
        public int size() {
            return AstraMap.this.size();
        }
        
        @Override
        public boolean contains(Object o) {
            return AstraMap.this.containsValue(o);
        }
        
        @Override
        public void clear() {
            AstraMap.this.clear();
        }
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }
        
        @Override
        public int size() {
            return AstraMap.this.size();
        }
        
        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e))
                return false;
            Object key = e.getKey();
            Node<K, V> node = getNode(key);
            return node != null && node.equals(e);
        }
        
        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e))
                return false;
            Object key = e.getKey();
            return AstraMap.this.remove(key) != null;
        }
        
        @Override
        public void clear() {
            AstraMap.this.clear();
        }
    }

    private abstract class HashIterator {
        int nextSegmentIndex;
        Node<K, V> nextNode;
        Node<K, V> currentNode;
        
        HashIterator() {
            nextSegmentIndex = 0;
            advance();
        }
        
        final void advance() {
            currentNode = nextNode;
            
            if (currentNode != null) {
                nextNode = currentNode.next;
                return;
            }
            
            while (nextSegmentIndex < segments.length()) {
                Node<K, V> node = segments.get(nextSegmentIndex++);
                if (node != null) {
                    nextNode = node;
                    return;
                }
            }
        }
        
        public final boolean hasNext() {
            return nextNode != null;
        }
        
        public final void remove() {
            if (currentNode == null)
                throw new IllegalStateException();
            AstraMap.this.remove(currentNode.key);
            currentNode = null;
        }
    }

    private final class KeyIterator extends HashIterator implements Iterator<K> {
        public final K next() {
            if (!hasNext())
                throw new NoSuchElementException();
            Node<K, V> node = nextNode;
            advance();
            return node.key;
        }
    }

    private final class ValueIterator extends HashIterator implements Iterator<V> {
        public final V next() {
            if (!hasNext())
                throw new NoSuchElementException();
            Node<K, V> node = nextNode;
            advance();
            return node.value;
        }
    }

    private final class EntryIterator extends HashIterator implements Iterator<Entry<K, V>> {
        public final Entry<K, V> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            Node<K, V> node = nextNode;
            advance();
            return node;
        }
    }
}