package torsete.util.entry;

import torsete.util.entry.util.EntryIterator;
import torsete.util.entry.util.IncludingEntryIterator;
import torsete.util.entry.util.LinkedEntryIterator;
import torsete.util.entry.util.ReaderEntryIterator;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Provides a set of utility methods to a set of {@link LinkedEntry <K,V>} (or in other words: An ordered set of {@link Map.Entry<K,V>}).
 * <p>
 * The entry key might be duplicated.
 * Ie. more than one entry can have the same key value (as illustrated by the {@link #map()} method.
 * <p>
 * Source of the entries might be set by one of the setSource methods. The source should be formatted as a {@link Properties} file.
 * <p>
 * The entries should be initialized by  ne of the append methods.
 * <p>
 * The entries are accessible by {@link #stream()}, {@link #getFirstEntry()}, {@link #getLastEntry()}, {@link #map()}, {@link #properties()}, ot {@link #getEntriesAsString()}
 * <p>
 * The entries are also accessible by {@link LinkedEntry#getSuccessor()}.
 *
 * @param <K> Key class
 * @param <V> value class
 */
public class LinkedEntries<K, V> {
    /**
     * Optional specifikaton of inclusion of source entries
     */
    private Predicate<LinkedEntry<K, V>> includePredicate;
    /**
     * Optional preprocessing of entries when appending entries
     */
    private List<Consumer<LinkedEntry<K, V>>> entryConsumers;

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
    private LinkedEntry<K, V> firstEntry;

    /**
     * Last entry
     */
    private LinkedEntry<K, V> lastEntry;

    public LinkedEntries() {
        entryConsumers = new ArrayList<>();
        includePredicate = entry -> false;
    }

    public LinkedEntries<K, V> addEntryConsumer(Consumer<LinkedEntry<K, V>> entryConsumer) {
        entryConsumers.add(entryConsumer);
        return this;
    }

    public LinkedEntries<K, V> setIncludePredicate(Predicate<LinkedEntry<K, V>> includePredicate) {
        this.includePredicate = includePredicate;
        return this;
    }

    public LinkedEntries<K, V> setSource(Reader reader) {
        this.reader = reader;
        return this;
    }

    public LinkedEntries<K, V> setSource(File file) throws FileNotFoundException {
        this.file = file;
        this.reader = new FileReader(file);
        return this;
    }

    public LinkedEntries<K, V> setSource(String string) {
        this.reader = new StringReader(string);
        return this;
    }

    public LinkedEntries<K, V> setSource(InputStream inputStream) {
        this.reader = new InputStreamReader(inputStream);
        return this;
    }

    /**
     * Appends source entries to the ordered entry set
     *
     * @return
     */
    public LinkedEntries<K, V> append() {
        EntryIterator<K, V> iterator = newSourceIterator();
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
    public LinkedEntries<K, V> append(K key, V value) {
        LinkedEntry<K, V> entry = new LinkedEntry<>(key, value);
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
    public Stream<LinkedEntry<K, V>> stream() {
        EntryIterator<K, V> iterator = newEntryIterator();
        Iterable<LinkedEntry<K, V>> iterable = () -> iterator;
        Stream<LinkedEntry<K, V>> stream = StreamSupport.stream(iterable.spliterator(), false).onClose(() -> iterator.close());
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
        EntryIterator<K, V> iterator = newEntryIterator();
        while (iterator.hasNext()) {
            LinkedEntry<K, V> linkedEntry = iterator.next();
            if (map.get(linkedEntry.getKey()) == null) {
                map.put(linkedEntry.getKey(), new ArrayList<>());
            }
            map.get(linkedEntry.getKey()).add(linkedEntry.getValue());
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
        EntryIterator<K, V> iterator = newEntryIterator();
        iterator.forEachRemaining(oe -> sb.append(oe.toString() + "\n"));
        return sb.toString();
    }


    public LinkedEntry<K, V> getFirstEntry() {
        return firstEntry;
    }

    public LinkedEntry<K, V> getLastEntry() {
        return lastEntry;
    }

    private EntryIterator<K, V> newEntryIterator() {
        EntryIterator<K, V> iterator = null;
        if (firstEntry != null) {
            iterator = newLinkedEntryIterator();
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
    private EntryIterator<K, V> newSourceIterator() {
        EntryIterator<K, V> iterator = null;
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
    private EntryIterator<K, V> newFileterator() {
        BiFunction<V, V, EntryIterator<K, V>> sourceFactoryFunction = (parentSource, source) -> {
            String sourceString = parentSource == null ?
                    file.getAbsolutePath() :
                    new File(parentSource.toString()).getParent() + File.separator + source;
            File sourceFile = new File(sourceString);
            try {
                return new ReaderEntryIterator<K, V>()
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
    private EntryIterator<K, V> newReaderIterator() {
        BiFunction<V, V, EntryIterator<K, V>> sourceFactoryFunction = (parentSource, source) -> {
            if (parentSource != null) {
                throw new UnsupportedOperationException("include is not supported");
            }
            return new ReaderEntryIterator<K, V>().setReader(reader);
        };
        return newIncludingIterator(sourceFactoryFunction);
    }

    /**
     * Iterates an including structure
     *
     * @param sourceFactoryFunction
     * @return
     */
    private EntryIterator<K, V> newIncludingIterator(BiFunction<V, V, EntryIterator<K, V>> sourceFactoryFunction) {
        EntryIterator<K, V> iterator = new IncludingEntryIterator<K, V>()
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
    private EntryIterator<K, V> newLinkedEntryIterator() {
        return new LinkedEntryIterator(firstEntry);
    }
}

