package torsete.util.entry;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * An ordered {@link Map.Entry}. The ordering of a set of {@link Map.Entry} is implemented by a successor for each element of the set
 */
public class LinkedEntry<K, V> {
    /**
     * The entry
     */
    private Map.Entry<K, V> entry;
    /**
     * Succesor successor of the entry. Is null for the last element
     */
    private LinkedEntry successor;
    /**
     * Optional decoration of the element. Null if not present
     */
    private Integer lineNumber;
    /**
     * Optional decoration of the element. Null if not present
     */
    private V source;

    public LinkedEntry(Map.Entry<K, V> entry) {
        this.entry = entry;
    }

    public LinkedEntry(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        this.entry = map.entrySet().iterator().next();
    }


    public K getKey() {
        return entry.getKey();
    }

    public V getValue() {
        return entry.getValue();
    }

    public Map.Entry<K, V> getEntry() {
        return entry;
    }

    /**
     * @return Null if this is the last element
     */
    public LinkedEntry<K, V> getSuccessor() {
        return successor;
    }

    public void setSuccessor(LinkedEntry<K, V> successor) {
        this.successor = successor;
    }

    public void setEntry(Map.Entry<K, V> entry) {
        this.entry = entry;
    }

    /**
     * "Replaces" the entry key.
     * <p>
     * The existing entry is overwritten by a new entry with the same value and a new key.
     *
     * @param newKey
     */
    public void replaceKey(K newKey) {
        Properties properties = new Properties();
        properties.put(newKey, getValue());
        setEntry((Map.Entry<K, V>) properties.entrySet().iterator().next());
    }

    /**
     * @return Null if not present
     */
    public Integer getLineNumber() {
        return lineNumber;
    }

    public LinkedEntry<K, V> setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
        return this;
    }

    /**
     * @return Null if not present
     */
    public V getSource() {
        return source;
    }

    public LinkedEntry<K, V> setSource(V source) {
        this.source = source;
        return this;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(source == null ? "" : (source.toString() + " :"));
        sb.append(lineNumber == null ? "" : (lineNumber.toString() + ": "));
        sb.append(entry.toString());
        sb.append(successor == null ? "" : " successor=" + successor.getEntry());
        return sb.toString();
    }


}
