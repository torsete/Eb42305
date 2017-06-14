package torsete.util.entry;

import java.util.*;

/**
 * Created by Torsten on 18.05.2017.
 */
public class OrderedProperties extends OrderedEntries<Object, Object> {

    private Properties properties;

    public OrderedProperties() {
        super();
        properties = new Properties();
        setEntryConsumer(e -> {
            Object key = e.getKey();
            Object value = e.getValue();
            if (properties.get(key) == null) {
                put(key, new ArrayList<>());
            }
            get(key).add(value);
        });

    }

    public void put(Object key, List<Object> values) {
        properties.put(key, values);
    }

    public List<Object> get(Object key) {
        return ((ArrayList<Object>) properties.get(key));
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return properties.entrySet();
    }

    public int size() {
        return properties.size();
    }

    public Enumeration<?> propertyNames() {
        return properties.propertyNames();
    }


}
