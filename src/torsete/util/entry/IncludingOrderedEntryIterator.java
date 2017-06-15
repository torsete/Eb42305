package torsete.util.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;


class IncludingOrderedEntryIterator<K, V> extends OrderedEntryIterator<K, V> {
    private OrderedEntryIteratorStack<K, V> iteratorStack;
    private Predicate<OrderedEntry<K, V>> includePredicate;
    private BiFunction<V, V, ReaderOrderedEntryIterator<K, V>> sourceFactoryFunction;
    private boolean tabsInKeyEnabled;
    private List<ReaderOrderedEntryIterator<K, V>> toBeClosedIterators;

    public IncludingOrderedEntryIterator() {
        iteratorStack = new OrderedEntryIteratorStack<>();
        toBeClosedIterators = new ArrayList<>();
    }

    public IncludingOrderedEntryIterator<K, V> setSourceFactoryFunction(BiFunction<V, V, ReaderOrderedEntryIterator<K, V>> sourceFactoryFunction) {
        this.sourceFactoryFunction = sourceFactoryFunction;
        return this;
    }

    public IncludingOrderedEntryIterator<K, V> setIncludePredicate(Predicate<OrderedEntry<K, V>> includePredicate) {
        this.includePredicate = includePredicate;
        return this;
    }

    public IncludingOrderedEntryIterator<K, V> enableTabsInKey(boolean enabled) {
        tabsInKeyEnabled = enabled;
        return this;
    }


    public void close() {
        toBeClosedIterators.forEach(i -> i.close());
    }

    public OrderedEntryIterator<K, V> open() {
        return open(getSource());
    }

    public OrderedEntryIterator<K, V> open(V source) {
        ReaderOrderedEntryIterator<K, V> kvReaderOrderedEntryIterator = newOrderedEntryIterator(source);
        final V newSource = kvReaderOrderedEntryIterator.getSource();
        Optional<V> any = iteratorStack.stream()
                .map(entryReader -> entryReader.getSource())
                .filter(s -> newSource != null && s != null && s.equals(newSource))
                .findAny();
        if (any.isPresent()) {
            throw new IllegalArgumentException(newSource + " is self referencing");
        }

        iteratorStack.push(kvReaderOrderedEntryIterator);
        super.open();
        return this;
    }

    @Override
    protected OrderedEntry<K, V> readEntry() {
        OrderedEntry<K, V> entry = iteratorStack.top().readEntry();
        while (entry == null) {
            toBeClosedIterators.add(iteratorStack.top());
            iteratorStack.pop();
            if (iteratorStack.empty()) {
                return null;
            } else {
                entry = iteratorStack.top().readEntry();
            }
        }
        if (includePredicate.test(entry)) {
            open(entry.getValue());
            return iteratorStack.top().nextEntry;
        }
        return entry;
    }

    private ReaderOrderedEntryIterator<K, V> newOrderedEntryIterator(V source) {
        ReaderOrderedEntryIterator<K, V> iterator;
        if (iteratorStack.empty()) {
            iterator = sourceFactoryFunction.apply(null, source);
        } else {
            iterator = sourceFactoryFunction.apply(iteratorStack.top().getSource(), source);
        }
        return (ReaderOrderedEntryIterator) iterator.enableTabsInKey(tabsInKeyEnabled).setEntryConsumers(entryConsumers);

    }

    class OrderedEntryIteratorStack<K, V> {
        private List<ReaderOrderedEntryIterator<K, V>> iterators;

        public OrderedEntryIteratorStack() {
            iterators = new ArrayList<>();
        }

        /**
         * Sustains no source duplicates in stack
         */
        public void push(ReaderOrderedEntryIterator<K, V> iterator) {
            iterators.add(iterator);
        }

        public ReaderOrderedEntryIterator<K, V> pop() {
            int topIndex = iterators.size() - 1;
            ReaderOrderedEntryIterator iterator = iterators.get(topIndex);
            iterators.remove(topIndex);
            return iterator;
        }

        public ReaderOrderedEntryIterator<K, V> top() {
            return iterators.get(iterators.size() - 1);
        }

        public boolean empty() {
            return iterators.size() == 0;
        }

        public Stream<ReaderOrderedEntryIterator<K, V>> stream() {
            return iterators.stream();
        }

    }
}
