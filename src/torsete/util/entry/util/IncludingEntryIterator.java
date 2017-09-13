package torsete.util.entry.util;

import torsete.util.entry.LinkedEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class IncludingEntryIterator<K, V> extends EntryIterator<K, V> {
    private OrderedEntryIteratorStack<K, V> iteratorStack;
    private Predicate<LinkedEntry<K, V>> includePredicate;
    private BiFunction<V, V, EntryIterator<K, V>> sourceFactoryFunction;
    private List<EntryIterator<K, V>> toBeClosedIterators;

    public IncludingEntryIterator() {
        iteratorStack = new OrderedEntryIteratorStack<>();
        toBeClosedIterators = new ArrayList<>();
    }

    public IncludingEntryIterator<K, V> setSourceFactoryFunction(BiFunction<V, V, EntryIterator<K, V>> sourceFactoryFunction) {
        this.sourceFactoryFunction = sourceFactoryFunction;
        return this;
    }

    public IncludingEntryIterator<K, V> setIncludePredicate(Predicate<LinkedEntry<K, V>> includePredicate) {
        this.includePredicate = includePredicate;
        return this;
    }

    public void close() {
        toBeClosedIterators.forEach(i -> i.close());
    }

    public EntryIterator<K, V> open() {
        return open(getSource());
    }

    public EntryIterator<K, V> open(V source) {
        EntryIterator<K, V> iterator = newOrderedEntryIterator(source);

        final V newSource = iterator.getSource();
        Optional<V> any = iteratorStack.stream()
                .map(entryReader -> entryReader.getSource())
                .filter(s -> newSource != null && s != null && s.equals(newSource))
                .findAny();
        if (any.isPresent()) {
            throw new IllegalArgumentException(newSource + " is self referencing");
        }

        iteratorStack.push(iterator);
        super.open();
        return this;
    }

    @Override
    protected LinkedEntry<K, V> readEntry() {
        LinkedEntry<K, V> entry = iteratorStack.top().readEntry();
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

    private EntryIterator<K, V> newOrderedEntryIterator(V source) {
        EntryIterator<K, V> iterator;
        if (iteratorStack.empty()) {
            iterator = sourceFactoryFunction.apply(null, source);
        } else {
            iterator = sourceFactoryFunction.apply(iteratorStack.top().getSource(), source);
        }
        return iterator.setEntryConsumers(entryConsumers);

    }

    class OrderedEntryIteratorStack<K, V> {
        private List<EntryIterator<K, V>> iterators;

        public OrderedEntryIteratorStack() {
            iterators = new ArrayList<>();
        }

        /**
         * Sustains no source duplicates in stack
         */
        public void push(EntryIterator<K, V> iterator) {
            iterators.add(iterator);
        }

        public EntryIterator<K, V> pop() {
            int topIndex = iterators.size() - 1;
            EntryIterator iterator = iterators.get(topIndex);
            iterators.remove(topIndex);
            return iterator;
        }

        public EntryIterator<K, V> top() {
            return iterators.get(iterators.size() - 1);
        }

        public boolean empty() {
            return iterators.size() == 0;
        }

        public Stream<EntryIterator<K, V>> stream() {
            return iterators.stream();
        }

    }
}
