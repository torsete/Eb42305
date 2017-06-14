package torsete.util.entry;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Provides iteration of entries.
 */
public abstract class OrderedEntryIterator<K, V> implements Iterator<OrderedEntry<K, V>> {

    private OrderedEntry<K, V> nextEntry;
    /**
     * Optional identification of source
     */
    private V source;
    private Consumer<OrderedEntry<K, V>> entryConsumer;

    public OrderedEntryIterator() {
        entryConsumer = e -> {
        };
    }

    public OrderedEntryIterator<K, V> setSource(V source) {
        this.source = source;
        return this;
    }

    public V getSource() {
        return source;
    }


    public OrderedEntryIterator<K, V> setEntryConsumer(Consumer<OrderedEntry<K, V>> entryConsumer) {
        this.entryConsumer = entryConsumer;
        return this;
    }

    @Override
    public OrderedEntry<K, V> next() {
        OrderedEntry<K, V> entry = nextEntry;
        nextEntry = readEntry();
        if (entry != null) {
            entryConsumer.accept(entry);
        }
        return entry;
    }

    @Override
    public boolean hasNext() {
        return lookAhead() != null;
    }

    public OrderedEntry<K, V> lookAhead() {
        return nextEntry;
    }

    public OrderedEntryIterator<K, V> open() {
        nextEntry = readEntry();
        return this;
    }


    public void close() {
    }

    protected abstract OrderedEntry<K, V> readEntry();

}
