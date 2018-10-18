package data;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Keep a buffer of some objects. Oldest gets thrown out to keep the size
 * at/below a maximum. Using an iterator, you can go through it LIFO-style.
 *
 * Based around an {@link ArrayDeque}, but with push being the only way to add elements.
 *
 * @param <T>
 * @author matt
 */
public class LIFOFixedSize<T> {

    /**
     * Actually handles the data storage. However, we restrict access to the methods we want.
     */
    private ArrayDeque<T> deque = new ArrayDeque<>();

    /**
     * Maximum number of elements held by this before throwing old ones out.
     */
    private final int maxSize;

    /**
     * Make a new fixed size last-in-first-out queue.
     * @param maxSize Maximum number of elements held by this before throwing old ones out.
     * @throws IllegalArgumentException Trying to make a LIFO queue with less than 1 element allowed.
     */
    public LIFOFixedSize(int maxSize) {
        if (maxSize < 1)
            throw new IllegalArgumentException("Must allow at least one element.");

        this.maxSize = maxSize;
    }

    /**
     * Add a new element to the head of the deque.
     * @param newElement New element to add.
     */
    public void push(T newElement) {
        deque.push(newElement);
        while (deque.size() > maxSize) {
            deque.pollLast();
        }
    }

    /**
     * Check whether the deque is empty.
     * @return Whether the deque is empty.
     */
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    /**
     * Get the element at the head of the deque without removing it.
     * @return Element at the head of the deque.
     */
    public T peek() {
        return deque.peek();
    }

    /**
     * Get an iterator for this deque.
     * @return Iterator object for the deque.
     */
    public Iterator<T> iterator() {
        return deque.iterator();
    }

    /**
     * Get the element from the head of the deque and remove it.
     * @return Element at the head of the deque.
     */
    public T poll() {
        return deque.poll();
    }

    /**
     * Get the number of elements contained.
     * @return Number of elements contained.
     */
    public int size() {
        return deque.size();
    }
}

