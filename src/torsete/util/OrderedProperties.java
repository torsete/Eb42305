package torsete.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OrderedProperties {
    private List<Map.Entry<Object, Object>> orderedProperties;
    private List<Map.Entry<Object, Object>> deepOrderedProperties;
    private List<Integer> orderedLineNumbers;
    private List<Integer> deepOrderedLineNumbers;

    private List<OrderedProperties> includingOrderedProperties;
    private List<OrderedProperties> includedOrderedProperties;
    private Predicate<Map.Entry<Object, Object>> includeKeyPredicate;
    private boolean isTabsInKeyEnabled;
    private boolean isDotsInKeyEnabled;
    private String sourcename;

    private Properties properties;

    public OrderedProperties() {
        super();
        clear();
    }

    public synchronized void clear() {
        properties = new Properties();
        properties.clear();
        orderedProperties = new ArrayList<>();
        deepOrderedProperties = new ArrayList<>();
        orderedLineNumbers = new ArrayList<>();
        deepOrderedLineNumbers = new ArrayList<>();
        includingOrderedProperties = new ArrayList<>();
        includedOrderedProperties = new ArrayList<>();
        includeKeyPredicate = entry -> entry.getKey().toString().toLowerCase().equals("include");
        enableTabsInKey(false);
        enableDotsInKey(false);
    }


    public synchronized void load(InputStream inputStream) throws IOException {
        load(new InputStreamReader(inputStream));
        inputStream.close();
    }

    public synchronized void load(Reader reader) throws IOException {
        EntrySupplier entrySupplier = new EntrySupplier(reader);
        Map.Entry<Object, Object> entry = entrySupplier.get();
        while (entry != null) {
            boolean isInclude = includeKeyPredicate.test(entry);
            if (isInclude) {
                if (sourcename == null) {
                    throw new IllegalArgumentException("Source is missing");
                }
                includeProperties(entry);

            } else {
                setProperty(entry, entrySupplier.getLineNumber());
            }
            entry = entrySupplier.get();
        }
    }

    private void includeProperties(Map.Entry<Object, Object> entry) {
        OrderedProperties includedOrderedProperties = new OrderedProperties().setIncludeKeyPredicate(includeKeyPredicate);

        this.includedOrderedProperties.add(includedOrderedProperties);
        includedOrderedProperties.addIncludingPropterties(this);

        String includeSourcename = getAbsolutePath(entry.getValue().toString());
        OrderedProperties sameSourceParent = getIncludingProperties(includeSourcename);
        if (sameSourceParent != null) {
            throw new IllegalArgumentException("Circular reference from " + sourcename + " to " + includeSourcename);
        }
        try {
            includedOrderedProperties.load(new File(includeSourcename));
        } catch (IOException e) {
            throw new IllegalArgumentException(sourcename + ": " + e.getMessage(), e);
        }
        includedOrderedProperties.deepOrderedProperties.forEach(e -> deepOrderedProperties.add(e));


    }

    /**
     * Predicate for keys identifying another property file to be included
     *
     * @param includeKeyPredicate
     */
    public synchronized OrderedProperties setIncludeKeyPredicate(Predicate<Map.Entry<Object, Object>> includeKeyPredicate) {
        this.includeKeyPredicate = includeKeyPredicate;
        return this;
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
     * Locates parent with a specfific file name
     *
     * @param sourcename
     * @return Null id not fournd
     */
    public OrderedProperties getIncludingProperties(String sourcename) {
        for (OrderedProperties includingProperties : includingOrderedProperties) {
            if (sourcename.equals(includingProperties.getSourcename())) {
                return includingProperties;
            }
        }
        return null;
    }

    /**
     * Get number of first matching entry
     *
     * @param key
     * @return -1 if not found
     */
    public int getOrderedPropertyIndex(String key) {
        return getOrderedPropertyIndex(key, true);
    }

    public int getOrderedPropertyIndex(String key, boolean deep) {
        List<Map.Entry<Object, Object>> ops = deep ? deepOrderedProperties : orderedProperties;
        for (int i = 0; i < ops.size(); i++) {
            Map.Entry<Object, Object> entry = ops.get(i);
            if (entry.getKey().equals(key)) {
                return i;
            }
        }
        return -1;
    }


    public boolean isUnique(String key, boolean deep) {
        List<Map.Entry<Object, Object>> ops = deep ? deepOrderedProperties : orderedProperties;
        int index = getOrderedPropertyIndex(key);
        if (index < 0 || index == ops.size() - 1) {
            return true;
        }
        return !ops.get(index).getKey().equals(ops.get(index + 1).getKey());
    }

    public boolean isUnique(String key) {
        return isUnique(key, true);
    }

    public List<Map.Entry<Object, Object>> getOrdered(boolean deep) {
        return deep ? deepOrderedProperties : orderedProperties;
    }

    public List<Map.Entry<Object, Object>> getOrdered() {
        return getOrdered(true);
    }


    public List<Integer> getOrderedLineNumbers(boolean deep) {
        return deep ? deepOrderedLineNumbers : orderedLineNumbers;
    }

    public List<Integer> getOrderedLineNumbers() {
        return getOrderedLineNumbers(true);
    }

    public int size(boolean deep) {
        return deep ? deepOrderedProperties.size() : orderedProperties.size();
    }

    public int size() {
        return size(true);
    }

    /**
     * Slow!
     *
     * @param key
     * @param deep
     * @return
     */
    public Object get(Object key, boolean deep) {
        List<Map.Entry<Object, Object>> ops = deep ? deepOrderedProperties : orderedProperties;
        Optional<Map.Entry<Object, Object>> entryOptional = ops.stream()
                .filter(e -> e.getKey().equals(key)).findFirst();
        return entryOptional.isPresent() ? entryOptional.get().getValue() : null;
    }

    /**
     * Slow!
     *
     * @param key
     * @return
     */
    public Object get(Object key) {
        return get(key, true);
    }

    public OrderedProperties enableTabsInKey(boolean enabled) {
        isTabsInKeyEnabled = enabled;
        return this;
    }

    public OrderedProperties enableDotsInKey(boolean enabled) {
        isDotsInKeyEnabled = enabled;
        return this;
    }

    public String getSourcename() {
        return sourcename;
    }

    public List<OrderedProperties> getIncludingOrderedProperties() {
        return includingOrderedProperties;
    }

    public List<OrderedProperties> getIncludedOrderedProperties() {
        return includedOrderedProperties;
    }


    public Properties getProperties() {
        return properties;
    }

    private synchronized Object setProperty(Map.Entry<Object, Object> entry, Integer lineNumber) {
        Object put = properties.put(entry.getKey(), entry.getValue());
        this.orderedLineNumbers.add(lineNumber);
        this.deepOrderedLineNumbers.add(lineNumber);
        this.orderedProperties.add(entry);
        this.deepOrderedProperties.add(entry);
        return put;
    }

    private Map.Entry readProperty(String string) {
        Properties p = new Properties();
        StringReader stringReader = new StringReader(string);
        try {
            p.load(stringReader);
        } catch (IOException e) {
            return null; // Key is not present
        }
        return p.size() == 0 ? null : p.entrySet().iterator().next();

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


    private void addIncludingPropterties(OrderedProperties includingProperties) {
        this.includingOrderedProperties.add(includingProperties);
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
                entry = readProperty(line);
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
            return readProperty(newKey + "=" + entry.getValue());

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

    public String getOrderdAsString(boolean deep, String assignString) {
        if (assignString == null) {
            if (isTabsInKeyEnabled || isDotsInKeyEnabled) {
                assignString = " ";
            } else {
                assignString = "=";
            }
        }
        StringBuilder sb = new StringBuilder();
        List<Map.Entry<Object, Object>> op = deep ? deepOrderedProperties : orderedProperties;
        for (Map.Entry e : op) {
            String key = e.getKey().toString();
            if (isTabsInKeyEnabled) {
                key = key.replace('.', '\t');
            }
            sb.append(new String((key + assignString + e.getValue() + "\n").getBytes(), StandardCharsets.ISO_8859_1));
        }
        return sb.toString();
    }

    public String getOrderdAsString(String assignString) {
        return getOrderdAsString(true, assignString);
    }

    public String getOrderdAsString() {
        return getOrderdAsString(true, null);
    }

}

