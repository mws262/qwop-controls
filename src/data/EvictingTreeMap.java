package data;

import java.util.TreeMap;

/**
 * TreeMap with limited size. Only keeps the elements with the smallest keys. Also trying to be thread-safe.
 *
 * @param <K>
 * @param <V>
 * @author matt
 */
public class EvictingTreeMap<K, V> extends TreeMap<K, V> {

    private static final long serialVersionUID = 1L;

    /**
     * Maximum number of allowed elements in this map before smaller ones get thrown out.
     */
    private final int maxSize;

    /**
     * Make a new map which keeps a limited number of elements.
     *
     * @param maxSize Maximum number of elements this map will hold before throwing some out.
     */
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
