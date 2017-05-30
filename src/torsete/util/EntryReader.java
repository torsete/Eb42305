package torsete.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Torsten on 28.05.2017.
 */
public class EntryReader<K, V> {
    private int nextLineNumber;

    private BufferedReader bufferedReader;
    private boolean tabsInKeyEnabled;
    private V source;

    EntryReader(Reader reader, V source) {
        bufferedReader = new BufferedReader(reader);
        this.source = source;
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

    OrderedEntry<K, V> readEntry() throws IOException {
        Map.Entry<K, V> entry = null;
        do {
            String line = readLine();
            if (line == null) {
                return null;
            }
            if (tabsInKeyEnabled) {
                line = replaceTabs(line);
            }
            entry = readEntry(line);
        } while (entry == null);
        return new OrderedEntry(entry, nextLineNumber - 1);
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


    private Map.Entry readEntry(String string) throws IOException {
        Properties p = new Properties();
        StringReader stringReader = new StringReader(string);
        p.load(stringReader);
        return p.size() == 0 ? null : p.entrySet().iterator().next();
    }

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
