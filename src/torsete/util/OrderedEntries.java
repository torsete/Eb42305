package torsete.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class OrderedEntries {
    private List<Map.Entry<Object, Object>> entries;
    private List<Integer> linenumbers;
    private List<OrderedEntries> includingOrderedEntries;

    private Predicate<Map.Entry<Object, Object>> includePredicate;
    private BiConsumer<Map.Entry<Object, Object>, Integer> entryConsumer;
    private EntrySupplier entrySupplier;
    private boolean isTabsInKeyEnabled;
    private boolean isDotsInKeyEnabled;
    private String sourcename;

    private boolean isStreaming;

    public OrderedEntries() {
        clear();
    }

    public synchronized void clear() {
        entries = new ArrayList<>();
        linenumbers = new ArrayList<>();
        includingOrderedEntries = new ArrayList<>();
        enableTabsInKey(false);
        enableDotsInKey(false);

        includePredicate = entry -> entry.getKey().toString().toLowerCase().equals("include");
        entryConsumer = (e, l) -> {
        };
        entrySupplier = new EntrySupplier();

        isStreaming = false;
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

    /**
     * Required if using includes
     *
     * @param file
     * @throws IOException
     */
    public OrderedEntries load(File file) throws IOException {
        sourcename = file.getAbsolutePath();
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            load(fileInputStream);
        } finally {
            fileInputStream.close();
        }
        return this;
    }

    public synchronized void load(Reader reader) throws IOException {
        entrySupplier
                .setReader(reader)
                .enableDotsInKey(isDotsInKeyEnabled)
                .enableTabsInKey(isTabsInKeyEnabled);
        Map.Entry<Object, Object> entry;
        do {
            entry = processNextEntry();
        } while (entry != null);
    }


    public Stream<Map.Entry<Object, Object>> asStream(Reader reader) throws IOException {
        return asStream(reader, () -> {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Stream<Map.Entry<Object, Object>> asStream(Reader reader, Runnable onClose) {
        entrySupplier
                .setReader(reader)
                .enableDotsInKey(isDotsInKeyEnabled)
                .enableTabsInKey(isTabsInKeyEnabled);
        class EntrySpliterator extends Spliterators.AbstractSpliterator<Map.Entry<Object, Object>> {

            protected EntrySpliterator(long est, int additionalCharacteristics) {
                super(est, additionalCharacteristics);
            }

            @Override
            public boolean tryAdvance(Consumer<? super Map.Entry<Object, Object>> action) {
                Map.Entry<Object, Object> nextEentry = processNextEntry();
                if (nextEentry == null) {
                    onClose.run();
                    return false;
                }
                action.accept(nextEentry);
                return true;
            }
        }
        isStreaming = true;
        EntrySpliterator s0 = new EntrySpliterator(1, 2);
        Stream<Map.Entry<Object, Object>> stream = StreamSupport.stream(s0, false);
        return stream;
    }

    /**
     * Required if using includes
     *
     * @param file
     * @throws IOException
     */
    public synchronized Stream<Map.Entry<Object, Object>> asStream(File file) throws IOException {
        sourcename = file.getAbsolutePath();
        FileInputStream fileInputStream = new FileInputStream(file);
        return asStream(fileInputStream);
    }

    public synchronized Stream<Map.Entry<Object, Object>> asStream(InputStream inputStream) throws IOException {
        return asStream(new InputStreamReader(inputStream), () -> {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Map.Entry<Object, Object> processNextEntry() {
        Map.Entry<Object, Object> entry = entrySupplier.get();
        if (entry != null) {
            boolean isInclude = includePredicate.test(entry);
            if (isInclude) {
                if (sourcename == null) {
                    throw new IllegalArgumentException("Source is missing");
                }
                includeProperties(entry);
            } else {
                addProperty(entry, entrySupplier.getLineNumber());
            }
        }
        return entry;
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


    private synchronized void addProperty(Map.Entry<Object, Object> entry, Integer lineNumber) {
        entryConsumer.accept(entry, lineNumber);
        if (isStreaming) {
            return;
        }
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


}

