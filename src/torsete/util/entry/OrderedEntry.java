package torsete.util.entry;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Torsten on 28.05.2017.
 */
public class OrderedEntry<K, V> {
    private Map.Entry<K, V> entry;
    private Integer lineNumber;
    private OrderedEntry successor;
    private V source;

    public OrderedEntry(Map.Entry<K, V> entry) {
        this.entry = entry;
    }

    public OrderedEntry(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        this.entry = map.entrySet().iterator().next();
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public OrderedEntry<K, V> setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
        return this;
    }

    public V getSource() {
        return source;
    }

    public OrderedEntry<K, V> setSource(V source) {
        this.source = source;
        return this;
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

    public void setEntry(Map.Entry<K, V> entry) {
        this.entry = entry;
    }

    public void setKey(K newKey) {
        Properties properties = new Properties();
        properties.put(newKey, getValue());
        setEntry((Map.Entry<K, V>) properties.entrySet().iterator().next());
    }

    public OrderedEntry<K, V> getSuccessor() {
        return successor;
    }

    public void setSuccessor(OrderedEntry<K, V> successor) {
        this.successor = successor;
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
