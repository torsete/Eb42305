package torsete.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

/**
 * Fetches input from a Reader.
 */
public class EntryReader<K, V> {
    /**
     * Current line number defined by the BufferedReader
     */
    private int nextLineNumber;
    private BufferedReader bufferedReader;
    /**
     * If true tab chars to the left of an entry key is replaced by dots
     */
    private boolean tabsInKeyEnabled;
    /**
     * Optional identification of source
     */
    private V source;

    EntryReader(Reader reader, V source) {
        bufferedReader = new BufferedReader(reader);
        this.source = source;
    }

    EntryReader(Reader reader) {
        this(reader, null);
    }

    V getSource() {
        return source;
    }

    void close() throws IOException {
        bufferedReader.close();
    }

    EntryReader<K, V> enableTabsInKey(boolean enabled) {
        tabsInKeyEnabled = enabled;
        return this;
    }

    /**
     * Fetches next entry
     */
    OrderedEntry<K, V> readEntry() throws IOException {
        Map.Entry<K, V> entry = null;
        do {
            String line = readLine();
            if (line == null) {
                return null;
            }
            if (tabsInKeyEnabled) {
                line = replaceTabsBeforeEntryKey(line);
            }
            entry = readEntry(line);
        } while (entry == null);
        return new OrderedEntry(entry).setLineNumber(nextLineNumber - 1).setSource(source);
    }

    private String replaceTabsBeforeEntryKey(String line) {
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

    /**
     * Parse a string which may contain en entry
     *
     * @return Null if the input line does not contain a full entry
     */
    private Map.Entry readEntry(String string) throws IOException {
        Properties p = new Properties();
        StringReader stringReader = new StringReader(string);
        p.load(stringReader);
        return p.size() == 0 ? null : p.entrySet().iterator().next();
    }

    /**
     * Attempts to collect an ntry
     *
     * @return Not null. May have the length 0.
     */
    private String readLine() throws IOException {
        String line;
        line = bufferedReader.readLine();
        nextLineNumber++;
        while (isLineToBeContinued(line)) {
            String additionalLine = bufferedReader.readLine();
            nextLineNumber++;
            if (additionalLine == null) {
                return line;
            }
            additionalLine = additionalLine.trim();
            line = line.substring(0, line.length() - 1).trim() + additionalLine;
        }
        return line;
    }

    private boolean isLineToBeContinued(String line) {
        if (line == null) {
            return false;
        }
        if (line.length() == 0 || line.charAt(line.length() - 1) != '\\') {
            return false;
        }
        return line.charAt(line.length() - 2) != '\\';
    }


}
