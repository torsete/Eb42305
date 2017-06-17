package torsete.util.entry;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Entries<K, V> {
    private Predicate<OrderedEntry<K, V>> includePredicate;
    private List<Consumer<OrderedEntry<K, V>>> entryConsumers;
    private boolean isTabsInKeyEnabled;

    private Reader reader;
    private File file;
    private OrderedEntry<K, V> firstEntry;

    public Entries() {
        clear();
    }

    public Entries clear() {
        reader = null;
        file = null;
        firstEntry = null;
        entryConsumers = new ArrayList<>();
        isTabsInKeyEnabled = false;
        includePredicate = entry -> false;
        return this;
    }

    public Entries<K, V> addEntryConsumer(Consumer<OrderedEntry<K, V>> entryConsumer) {
        entryConsumers.add(entryConsumer);
        return this;
    }


    public Entries<K, V> enableTabsInKey(boolean enabled) {
        isTabsInKeyEnabled = enabled;
        return this;
    }

    public Entries<K, V> setIncludePredicate(Predicate<OrderedEntry<K, V>> includePredicate) {
        this.includePredicate = includePredicate;
        return this;
    }

    public Entries<K, V> setSource(Reader reader) {
        this.reader = reader;
        return this;
    }

    public Entries<K, V> setSource(File file) throws FileNotFoundException {
        this.file = file;
        this.reader = new FileReader(file);
        return this;
    }

    public Entries<K, V> setSource(String string) {
        this.reader = new StringReader(string);
        return this;
    }

    public Entries<K, V> setSource(InputStream inputStream) {
        this.reader = new InputStreamReader(inputStream);
        return this;
    }


    public Stream<OrderedEntry<K, V>> stream() {
        OrderedEntryIterator<K, V> iterator = iterator();
        Iterable<OrderedEntry<K, V>> iterable = () -> iterator;
        Stream<OrderedEntry<K, V>> stream = StreamSupport.stream(iterable.spliterator(), false).onClose(() -> iterator.close());
        return stream;
    }

    public Map<K, V> hashMap() {
        Map<K, V> map = new HashMap<>();
        stream().forEach(oe -> map.put(oe.getKey(), oe.getValue()));
        return map;
    }

    public Properties properties() {
        Properties properties = new Properties();
        stream().forEach(oe -> properties.put(oe.getKey(), oe.getValue()));
        return properties;
    }

    public Entries<K, V> load() {
        OrderedEntryIterator<K, V> iterator = iterator();
        iterator.forEachRemaining(oe -> oe.setSuccessor(iterator.lookAhead()));
        iterator.close();
        return this;
    }

    public OrderedEntryIterator<K, V> iterator() {
        OrderedEntryIterator<K, V> iterator = null;
        if (firstEntry != null) {
            iterator = newLinkedOrderedEntryIterator();
        } else if (file != null) {
            iterator = newFileterator();
        } else if (reader != null) {
            iterator = newReaderIterator();
        } else {
            throw new RuntimeException("Source not defined");
        }
        iterator.open();
        firstEntry = iterator.getFirstEntry();
        return iterator;
    }

    public String getEntriesAsString() {
        StringBuilder sb = new StringBuilder();
        OrderedEntryIterator<K, V> iterator = iterator();
        iterator.forEachRemaining(oe -> sb.append(oe.toString() + "\n"));
        return sb.toString();
    }


    public OrderedEntry<K, V> getFirstEntry() {
        return firstEntry;
    }

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

    private OrderedEntryIterator<K, V> newReaderIterator() {
        BiFunction<V, V, OrderedEntryIterator<K, V>> sourceFactoryFunction = (parentSource, source) -> {
            if (parentSource != null) {
                throw new UnsupportedOperationException("include is not supported");
            }
            return new ReaderOrderedEntryIterator<K, V>().setReader(reader);
        };
        return newIncludingIterator(sourceFactoryFunction);
    }

    private OrderedEntryIterator<K, V> newIncludingIterator(BiFunction<V, V, OrderedEntryIterator<K, V>> sourceFactoryFunction) {
        OrderedEntryIterator<K, V> iterator = new IncludingOrderedEntryIterator<K, V>()
                .enableTabsInKey(isTabsInKeyEnabled)
                .setSourceFactoryFunction(sourceFactoryFunction)
                .setIncludePredicate(includePredicate);
        entryConsumers.forEach(ec -> iterator.addEntryConsumer(ec));
        return iterator;
    }

    private OrderedEntryIterator<K, V> newLinkedOrderedEntryIterator() {
        return new LinkedOrderedEntryIterator(firstEntry);
    }
}

