package torsete.util.entry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides iteration of entries.
 */
public abstract class OrderedEntryIterator<K, V> implements Iterator<OrderedEntry<K, V>> {

    protected OrderedEntry<K, V> firstEntry;
    protected OrderedEntry<K, V> nextEntry;
    /**
     * Optional identification of source
     */
    private V source;

    protected List<Consumer<OrderedEntry<K, V>>> entryConsumers;

    public OrderedEntryIterator() {
        entryConsumers = new ArrayList<>();
    }


    @Override
    public OrderedEntry<K, V> next() {
        OrderedEntry<K, V> entry = nextEntry;
        nextEntry = readEntry();
        entryConsumers.forEach(ec -> ec.accept(entry));
        return entry;
    }

    @Override
    public boolean hasNext() {
        return lookAhead() != null;
    }

    public OrderedEntryIterator<K, V> setSource(V source) {
        this.source = source;
        return this;
    }

    public V getSource() {
        return source;
    }

    public OrderedEntry<K, V> lookAhead() {
        return nextEntry;
    }

    public OrderedEntryIterator<K, V> open() {
        firstEntry = readEntry();
        nextEntry = firstEntry;
        return this;
    }

    public OrderedEntry<K, V> getFirstEntry() {
        return firstEntry;
    }

    public void close() {
    }

    protected abstract OrderedEntry<K, V> readEntry();

    protected OrderedEntryIterator<K, V> setEntryConsumers(List<Consumer<OrderedEntry<K, V>>> entryConsumers) {
        this.entryConsumers = entryConsumers;
        return this;
    }

    public OrderedEntryIterator<K, V> addEntryConsumer(Consumer<OrderedEntry<K, V>> entryConsumer) {
        entryConsumers.add(entryConsumer);
        return this;
    }






}
