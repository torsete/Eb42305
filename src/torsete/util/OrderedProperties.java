package torsete.util;

import java.util.*;

/**
 * Created by Torsten on 18.05.2017.
 */
public class OrderedProperties extends OrderedEntries {

    private Properties properties;

    public OrderedProperties() {
        super();
        properties = new Properties();
        setEntryConsumer((e1, e2) -> {
            Object key = e1.getKey();
            Object value = e1.getValue();
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
