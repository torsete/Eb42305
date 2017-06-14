package torsete.util.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Torsten on 28.05.2017.
 */
class DottedEntryKey<K, V> implements Consumer<OrderedEntry<K, V>> {
    //    private OrderedEntries orderedEntries;
    private String[] previousSplitKey;

    public DottedEntryKey() {
        int c = 0;
//        this.orderedEntries = orderedEntries;
    }

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
        orderedEntry.setKey(newKey);
        previousSplitKey = splitKey;

    }

    private List<String> splitKey(OrderedEntry<K, V> entry) {
        String key = entry.getKey().toString();
        List<String> strings = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '.' || c == ' ') {
                strings.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }

        strings.add(sb.toString());
        return strings;
    }
}
