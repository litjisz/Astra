package lol.jisz.astra.utils;

import lol.jisz.astra.api.Implements;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A thread-safe, high-performance list implementation optimized for Minecraft servers.
 * Uses atomic operations to ensure thread safety with minimal locking overhead.
 * 
 * @param <E> the type of elements in this list
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AstraList<E> extends AbstractList<E> {
    private final AtomicReference<Node<E>[]> elementsRef;
    private volatile int cachedSize;
    private static final int PARALLEL_THRESHOLD = 1000;

    /**
     * Creates a new empty AstraList.
     */
    public AstraList() {
        this.elementsRef = new AtomicReference<>(new Node[0]);
        this.cachedSize = 0;
    }

    /**
     * Creates a new AstraList containing all elements from the specified collection.
     *
     * @param collection the collection whose elements are to be placed into this list
     */
    public AstraList(Collection<E> collection) {
        this();
        if (collection != null && !collection.isEmpty()) {
            addAllInternal(collection);
        }
    }

    /**
     * Creates a new AstraList containing the specified elements.
     *
     * @param elements the elements to be placed into this list
     */
    @SafeVarargs
    public AstraList(E... elements) {
        this();
        if (elements != null && elements.length > 0) {
            addAllInternal(Arrays.asList(elements));
        }
    }

    /**
     * Creates a new empty AstraList.
     *
     * @param <E> the type of elements in the list
     * @return a new empty AstraList
     */
    public static <E> AstraList<E> create() {
        return new AstraList<>();
    }

    /**
     * Creates a new empty AstraList with the specified element type.
     *
     * @param <E> the type of elements in the list
     * @param type the class object representing the element type
     * @return a new empty AstraList
     */
    @SuppressWarnings("unused")
    public static <E> AstraList<E> create(Class<? extends E> type) {
        return new AstraList<>();
    }

    /**
     * Adds the specified element to this list and returns the list.
     * This method enables method chaining.
     *
     * @param element the element to be added to this list
     * @return this list
     */
    public AstraList<E> addElement(final E element) {
        add(element);
        return this;
    }

    /**
     * Adds all elements from the specified list to this list and returns the list.
     * This method enables method chaining.
     *
     * @param elements the list of elements to be added to this list
     * @return this list
     */
    public AstraList<E> addElements(final List<E> elements) {
        if (elements != null && !elements.isEmpty()) {
            addAllInternal(elements);
        }
        return this;
    }

    /**
     * Internal method to add all elements from a collection to this list in a thread-safe manner.
     *
     * @param collection the collection whose elements are to be added to this list
     */
    private void addAllInternal(Collection<? extends E> collection) {
        if (collection.isEmpty()) {
            return;
        }

        Node<E>[] currentArray;
        Node<E>[] newArray;
        int collectionSize = collection.size();

        do {
            currentArray = elementsRef.get();
            int currentSize = currentArray.length;
            newArray = Arrays.copyOf(currentArray, currentSize + collectionSize);
            
            int i = currentSize;
            for (E element : collection) {
                newArray[i++] = new Node<>(element, 0);
            }
        } while (!elementsRef.compareAndSet(currentArray, newArray));
        
        cachedSize = newArray.length;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public E get(int index) {
        Node<E>[] currentArray = elementsRef.get();
        if (index >= currentArray.length || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + currentArray.length);
        }
        return currentArray[index].element();
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return cachedSize;
    }

    /**
     * Performs the given action for each element of the list.
     * Uses parallel processing if the list size exceeds the threshold.
     *
     * @param action the action to be performed for each element
     * @throws NullPointerException if the specified action is null
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        Node<E>[] currentArray = elementsRef.get();
        
        if (currentArray.length > PARALLEL_THRESHOLD) {
            Arrays.stream(currentArray).parallel().forEach(node -> action.accept(node.element()));
        } else {
            for (Node<E> node : currentArray) {
                action.accept(node.element());
            }
        }
    }

    /**
     * Returns a new AstraList containing only the elements that match the given predicate.
     *
     * @param predicate a predicate to apply to each element to determine if it should be included
     * @return a new AstraList containing only the elements that match the given predicate
     * @throws NullPointerException if the specified predicate is null
     */
    public AstraList<E> filter(Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "Predicate cannot be null");

        Node<E>[] currentArray = elementsRef.get();
        if (currentArray.length == 0) {
            return new AstraList<>();
        }

        int count = 0;
        for (Node<E> node : currentArray) {
            E element = node.element();
            if (element != null && predicate.test(element)) {
                count++;
            }
        }

        AstraList<E> filtered = new AstraList<>();
        if (count == 0) {
            return filtered;
        }

        Node<E>[] newArray = new Node[count];
        int index = 0;
        
        for (Node<E> node : currentArray) {
            E element = node.element();
            if (element != null && predicate.test(element)) {
                newArray[index++] = new Node<>(element, node.priority());
            }
        }
        
        filtered.elementsRef.set(newArray);
        filtered.cachedSize = count;
        return filtered;
    }

    /**
     * Returns an Optional containing the first element of this list,
     * or an empty Optional if this list is empty.
     *
     * @return an Optional containing the first element, or an empty Optional if this list is empty
     */
    public Optional<E> findFirst() {
        Node<E>[] currentArray = elementsRef.get();
        if (currentArray.length == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(currentArray[0].element());
    }

    /**
     * Returns an Optional containing the first element that matches the given predicate,
     * or an empty Optional if no element matches.
     *
     * @param predicate a predicate to apply to each element to determine if it should be returned
     * @return an Optional containing the first matching element, or an empty Optional if none match
     * @throws NullPointerException if the specified predicate is null
     */
    public Optional<E> find(Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "Predicate cannot be null");
        Node<E>[] elements = elementsRef.get();

        for (Node<E> node : elements) {
            E element = node.element();
            if (element != null && predicate.test(element)) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a new AstraList containing the results of applying the given function
     * to each element of this list. Null results are excluded from the new list.
     *
     * @param <R> the element type of the new list
     * @param mapper a function to apply to each element
     * @return a new AstraList containing the results of applying the function to each element
     * @throws NullPointerException if the specified function is null
     */
    public <R> AstraList<R> map(Function<E, R> mapper) {
        Objects.requireNonNull(mapper, "Function cannot be null");

        Node<E>[] elements = elementsRef.get();
        if (elements.length == 0) {
            return new AstraList<>();
        }

        AstraList<R> mapped = new AstraList<>();
        Node<R>[] newArray = new Node[elements.length];
        int validCount = 0;

        for (Node<E> eNode : elements) {
            E element = eNode.element();
            if (element != null) {
                R mappedValue = mapper.apply(element);
                if (mappedValue != null) {
                    newArray[validCount++] = new Node<>(mappedValue, eNode.priority());
                }
            }
        }
        
        if (validCount < elements.length) {
            newArray = Arrays.copyOf(newArray, validCount);
        }
        
        mapped.elementsRef.set(newArray);
        mapped.cachedSize = validCount;
        return mapped;
    }

    /**
     * Returns true if this list contains the specified element.
     *
     * @param o element whose presence in this list is to be tested
     * @return true if this list contains the specified element
     */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        
        Node<E>[] currentArray = elementsRef.get();
        for (Node<E> node : currentArray) {
            E element = node.element();
            if (element != null && element.equals(o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the specified element to the end of this list with default priority (0).
     *
     * @param element element to be appended to this list
     * @return true (as specified by Collection.add)
     */
    @Override
    public boolean add(E element) {
        add(element, 0);
        return true;
    }

    /**
     * Adds the specified element to the end of this list with the specified priority.
     * Higher priority values will be sorted first when using sortByPriority().
     *
     * @param element element to be appended to this list
     * @param priority the priority value for this element
     */
    public void add(E element, int priority) {
        if (element == null) {
            return;
        }
        
        Node<E>[] currentArray;
        Node<E>[] newArray;

        do {
            currentArray = elementsRef.get();
            int currentSize = currentArray.length;
            newArray = Arrays.copyOf(currentArray, currentSize + 1);
            newArray[currentSize] = new Node<>(element, priority);
        } while (!elementsRef.compareAndSet(currentArray, newArray));
        
        cachedSize = newArray.length;
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     *
     * @param o element to be removed from this list, if present
     * @return true if this list contained the specified element
     */
    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        
        Node<E>[] currentArray;
        Node<E>[] newArray;
        int index;

        do {
            currentArray = elementsRef.get();
            index = indexOf(currentArray, o);
            if (index == -1) {
                return false;
            }
        
            int newSize = currentArray.length - 1;
            newArray = new Node[newSize];
        
            if (index == 0) {
                System.arraycopy(currentArray, 1, newArray, 0, newSize);
            } else if (index == newSize) {
                System.arraycopy(currentArray, 0, newArray, 0, newSize);
            } else {
                System.arraycopy(currentArray, 0, newArray, 0, index);
                System.arraycopy(currentArray, index + 1, newArray, index, newSize - index);
            }
        } while (!elementsRef.compareAndSet(currentArray, newArray));
        
        cachedSize = newArray.length;
        return true;
    }

    /**
     * Randomly shuffles the elements in this list using the Fisher-Yates algorithm.
     * The operation is performed atomically.
     *
     * @return this list after shuffling
     */
    public AstraList<E> shuffle() {
        Node<E>[] currentArray = elementsRef.get();
        if (currentArray.length <= 1) {
            return this;
        }
    
        Random random = Implements.fetch(Random.class);
        Node<E>[] shuffledArray;

        do {
            shuffledArray = Arrays.copyOf(currentArray, currentArray.length);
        
            for (int i = shuffledArray.length - 1; i > 0; i--) {
                int randomIndex = random.nextInt(i + 1);
                Node<E> temp = shuffledArray[i];
                shuffledArray[i] = shuffledArray[randomIndex];
                shuffledArray[randomIndex] = temp;
            }
        } while (!elementsRef.compareAndSet(currentArray, shuffledArray));
        return this;
    }

    /**
     * Sorts the elements in this list according to the order induced by the specified comparator.
     * The operation is performed atomically.
     *
     * @param comparator the comparator to determine the order of the list
     * @return this list after sorting
     */
    public AstraList<E> sorting(Comparator<? super E> comparator) {
        Node<E>[] currentArray;
        Node<E>[] sortedArray;

        do {
            currentArray = elementsRef.get();
            sortedArray = Arrays.copyOf(currentArray, currentArray.length);
            Arrays.sort(sortedArray, (a, b) -> comparator.compare(a.element(), b.element()));
        } while (!elementsRef.compareAndSet(currentArray, sortedArray));
        return this;
    }

    /**
     * Sorts the elements in this list according to their priority values.
     * Elements with lower priority values will appear first in the sorted list.
     * The operation is performed atomically.
     */
    public void sortByPriority() {
        Node<E>[] currentArray;
        Node<E>[] sortedArray;

        do {
            currentArray = elementsRef.get();
            sortedArray = Arrays.copyOf(currentArray, currentArray.length);
            Arrays.sort(sortedArray, Comparator.comparingInt(Node::priority));
        } while (!elementsRef.compareAndSet(currentArray, sortedArray));
    }

    /**
     * Returns an array containing all of the elements in this list in proper sequence.
     *
     * @return an array containing all of the elements in this list in proper sequence
     */
    @Override
    public Object @NotNull [] toArray() {
        Node<E>[] currentArray = elementsRef.get();
        Object[] result = new Object[currentArray.length];
        for (int i = 0; i < currentArray.length; i++) {
            result[i] = currentArray[i].element();
        }
        return result;
    }

    /**
     * Returns an array containing all the elements in this list in proper sequence;
     * the runtime type of the returned array is that of the specified array.
     *
     * @param <T> the runtime type of the array to contain the collection
     * @param a the array into which the elements of this list are to be stored,
     *          if it is big enough; otherwise, a new array is allocated with the runtime
     *          type of the specified array and the size of this list.
     * @return an array containing the elements of this list
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T @NotNull [] toArray(T[] a) {
        Node<E>[] currentArray = elementsRef.get();
        if (a.length < currentArray.length) {
            return (T[]) Arrays.copyOf(toArray(), currentArray.length, a.getClass());
        }
        System.arraycopy(toArray(), 0, a, 0, currentArray.length);
        if (a.length > currentArray.length) {
            a[currentArray.length] = null;
        }
        return a;
    }

    /**
     * Finds the index of the first occurrence of the specified element in the given array.
     * 
     * @param array the array of nodes to search through
     * @param element the element to search for
     * @return the index of the first occurrence of the element in the array,
     *         or -1 if the element is not found
     */
    private int indexOf(Node<E>[] array, Object element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].element().equals(element)) {
                return i;
            }
        }
        return -1;
    }

    private record Node<E>(E element, int priority) {
    }
}