package torsete.util.entry;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class OrderedEntries<K, V> {
    private OrderedEntry<K, V> firstEntry;
    private Predicate<OrderedEntry<K, V>> includePredicate;
    private Consumer<OrderedEntry<K, V>> entryConsumer;
    private Consumer<OrderedEntry<K, V>> handleDotsInKey;
    private boolean isTabsInKeyEnabled;

    public OrderedEntries() {
        clear();
    }

    public synchronized OrderedEntries clear() {
        enableDotsInKey(true);
        enableTabsInKey(true);
        includePredicate = entry -> entry.getKey().toString().toLowerCase().contains("include");
        entryConsumer = oe -> {
        };
        firstEntry = null;
        return this;
    }

    public OrderedEntries<K, V> enableDotsInKey(boolean enabled) {
        handleDotsInKey = enabled ? new DottedEntryKey() : e -> {
        };
        return this;
    }

    public OrderedEntries<K, V> enableTabsInKey(boolean enabled) {
        isTabsInKeyEnabled = enabled;
        return this;
    }


    public synchronized OrderedEntries<K, V> setIncludePredicate(Predicate<OrderedEntry<K, V>> includePredicate) {
        this.includePredicate = includePredicate;
        return this;
    }


    public OrderedEntries<K, V> setEntryConsumer(Consumer<OrderedEntry<K, V>> entryConsumer) {
        this.entryConsumer = entryConsumer;
        return this;
    }

    public OrderedEntry<K, V> getFirstOrderedEntry() {
        return firstEntry;
    }


    public synchronized OrderedEntries<K, V> load(Reader reader) {
        runIterator(newReaderOrderedEntryIterator(reader));
        return this;
    }

    public OrderedEntries<K, V> load(File file) {
        runIterator(newIncludingFileOrderedEntryIterator(file));
        return this;
    }

    public synchronized OrderedEntries<K, V> load(String string) {
        return load(new StringReader(string));
    }

    public synchronized OrderedEntries<K, V> load(InputStream inputStream) {
        return load(new InputStreamReader(inputStream));
    }

    public OrderedEntryIterator<K, V> iterator(Reader reader) {
        return newReaderOrderedEntryIterator(reader).open();
    }

    public OrderedEntryIterator<K, V> iterator(File file) {
        return newIncludingFileOrderedEntryIterator(file).open();
    }

    public OrderedEntryIterator<K, V> iterator() {
        return new LinkedOrderedEntryIterator(firstEntry).open();
    }

    public Stream<OrderedEntry<K, V>> stream(Reader reader) {
        return newStream(newReaderOrderedEntryIterator(reader));
    }

    public synchronized Stream<OrderedEntry<K, V>> stream(File file) {
        return newStream(newIncludingFileOrderedEntryIterator(file));
    }

    public synchronized Stream<OrderedEntry<K, V>> stream(String string) {
        return stream(new StringReader(string));
    }

    public synchronized Stream<OrderedEntry<K, V>> stream(InputStream inputStream) {
        return stream(new InputStreamReader(inputStream));
    }


    public Stream<OrderedEntry<K, V>> stream() {
        return newStream(new LinkedOrderedEntryIterator<>(firstEntry));
    }


    public String getOrderedAsString() {
        StringBuilder sb = new StringBuilder();
        OrderedEntry<K, V> orderedEntry = firstEntry;
        while (orderedEntry != null) {
            sb.append(orderedEntry.toString() + "\n");
            orderedEntry = orderedEntry.getSuccessor();
        }
        return sb.toString();
    }

    private void runIterator(OrderedEntryIterator<K, V> iterator) {
        iterator.open();
        OrderedEntry<K, V> entry = null;
        while (iterator.hasNext()) {
            OrderedEntry<K, V> next = iterator.next();
            if (firstEntry == null) {
                firstEntry = next;
            }
            if (entry != null) {
                entry.setSuccessor(next);
            }
            entry = next;
        }
    }

    private Stream<OrderedEntry<K, V>> newStream(OrderedEntryIterator<K, V> iterator) {
        iterator.open();
        Iterable<OrderedEntry<K, V>> iterable = () -> iterator;
        Stream<OrderedEntry<K, V>> stream = StreamSupport.stream(iterable.spliterator(), false);
        return stream;
    }

    private OrderedEntryIterator<K, V> newIncludingFileOrderedEntryIterator(File file) {
        return new IncludingFileOrderedEntryIterator<K, V>()
                .setSource((V) file.getAbsolutePath())
                .enableTabsInKey(isTabsInKeyEnabled)
                .setHandleDotsInKey(handleDotsInKey)
                .setIncludePredicate(includePredicate)
                .setEntryConsumer(entryConsumer);
    }

    private OrderedEntryIterator<K, V> newReaderOrderedEntryIterator(Reader reader) {
        return new ReaderOrderedEntryIterator<K, V>()
                .setReader(reader)
                .enableTabsInKey(isTabsInKeyEnabled)
                .setHandleDotsInKey(handleDotsInKey)
                .setEntryConsumer(entryConsumer);
    }
}

