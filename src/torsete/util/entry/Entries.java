package torsete.util.entry;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Entries<K, V> {
    private Predicate<OrderedEntry<K, V>> includePredicate;
    private List<Consumer<OrderedEntry<K, V>>> entryConsumers;
    private boolean isTabsInKeyEnabled;

    public Entries() {
        clear();
    }

    public Entries clear() {
        entryConsumers = new ArrayList<>();
        enableTabsInKey(true);
        includePredicate = entry -> entry.getKey().toString().toLowerCase().contains("include");
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


    public Map<K, V> hashMap(Reader reader) {
        return runHashMap(() -> linked(reader));
    }

    public Map<K, V> hashMap(File file) {
        return runHashMap(() -> linked(file));
    }

    public Map<K, V> hashMap(String string) {
        return runHashMap(() -> linked(string));
    }

    public Map<K, V> hashMap(InputStream inputStream) {
        return runHashMap(() -> linked(inputStream));
    }


    public OrderedEntry<K, V> linked(Reader reader) {
        return runLinkedIterator(iterator(reader));
    }

    public OrderedEntry<K, V> linked(File file) {
        return runLinkedIterator(iterator(file));
    }

    public OrderedEntry<K, V> linked(String string) {
        return linked(new StringReader(string));
    }

    public OrderedEntry<K, V> linked(InputStream inputStream) {
        return linked(new InputStreamReader(inputStream));
    }


    public OrderedEntryIterator<K, V> iterator(Reader reader) {
        return newReaderOrderedEntryIterator(reader).open();
    }

    public OrderedEntryIterator<K, V> iterator(File file) {
        return newIncludingFileOrderedEntryIterator(file).open();
    }

    public OrderedEntryIterator<K, V> iterator(String string) {
        return iterator(new StringReader(string));
    }

    public OrderedEntryIterator<K, V> iterator(InputStream inputStream) {
        return iterator(new InputStreamReader(inputStream));
    }

    public OrderedEntryIterator<K, V> iterator(OrderedEntry<K, V> firstEntry) {
        return new LinkedOrderedEntryIterator(firstEntry).open();
    }


    public Stream<OrderedEntry<K, V>> stream(Reader reader) {
        return newStream(iterator(reader));
    }

    public Stream<OrderedEntry<K, V>> stream(File file) {
        return newStream(iterator(file));
    }

    public Stream<OrderedEntry<K, V>> stream(String string) {
        return newStream(iterator(string));
    }

    public Stream<OrderedEntry<K, V>> stream(InputStream inputStream) {
        return newStream(iterator(inputStream));
    }

    public Stream<OrderedEntry<K, V>> stream(OrderedEntry<K, V> firstEntry) {
        return newStream(iterator(firstEntry));
    }


    public String getOrderedAsString(OrderedEntry<K, V> firstEntry) {
        StringBuilder sb = new StringBuilder();
        runLinkedIterator(iterator(firstEntry).addEntryConsumer((oe -> sb.append(oe.toString() + "\n"))));
        return sb.toString();
    }

    private OrderedEntry<K, V> runLinkedIterator(OrderedEntryIterator<K, V> iterator) {
        OrderedEntry<K, V> firstEntry = iterator.getFirstEntry();
        iterator.forEachRemaining(oe -> oe.setSuccessor(iterator.lookAhead()));
        iterator.close();
        return firstEntry;
    }

    private Map<K, V> runHashMap(Runnable loadRunnable) {
        Map<K, V> map = new HashMap<>();
        Consumer<OrderedEntry<K, V>> entryConsumer = oe -> map.put(oe.getKey(), oe.getValue());
        addEntryConsumer(entryConsumer);
        loadRunnable.run();
        return map;
    }

    private Stream<OrderedEntry<K, V>> newStream(OrderedEntryIterator<K, V> iterator) {
        Iterable<OrderedEntry<K, V>> iterable = () -> iterator;
        Stream<OrderedEntry<K, V>> stream = StreamSupport.stream(iterable.spliterator(), false).onClose(() -> iterator.close());
        return stream;
    }

    private OrderedEntryIterator<K, V> newIncludingFileOrderedEntryIterator(File file) {
        BiFunction<V, V, ReaderOrderedEntryIterator<K, V>> sourceFactoryFunction = (parentSource, source) -> {
            String sourceString = parentSource == null ?
                    file.getAbsolutePath() :
                    new File(parentSource.toString()).getParent() + File.separator + source;
            File sourceFile = new File(sourceString);
            try {
                return (ReaderOrderedEntryIterator) new ReaderOrderedEntryIterator<K, V>()
                        .setReader(new FileReader(sourceFile))
                        .setSource((V) sourceFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
        return newIncludingOrderedEntryIterator(sourceFactoryFunction);
    }

    private OrderedEntryIterator<K, V> newReaderOrderedEntryIterator(Reader reader) {
        BiFunction<V, V, ReaderOrderedEntryIterator<K, V>> sourceFactoryFunction = (parentSource, source) -> {
            if (parentSource != null) {
                throw new UnsupportedOperationException("include is not supported");
            }
            return new ReaderOrderedEntryIterator<K, V>().setReader(reader);
        };
        return newIncludingOrderedEntryIterator(sourceFactoryFunction);
    }

    private OrderedEntryIterator<K, V> newIncludingOrderedEntryIterator(BiFunction<V, V, ReaderOrderedEntryIterator<K, V>> sourceFactoryFunction) {
        OrderedEntryIterator<K, V> iterator = new IncludingOrderedEntryIterator<K, V>()
                .enableTabsInKey(isTabsInKeyEnabled)
                .setSourceFactoryFunction(sourceFactoryFunction)
                .setIncludePredicate(includePredicate);
        entryConsumers.forEach(ec -> iterator.addEntryConsumer(ec));
        return iterator;
    }
}

