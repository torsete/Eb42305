package torsete.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A stack of {@EntryReader}'s. Each is responsible for fetching input from one source (file of entries)
 */
class EntryReaderStack<K, V> {
    private List<EntryReader<K, V>> entryReaders;

    public EntryReaderStack() {
        entryReaders = new ArrayList<>();
    }

    /**
     * Sustains no source duplicates in stack
     */
    public void push(EntryReader<K, V> entryreader) {
        Optional<V> any = entryReaders.stream()
                .map(entryReader -> entryReader.getSource())
                .filter(source -> source.equals(entryreader.getSource()))
                .findAny();
        if (any.isPresent()) {
            throw new IllegalArgumentException(entryreader.getSource() + " is self referencing");
        }
        entryReaders.add(entryreader);
    }

    public EntryReader<K, V> pop() {
        int topIndex = entryReaders.size() - 1;
        EntryReader entryReader = entryReaders.get(topIndex);
        entryReaders.remove(topIndex);
        return entryReader;
    }

    public EntryReader<K, V> top() {
        return entryReaders.get(entryReaders.size() - 1);
    }

    public boolean empty() {
        return entryReaders.size() == 0;
    }

}
