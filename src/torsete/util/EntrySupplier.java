package torsete.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Torsten on 19.05.2017.
 */
class EntrySupplier implements Supplier<Map.Entry<Object, Object>> {
    private BufferedReader bufferedReader;
    private Reader reader;
    private String[] splitPreviousKey;

    private int nextInputLineNumber;
    private boolean isTabsInKeyEnabled;
    private boolean isDotsInKeyEnabled;

    private Map.Entry<Object, Object> currentEntry;
    private int currentLineNumber;
    private Map.Entry<Object, Object> nextEntry;
    private int nextLineNumber;
    private Predicate<EntrySupplier> partitionPredicate;


    EntrySupplier() {
        nextInputLineNumber = -1;
        partitionPredicate = (es) -> false;
    }

    public void setPartitionPredicate(Predicate<EntrySupplier> partitionPredicate) {
        this.partitionPredicate = partitionPredicate;
    }

    @Override
    public Map.Entry<Object, Object> get() {
        if (currentEntry == null) {
            nextEntry = readEntry();
            nextLineNumber = nextInputLineNumber;
        }
        currentEntry = nextEntry;
        currentLineNumber = nextLineNumber;
        if (nextEntry != null) {
            nextEntry = readEntry();
            nextLineNumber = nextInputLineNumber;
        }
        if (nextEntry == null) {
            nextLineNumber = -1;
        }
        return currentEntry;
    }


    public Integer getCurrentLineNumber() {
        return currentLineNumber;
    }

    public Integer getNextLineNumber() {
        return nextLineNumber;
    }

    public Map.Entry<Object, Object> getCurrentEntry() {
        return currentEntry;
    }

    public Map.Entry<Object, Object> getNextEntry() {
        return nextEntry;
    }


    public List<String> getCurrentKeyParts() {
        return splitKey(currentEntry);
    }

    public List<String> getNextKeyParts() {
        return splitKey(nextEntry);
    }


    public boolean isTabsInKeyEnabled() {
        return isTabsInKeyEnabled;
    }

    public boolean isDotsInKeyEnabled() {
        return isDotsInKeyEnabled;
    }

    public EntrySupplier setReader(Reader reader) {
        this.reader = reader;
        return this;
    }

    public EntrySupplier enableTabsInKey(boolean enabled) {
        isTabsInKeyEnabled = enabled;
        return this;
    }

    public EntrySupplier enableDotsInKey(boolean enabled) {
        isDotsInKeyEnabled = enabled;
        return this;
    }

    public boolean isLastEntryInPartition() {
        if (nextEntry == null) {
            return true;
        }
        return partitionPredicate.test(this);
    }

    public boolean isLastEntry() {
        return getNextEntry() == null;
    }

    private String replaceTabs(String line) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') {
                sb.append('.');
            } else {
                sb.append(line.substring(i));
                break;
            }
        }
        return sb.toString();
    }

    private Map.Entry<Object, Object> readEntry() {
        Map.Entry<Object, Object> entry = null;
        while (entry == null) {
            String line = readLine();
            if (line == null) {
                return null;
            }
            if (isTabsInKeyEnabled) {
                line = replaceTabs(line);
            }
            entry = readEntry(line);
            if (isDotsInKeyEnabled && entry != null) {
                entry = replaceKey(entry);
            }
        }
        return entry;
    }

    private Map.Entry<Object, Object> replaceKey(Map.Entry<Object, Object> entry) {
        String[] splitKey = splitKey(entry).stream().map(s -> s.trim()).collect(Collectors.toList()).toArray(new String[0]);
        if (splitPreviousKey != null) {
            for (int i = 0; i < splitKey.length; i++) {
                if (splitKey[i].length() == 0) {
                    if (i < splitPreviousKey.length) {
                        splitKey[i] = splitPreviousKey[i];
                    }
                }
            }
        }
        splitPreviousKey = splitKey;
        String newKey = Arrays.stream(splitKey).collect(Collectors.joining("."));
        return readEntry(newKey + "=" + entry.getValue());
    }

    private Map.Entry readEntry(String string) {
        Properties p = new Properties();
        StringReader stringReader = new StringReader(string);
        try {
            p.load(stringReader);
        } catch (IOException e) {
            return null; // Key is not present
        }
        return p.size() == 0 ? null : p.entrySet().iterator().next();

    }

    private String readLine() {
        String line;
        try {
            if (bufferedReader == null) {
                bufferedReader = new BufferedReader(reader);
            }
            line = bufferedReader.readLine();
            if (line == null) {
                bufferedReader.close();
                System.out.println("readLine() bufferedReader closed");
                return null;
            }
            nextInputLineNumber++;
            while (isLineToBeContinued(line)) {
                String additionalLine = bufferedReader.readLine();
                if (additionalLine == null) {
                    additionalLine = "";
                }
                nextInputLineNumber++;
                additionalLine = additionalLine.trim();
                line = line.substring(0, line.length() - 1).trim() + additionalLine;
            }
            return line;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isLineToBeContinued(String line) {
        if (line.length() == 0 || line.charAt(line.length() - 1) != '\\') {
            return false;
        }
        return line.charAt(line.length() - 2) != '\\';
    }

    private List<String> splitKey(Map.Entry<Object, Object> entry) {
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
