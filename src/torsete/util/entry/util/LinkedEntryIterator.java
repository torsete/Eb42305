package torsete.util.entry.util;

import torsete.util.entry.LinkedEntry;

/**
 * Fetches input from a orderedEntry list of {@link LinkedEntry <K, V>}.
 */
public class LinkedEntryIterator<K, V> extends EntryIterator<K, V> {
    private LinkedEntry<K, V> currentEntry;

    public LinkedEntryIterator(LinkedEntry<K, V> firstEntry) {
        currentEntry = firstEntry;
    }

    @Override
    protected LinkedEntry<K, V> readEntry() {
        if (currentEntry == null) {
            return null;
        }
        LinkedEntry<K, V> entry = currentEntry;
        currentEntry = entry.getSuccessor();
        return entry;
    }

}
