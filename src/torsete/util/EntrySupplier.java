package torsete.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Torsten on 19.05.2017.
 */
class EntrySupplier implements Supplier<Map.Entry<Object, Object>> {
    private BufferedReader bufferedReader;
    private String[] splitPreviousKey;
    private int lineNumber;
    private int nextLineNumber;
    private boolean isTabsInKeyEnabled;
    private boolean isDotsInKeyEnabled;

    EntrySupplier() {
        lineNumber = -1;
        nextLineNumber = -1;
    }

    @Override
    public Map.Entry<Object, Object> get() {
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


    public EntrySupplier setReader(Reader reader) {
        bufferedReader = new BufferedReader(reader);
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

    Map.Entry<Object, Object> replaceKey(Map.Entry<Object, Object> entry) {
        String key = entry.getKey().toString();
        String value = entry.getValue().toString();
        String[] splitKey = split(key).stream().map(s -> s.trim()).collect(Collectors.toList()).toArray(new String[0]);
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

    public Integer getLineNumber() {
        return lineNumber;
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
        lineNumber = nextLineNumber + 1;
        try {
            line = bufferedReader.readLine();
            if (line == null) {
                bufferedReader.close();
                return null;
            }
            nextLineNumber++;
            while (isLineToBeContinued(line)) {
                String additionalLine = bufferedReader.readLine();
                if (additionalLine == null) {
                    additionalLine = "";
                }
                nextLineNumber++;
                additionalLine = additionalLine.trim();
                line = line.substring(0, line.length() - 1).trim() + additionalLine;
            }
            return line;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    boolean isLineToBeContinued(String line) {
        if (line.length() == 0 || line.charAt(line.length() - 1) != '\\') {
            return false;
        }
        if (line.length() == 1) {
            return true;
        }
        return line.charAt(line.length() - 2) != '\\';
    }

    /**
     * String.split-method seems not to act as desired ("\\." or "\\\\."?)
     *
     * @param s
     * @return
     */
    private List<String> split(String s) {
        List<String> strings = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
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
