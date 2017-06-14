package torsete.util.entry;

/**
 * Fetches input from a linked list of {@link OrderedEntry<K, V>}.
 */
public class LinkedOrderedEntryIterator<K, V> extends OrderedEntryIterator<K, V> {
    private OrderedEntry<K, V> currentEntry;

    public LinkedOrderedEntryIterator(OrderedEntry<K, V> firstEntry) {
        currentEntry = firstEntry;
    }

    @Override
    protected OrderedEntry<K, V> readEntry() {
        if (currentEntry == null) {
            return null;
        }
        OrderedEntry<K, V> entry = currentEntry;
        currentEntry = entry.getSuccessor();
        return entry;
    }

}
