package torsete.util.entry.util;

import torsete.util.entry.LinkedEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Fetches input from a Reader.
 */
public class ReaderEntryIterator<K, V> extends EntryIterator<K, V> {

    /**
     * Entry source
     */
    private BufferedReader bufferedReader;
    /**
     * Current line number defined by the BufferedReader
     */
    private int nextLineNumber;

    public ReaderEntryIterator<K, V> setReader(Reader reader) {
        this.bufferedReader = new BufferedReader(reader);
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

    @Override
    protected LinkedEntry<K, V> readEntry() {
        String line = readLine();
        while (line != null) {
            String collectedLine = "";
            while (line != null) {
                nextLineNumber++;
                line = trimLeft(line);
                if (line.endsWith("\\") && !line.endsWith("\\\\")) {
                    collectedLine += line.substring(0, line.length() - 1);
                    line = readLine();
                } else {
                    collectedLine += line;
                    line = null;
                }
            }
            Map.Entry<K, V> entry = fetchEntry(collectedLine);
            if (entry != null) {
                return new LinkedEntry(entry).setLineNumber(nextLineNumber - 1).setSource(getSource());
            }
            line = readLine();
        }
        return null;
    }


    private String trimLeft(String string) {
        int pos = string.length();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c != ' ') {
                pos = i;
                break;
            }
        }
        return string.substring(pos);
    }

    private String readLine() {
        try {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ectracts an entry from a string which may contain en entry
     *
     * @param keyPrefix If not null this value will be appended as key prefix
     * @return Null if the input line does not contain a full entry
     */
    private Map.Entry<K, V> fetchEntry(String string) {
        Properties properties = new Properties();  // To be used as parser
        StringReader stringReader = new StringReader(string);
        try {
            properties.load(stringReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (properties.size() == 0) {
            return null;
        }
        Map.Entry<Object, Object> entry = properties.entrySet().iterator().next();
        K key = (K) entry.getKey();
        V value = (V) entry.getValue();

        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map.entrySet().iterator().next();
    }

}
