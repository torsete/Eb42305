package torsete.util.entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

/**
 * Fetches input from a Reader.
 */
public class ReaderOrderedEntryIterator<K, V> extends OrderedEntryIterator<K, V> {

    /**
     * Entry source
     */
    private BufferedReader bufferedReader;
    /**
     * Current line number defined by the BufferedReader
     */
    private int nextLineNumber;
    /**
     * If true tab chars to the left of an entry key is replaced by dots
     */
    protected boolean tabsInKeyEnabled;

    public ReaderOrderedEntryIterator<K, V> setReader(Reader reader) {
        this.bufferedReader = new BufferedReader(reader);
        return this;
    }


    public ReaderOrderedEntryIterator<K, V> enableTabsInKey(boolean enabled) {
        tabsInKeyEnabled = enabled;
        return this;
    }



    @Override
    public void close() {
        try {
            bufferedReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetches next entry
     */
    protected OrderedEntry<K, V> readEntry() {
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
        OrderedEntry orderedEntry = new OrderedEntry(entry).setLineNumber(nextLineNumber - 1).setSource(getSource());
        return orderedEntry;
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
    private Map.Entry readEntry(String string) {
        Properties p = new Properties();
        StringReader stringReader = new StringReader(string);
        try {
            p.load(stringReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return p.size() == 0 ? null : p.entrySet().iterator().next();
    }

    /**
     * Attempts to collect an ntry
     *
     * @return Not null. May have the length 0.
     */
    private String readLine() {
        String line = null;

        try {
            line = bufferedReader.readLine();
            nextLineNumber++;
            while (isLineToBeContinued(line)) {
                String additionalLine = bufferedReader.readLine();
                nextLineNumber++;
                if (additionalLine == null) {
                    return line;
                }
                additionalLine = additionalLine.trim();
                line = line.substring(0, line.length() - 1) + additionalLine;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
