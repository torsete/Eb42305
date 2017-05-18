package torsete.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OrderedEntries {
    private List<Map.Entry<Object, Object>> entries;
    private List<Integer> linenumbers;

    private List<OrderedEntries> includingOrderedEntries;
    private Predicate<Map.Entry<Object, Object>> includePredicate;
    private BiConsumer<Map.Entry<Object, Object>, Integer> entryConsumer;
    private boolean isTabsInKeyEnabled;
    private boolean isDotsInKeyEnabled;
    private String sourcename;

    public OrderedEntries() {
        clear();
    }

    public synchronized void clear() {
        entries = new ArrayList<>();
        linenumbers = new ArrayList<>();
        includingOrderedEntries = new ArrayList<>();
        includePredicate = entry -> entry.getKey().toString().toLowerCase().equals("include");
        entryConsumer = (e, l) -> {
        };
        enableTabsInKey(false);
        enableDotsInKey(false);
    }

    public synchronized void load(String string) throws IOException {
        StringReader stringReader = new StringReader(string);
        load(stringReader);
        stringReader.close();
    }

    public synchronized void load(InputStream inputStream) throws IOException {
        load(new InputStreamReader(inputStream));
        inputStream.close();
    }

    public synchronized void load(Reader reader) throws IOException {
        EntrySupplier entrySupplier = new EntrySupplier(reader);
        Map.Entry<Object, Object> entry = entrySupplier.get();
        while (entry != null) {
            boolean isInclude = includePredicate.test(entry);
            if (isInclude) {
                if (sourcename == null) {
                    throw new IllegalArgumentException("Source is missing");
                }
                includeProperties(entry);
            } else {
                addProperty(entry, entrySupplier.getLineNumber());
            }
            entry = entrySupplier.get();
        }
    }

    /**
     * Required if using includes
     *
     * @param file
     * @throws IOException
     */
    public synchronized void load(File file) throws IOException {
        sourcename = file.getAbsolutePath();
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            load(fileInputStream);
        } finally {
            fileInputStream.close();
        }
    }

    /**
     * Predicate for keys identifying another property file to be included
     *
     * @param includePredicate
     */
    public synchronized OrderedEntries setIncludePredicate(Predicate<Map.Entry<Object, Object>> includePredicate) {
        this.includePredicate = includePredicate;
        return this;
    }

    public OrderedEntries setEntryConsumer(BiConsumer<Map.Entry<Object, Object>, Integer> entryConsumer) {
        this.entryConsumer = entryConsumer;
        return this;
    }


    public OrderedEntries enableTabsInKey(boolean enabled) {
        isTabsInKeyEnabled = enabled;
        return this;
    }

    public OrderedEntries enableDotsInKey(boolean enabled) {
        isDotsInKeyEnabled = enabled;
        return this;
    }

    /**
     * Slow!
     */
    public int getOrderedEntriesIndex(String key) {
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Object, Object> entry = entries.get(i);
            if (entry.getKey().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Slow!
     */
    public boolean isUnique(String key) {
        int index = getOrderedEntriesIndex(key);
        if (index >= 0) {
            for (int i = index + 1; i < entries.size(); i++) {
                Map.Entry<Object, Object> entry = entries.get(i);
                if (entry.getKey().equals(key)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Slow!
     */
    public Object readFirst(Object key) {
        List<Map.Entry<Object, Object>> ops = entries;
        Optional<Map.Entry<Object, Object>> entryOptional = ops.stream()
                .filter(e -> e.getKey().equals(key)).findFirst();
        return entryOptional.isPresent() ? entryOptional.get().getValue() : null;
    }


    public List<Map.Entry<Object, Object>> getEntries() {
        return entries;
    }

    public List<Integer> getLinenumbers() {
        return linenumbers;
    }

    public int size() {
        return entries.size();
    }

    public String getOrderedAsString() {
        return getOrderedAsString("=");
    }

    public String getOrderedAsString(String assignString) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry e : entries) {
            String key = e.getKey().toString();
            if (isTabsInKeyEnabled) {
                key = key.replace('.', '\t');
            }
            sb.append(new String((key + assignString + e.getValue() + "\n").getBytes(), StandardCharsets.ISO_8859_1));
        }
        return sb.toString();
    }

    public String getSourcename() {
        return sourcename;
    }

    /**
     * Locates parent with a specfific file name
     *
     * @param sourcename
     * @return Null id not found
     */
    private OrderedEntries getIncludingProperties(String sourcename) {
        for (OrderedEntries includingProperties : includingOrderedEntries) {
            if (sourcename.equals(includingProperties.getSourcename())) {
                return includingProperties;
            }
        }
        return null;
    }

    private void includeProperties(Map.Entry<Object, Object> entry) {
        OrderedEntries includedOrderedEntries = new OrderedEntries()
                .setEntryConsumer(entryConsumer)
                .setIncludePredicate(includePredicate)
                .enableDotsInKey(isDotsInKeyEnabled)
                .enableTabsInKey(isTabsInKeyEnabled);

        String includeSourcename = getAbsolutePath(entry.getValue().toString());
        if (getIncludingProperties(includeSourcename) != null) {
            throw new IllegalArgumentException("Circular reference from " + sourcename + " to " + includeSourcename);
        }
        try {
            includedOrderedEntries.load(new File(includeSourcename));
        } catch (IOException e) {
            throw new IllegalArgumentException(sourcename + ": " + e.getMessage(), e);
        }
        entries.addAll(includedOrderedEntries.entries);
        linenumbers.addAll(includedOrderedEntries.linenumbers);
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

    private synchronized void addProperty(Map.Entry<Object, Object> entry, Integer lineNumber) {
        entryConsumer.accept(entry, lineNumber);
//        Object put = propertiesxxxxxx.put(entry.getKey(), entry.getValue());
        this.linenumbers.add(lineNumber);
        this.entries.add(entry);
    }


    private String getAbsolutePath(String filename) {
        boolean useRelativeFilename = true;
        if (filename != null && filename.length() > 1) {
            if (filename.charAt(1) == ':') {
                useRelativeFilename = false;
            }
            if (filename.substring(0, 2).equals("\\\\")) {
                useRelativeFilename = false;
            }
        }
        if (useRelativeFilename) {
            filename = new File(this.sourcename).getParent() + File.separator + filename;
        }
        return new File(filename).getAbsolutePath();
    }


    class EntrySupplier implements Supplier<Map.Entry<Object, Object>> {
        private BufferedReader bufferedReader;
        private String[] splitPreviousKey;
        private int lineNumber;
        private int nextLineNumber;

        EntrySupplier(Reader reader) {
            bufferedReader = new BufferedReader(reader);
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
                if (isDotsInKeyEnabled) {
                    entry = replaceKey(entry);
                }
            }
            return entry;
        }

        String replaceTabs(String line) {
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


}

