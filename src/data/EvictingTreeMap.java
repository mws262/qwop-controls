package data;

import java.util.TreeMap;

/**
 * TreeMap with limited size. Only keeps the elements with SMALLEST KEYS. Also trying to be thread-safe.
 *
 * @param <K>
 * @param <V>
 * @author matt
 */
public class EvictingTreeMap<K, V> extends TreeMap<K, V> {

    private static final long serialVersionUID = 1L;
    private final int maxSize;

    public EvictingTreeMap(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    @Override
    public synchronized V put(K key, V value) {
        super.put(key, value);
        while (this.size() > maxSize) {
            this.pollLastEntry();
        }

        return null; // Return isn't particularly important.
    }

}
