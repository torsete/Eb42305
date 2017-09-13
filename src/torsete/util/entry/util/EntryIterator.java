package torsete.util.entry.util;

import torsete.util.entry.LinkedEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides iteration of entries.
 */
public abstract class EntryIterator<K, V> implements Iterator<LinkedEntry<K, V>> {

    protected LinkedEntry<K, V> firstEntry;
    protected LinkedEntry<K, V> lastEntry;
    protected LinkedEntry<K, V> nextEntry;
    /**
     * Optional identification of source
     */
    private V source;

    protected List<Consumer<LinkedEntry<K, V>>> entryConsumers;

    public EntryIterator() {
        entryConsumers = new ArrayList<>();
    }


    @Override
    public LinkedEntry<K, V> next() {
        LinkedEntry<K, V> entry = nextEntry;
        nextEntry = readEntry();
        entryConsumers.forEach(ec -> ec.accept(entry));
        if (nextEntry != null) {
            lastEntry = nextEntry;
        }
        return entry;
    }

    @Override
    public boolean hasNext() {
        return lookAhead() != null;
    }

    public EntryIterator<K, V> setSource(V source) {
        this.source = source;
        return this;
    }

    public V getSource() {
        return source;
    }

    public LinkedEntry<K, V> lookAhead() {
        return nextEntry;
    }

    public EntryIterator<K, V> open() {
        firstEntry = readEntry();
        nextEntry = firstEntry;
        return this;
    }

    public LinkedEntry<K, V> getFirstEntry() {
        return firstEntry;
    }

    public LinkedEntry<K, V> getLastEntry() {
        return lastEntry;
    }

    public void close() {
    }

    protected abstract LinkedEntry<K, V> readEntry();

    protected EntryIterator<K, V> setEntryConsumers(List<Consumer<LinkedEntry<K, V>>> entryConsumers) {
        this.entryConsumers = entryConsumers;
        return this;
    }

    public EntryIterator<K, V> addEntryConsumer(Consumer<LinkedEntry<K, V>> entryConsumer) {
        entryConsumers.add(entryConsumer);
        return this;
    }






}
