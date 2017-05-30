package torsete.util;

import java.util.Map;
import java.util.Properties;

/**
 * Created by Torsten on 28.05.2017.
 */
public class OrderedEntry<K, V> {
    private Map.Entry<K, V> entry;
    private int lineNumber;
    private OrderedEntry successor;

    public OrderedEntry(Map.Entry<K, V> entry, int lineNumber) {
        this.entry = entry;
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
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
        sb.append(lineNumber + ": " + entry);
        sb.append(successor == null ? " last" : " successor=" + successor.getEntry());
        return sb.toString();
    }


}
