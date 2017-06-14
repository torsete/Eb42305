package torsete.util.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;


class IncludingOrderedEntryIterator<K, V> extends OrderedEntryIterator<K, V> {
    private OrderedEntryIteratorStack<K, V> iteratorStack;
    private Predicate<OrderedEntry<K, V>> includePredicate;
    private BiFunction<V, V, OrderedEntryIterator<K, V>> iteratorFactoryFunction;
    private Consumer<OrderedEntry<K, V>> handleDotsInKey;

    public IncludingOrderedEntryIterator() {
        includePredicate = entry -> false; // entry.getKey().toString().toLowerCase().contains("include");
        iteratorStack = new OrderedEntryIteratorStack<>();
        handleDotsInKey = e -> {
        };
    }

    public IncludingOrderedEntryIterator<K, V> setIteratorFactoryFunction(BiFunction<V, V, OrderedEntryIterator<K, V>> iteratorFactoryFunction) {
        this.iteratorFactoryFunction = iteratorFactoryFunction;
        return this;
    }

    public IncludingOrderedEntryIterator<K, V> setIncludePredicate(Predicate<OrderedEntry<K, V>> includePredicate) {
        this.includePredicate = includePredicate;
        return this;
    }

    public IncludingOrderedEntryIterator<K, V> setHandleDotsInKey(Consumer<OrderedEntry<K, V>> handleDotsInKey) {
        this.handleDotsInKey = handleDotsInKey;
        return this;
    }

    public OrderedEntryIterator<K, V> open() {
        iteratorStack.push(newOrderedEntryIterator(getSource()));
        super.open();
        return this;
    }

    @Override
    protected OrderedEntry<K, V> readEntry() {
        OrderedEntry<K, V> entry = iteratorStack.top().readEntry();
        while (entry == null) {
            iteratorStack.pop();
            if (iteratorStack.empty()) {
                return null;
            } else {
                entry = iteratorStack.top().readEntry();
            }
        }
        if (includePredicate.test(entry)) {
            iteratorStack.push(newOrderedEntryIterator(entry.getValue()));
            return readEntry();
        }
        handleDotsInKey.accept(entry);
        return entry;
    }

    private OrderedEntryIterator<K, V> newOrderedEntryIterator(V source) {
        if (iteratorStack.empty()) {
            return iteratorFactoryFunction.apply(null, source);
        }
        return iteratorFactoryFunction.apply(iteratorStack.top().getSource(), source);

    }

    class OrderedEntryIteratorStack<K, V> {
        private List<OrderedEntryIterator<K, V>> iterators;

        public OrderedEntryIteratorStack() {
            iterators = new ArrayList<>();
        }

        /**
         * Sustains no source duplicates in stack
         */
        public void push(OrderedEntryIterator<K, V> iterator) {
            Optional<V> any = iterators.stream()
                    .map(entryReader -> entryReader.getSource())
                    .filter(source -> source.equals(iterator.getSource()))
                    .findAny();
            if (any.isPresent()) {
                throw new IllegalArgumentException(iterator.getSource() + " is self referencing");
            }
            iterators.add(iterator);
        }

        public OrderedEntryIterator<K, V> pop() {
            int topIndex = iterators.size() - 1;
            OrderedEntryIterator iterator = iterators.get(topIndex);
            iterators.remove(topIndex);
            return iterator;
        }

        public OrderedEntryIterator<K, V> top() {
            return iterators.get(iterators.size() - 1);
        }

        public boolean empty() {
            return iterators.size() == 0;
        }

    }
}
