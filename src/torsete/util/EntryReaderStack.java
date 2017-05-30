package torsete.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Torsten on 28.05.2017.
 */
class EntryReaderStack<K, V> {
    private List<EntryReader<K, V>> entryReaders;

    public EntryReaderStack() {
        entryReaders = new ArrayList<>();
    }

    public void push(EntryReader<K, V> entryreader) {
        Optional<V> any = entryReaders.stream()
                .map(er -> er.getSource())
                .filter(s -> s.equals(entryreader.getSource()))
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
        return size() == 0;
    }

    public int size() {
        return entryReaders.size();
    }

}
