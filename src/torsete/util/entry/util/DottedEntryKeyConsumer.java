package torsete.util.entry.util;

import torsete.util.entry.OrderedEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Implements "dot notation" of keys
 *
 * Examples:
 *
 * aaa.bbb = ...
 * .ccc = ...
 * ..ddd
 * .eee
 *
 * The key ".ccc" wil be replaced by "aaa.ccc"
 * The key "..ddd" wil be replaced by "aaa.ccc.ddd"
 * The key ".eee" wil be replaced by "aaa.eee"
 *
 */
public class DottedEntryKeyConsumer<K, V> implements Consumer<OrderedEntry<K, V>> {
    private String[] previousSplitKey;

    private char[] splitChars;

    public DottedEntryKeyConsumer() {
        this('.');
    }

    public DottedEntryKeyConsumer(char... splitChars) {
        this.splitChars = splitChars;
    }

    /**
     * Changes thekey of the entry if nessecary
     *
     * @param orderedEntry
     */
    @Override
    public void accept(OrderedEntry<K, V> orderedEntry) {
        String[] splitKey = splitKey(orderedEntry).stream().map(s -> s.trim()).collect(Collectors.toList()).toArray(new String[0]);
        if (previousSplitKey == null) {
            previousSplitKey = splitKey;
            return;
        }
        if (splitKey != null) {
            for (int i = 0; i < splitKey.length; i++) {
                if (splitKey[i].length() == 0) {
                    if (i < previousSplitKey.length) {
                        splitKey[i] = previousSplitKey[i];
                    }
                }
            }
        }

        // Compute new Map.Entry<Object,Object>:
        K newKey = (K) Arrays.stream(splitKey).collect(Collectors.joining("."));
        orderedEntry.replaceKey(newKey);
        previousSplitKey = splitKey;

    }

    private List<String> splitKey(OrderedEntry<K, V> entry) {
        String key = entry.getKey().toString();
        List<String> strings = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (isSplitChar(c)) {
                strings.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }

        strings.add(sb.toString());
        return strings;
    }

    private boolean isSplitChar(char c) {
        for (char sc : splitChars) {
            if (c == sc) {
                return true;
            }

        }
        return false;
    }
}
