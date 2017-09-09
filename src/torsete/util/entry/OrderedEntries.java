package torsete.util.entry;

import torsete.util.entry.util.IncludingOrderedEntryIterator;
import torsete.util.entry.util.LinkedOrderedEntryIterator;
import torsete.util.entry.util.OrderedEntryIterator;
import torsete.util.entry.util.ReaderOrderedEntryIterator;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An set of {@link OrderedEntry<K,V>} (or in other words: An ordered set of {@link Map.Entry<K,V>}).
 * The entry key might be duplicated. Ie. more than one entry can have the same key value (as illustrated by the {@link #map()} method.
 * <p>
 * Source of the entries might be set by one of the setSource methods. The source should be formatted as a {@link Properties} file.
 * <p>
 * The entries should be initialized by  ne of the append methods.
 * <p>
 * The entries are accessible by {@link #stream()}, {@link #getFirstEntry()}, {@link #getLastEntry()}, {@link #map()}, {@link #properties()}, ot {@link #getEntriesAsString()}
 * <p>
 * The entries are also accessible by {@link OrderedEntry#getSuccessor()}.
 *
 * @param <K> Key class
 * @param <V> value class
 */
public class OrderedEntries<K, V> {
    /**
     * Optional specifikaton of inclusion of source entries
     */
    private Predicate<OrderedEntry<K, V>> includePredicate;
    /**
     * Optional preprocessing of entries when appending entries
     */
    private List<Consumer<OrderedEntry<K, V>>> entryConsumers;

    /**
     * Optional source
     */
    private Reader reader;
    /**
     * Optional source
     */
    private File file;

    /**
     * First entry
     */
    private OrderedEntry<K, V> firstEntry;

    /**
     * Last entry
     */
    private OrderedEntry<K, V> lastEntry;

    public OrderedEntries() {
        entryConsumers = new ArrayList<>();
        includePredicate = entry -> false;
    }

    public OrderedEntries<K, V> addEntryConsumer(Consumer<OrderedEntry<K, V>> entryConsumer) {
        entryConsumers.add(entryConsumer);
        return this;
    }

    public OrderedEntries<K, V> setIncludePredicate(Predicate<OrderedEntry<K, V>> includePredicate) {
        this.includePredicate = includePredicate;
        return this;
    }

    public OrderedEntries<K, V> setSource(Reader reader) {
        this.reader = reader;
        return this;
    }

    public OrderedEntries<K, V> setSource(File file) throws FileNotFoundException {
        this.file = file;
        this.reader = new FileReader(file);
        return this;
    }

    public OrderedEntries<K, V> setSource(String string) {
        this.reader = new StringReader(string);
        return this;
    }

    public OrderedEntries<K, V> setSource(InputStream inputStream) {
        this.reader = new InputStreamReader(inputStream);
        return this;
    }

    /**
     * Appends source entries to the ordered entry set
     *
     * @return
     */
    public OrderedEntries<K, V> append() {
        OrderedEntryIterator<K, V> iterator = newSourceIterator();
        iterator.forEachRemaining(oe -> oe.setSuccessor(iterator.lookAhead()));
        iterator.close();
        lastEntry = iterator.getLastEntry();
        return this;
    }

    /**
     * Appends an entry to the ordered entry set
     *
     * @param key
     * @param value
     * @return
     */
    public OrderedEntries<K, V> append(K key, V value) {
        OrderedEntry<K, V> entry = new OrderedEntry<>(key, value);
        entryConsumers.forEach(ec -> ec.accept(entry));
        if (firstEntry == null) {
            firstEntry = entry;
        } else {
            lastEntry.setSuccessor(entry);
        }
        lastEntry = entry;
        return this;
    }

    /**
     * @return All entries
     */
    public Stream<OrderedEntry<K, V>> stream() {
        OrderedEntryIterator<K, V> iterator = newEntryIterator();
        Iterable<OrderedEntry<K, V>> iterable = () -> iterator;
        Stream<OrderedEntry<K, V>> stream = StreamSupport.stream(iterable.spliterator(), false).onClose(() -> iterator.close());
        lastEntry = iterator.getLastEntry();
        return stream;
    }


    /**
     * Creates a map of the ordered entries.
     * <p>
     * If you need to access the entries by a key, this method might be the answer
     *
     * @return Never null
     */
    public HashMap<K, List<V>> map() {
        HashMap<K, List<V>> map = new HashMap<>();
        OrderedEntryIterator<K, V> iterator = newEntryIterator();
        while (iterator.hasNext()) {
            OrderedEntry<K, V> orderedEntry = iterator.next();
            if (map.get(orderedEntry.getKey()) == null) {
                map.put(orderedEntry.getKey(), new ArrayList<>());
            }
            map.get(orderedEntry.getKey()).add(orderedEntry.getValue());
        }
        return map;
    }

    /**
     * Warning! The method {@link #map} is the right choice.
     * <p>
     * Creates a Properties of the ordered entries.
     * <p>
     * Note. Duplicated keys are NOT handled
     * <p>
     * If you need to access the entries by a key, this method might be the answer
     *
     * @return Never null
     */
    public Properties properties() {
        Properties properties = new Properties();
        stream().forEach(oe -> properties.put(oe.getKey(), oe.getValue()));
        return properties;
    }

    public String getEntriesAsString() {
        StringBuilder sb = new StringBuilder();
        OrderedEntryIterator<K, V> iterator = newEntryIterator();
        iterator.forEachRemaining(oe -> sb.append(oe.toString() + "\n"));
        return sb.toString();
    }


    public OrderedEntry<K, V> getFirstEntry() {
        return firstEntry;
    }

    public OrderedEntry<K, V> getLastEntry() {
        return lastEntry;
    }

    private OrderedEntryIterator<K, V> newEntryIterator() {
        OrderedEntryIterator<K, V> iterator = null;
        if (firstEntry != null) {
            iterator = newLinkedOrderedEntryIterator();
        }
        if (iterator == null) {
            return newSourceIterator();
        }
        iterator.open();
        firstEntry = iterator.getFirstEntry();
        return iterator;
    }

    /**
     * Iterates the source set by {@link #setSource(File)} or {@link #setSource(Reader)}
     *
     * @return
     */
    private OrderedEntryIterator<K, V> newSourceIterator() {
        OrderedEntryIterator<K, V> iterator = null;
        if (file != null) {
            iterator = newFileterator();
        } else if (reader != null) {
            iterator = newReaderIterator();
        } else {
            throw new RuntimeException("Source not defined");
        }
        iterator.open();
        if (firstEntry == null) {
            firstEntry = iterator.getFirstEntry();
        } else {
            lastEntry.setSuccessor(iterator.getFirstEntry());
        }
        return iterator;
    }

    /**
     * Iterates a file
     *
     * @return
     */
    private OrderedEntryIterator<K, V> newFileterator() {
        BiFunction<V, V, OrderedEntryIterator<K, V>> sourceFactoryFunction = (parentSource, source) -> {
            String sourceString = parentSource == null ?
                    file.getAbsolutePath() :
                    new File(parentSource.toString()).getParent() + File.separator + source;
            File sourceFile = new File(sourceString);
            try {
                return new ReaderOrderedEntryIterator<K, V>()
                        .setReader(new FileReader(sourceFile))
                        .setSource((V) sourceFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
        return newIncludingIterator(sourceFactoryFunction);
    }

    /**
     * Iterates a reader
     *
     * @return
     */
    private OrderedEntryIterator<K, V> newReaderIterator() {
        BiFunction<V, V, OrderedEntryIterator<K, V>> sourceFactoryFunction = (parentSource, source) -> {
            if (parentSource != null) {
                throw new UnsupportedOperationException("include is not supported");
            }
            return new ReaderOrderedEntryIterator<K, V>().setReader(reader);
        };
        return newIncludingIterator(sourceFactoryFunction);
    }

    /**
     * Iterates an including structure
     *
     * @param sourceFactoryFunction
     * @return
     */
    private OrderedEntryIterator<K, V> newIncludingIterator(BiFunction<V, V, OrderedEntryIterator<K, V>> sourceFactoryFunction) {
        OrderedEntryIterator<K, V> iterator = new IncludingOrderedEntryIterator<K, V>()
                .setSourceFactoryFunction(sourceFactoryFunction)
                .setIncludePredicate(includePredicate);
        entryConsumers.forEach(ec -> iterator.addEntryConsumer(ec));
        return iterator;
    }

    /**
     * Iterates the currently appended vaues
     *
     * @return
     */
    private OrderedEntryIterator<K, V> newLinkedOrderedEntryIterator() {
        return new LinkedOrderedEntryIterator(firstEntry);
    }
}

