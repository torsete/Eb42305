package torsete.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * Created by Torsten on 18.05.2017.
 */
public class OrderedProperties extends Properties {

    private OrderedEntries orderedEntries;

    public OrderedProperties() {
        super();
        orderedEntries = new OrderedEntries().setEntryConsumer((e, l) -> {
            Object key = e.getKey();
            Object value = e.getValue();
            if (get(key) == null) {
                put(key, new ArrayList<>());
            }
            get(key).add(value);
        });

    }

    @Override
    public void clear() {
        super.clear();
        orderedEntries.clear();
    }


    @Override
    public synchronized void load(InputStream inputStream) throws IOException {
        orderedEntries.load(inputStream);
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        orderedEntries.load(reader);
    }

    public synchronized void load(File file) throws IOException {
        orderedEntries.load(file);
    }

    public synchronized void load(String string) throws IOException {
        StringReader stringReader = new StringReader(string);
        load(stringReader);
        stringReader.close();
    }

    @Override
    public List<Object> get(Object key) {
        return (List<Object>) super.get(key);
    }

    public OrderedProperties setIncludePredicate(Predicate<Map.Entry<Object, Object>> includeKeyPredicate) {
        orderedEntries.setIncludePredicate(includeKeyPredicate);
        return this;
    }

    public OrderedProperties enableDotsInKey(boolean enabled) {
        orderedEntries.enableDotsInKey(enabled);
        return this;
    }

    public OrderedProperties enableTabsInKey(boolean enabled) {
        orderedEntries.enableTabsInKey(enabled);
        return this;
    }

    public List<Map.Entry<Object, Object>> getEntries() {
        return orderedEntries.getEntries();
    }

    public Object readFirst(String key) {
        return orderedEntries.readFirst(key);
    }

    public int getOrderedEntriesIndex(String key) {
        return orderedEntries.getOrderedEntriesIndex(key);
    }

    public String getSourcename() {
        return orderedEntries.getSourcename();
    }


    public List<Integer> getLinenumbers() {
        return orderedEntries.getLinenumbers();
    }

    public boolean isUnique(String key) {
        return orderedEntries.isUnique(key);
    }

}
