package torsete.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class OrderedEntries {
    protected List<Map.Entry<Object, Object>> entries;
    protected List<Integer> linenumbers;
    protected List<String> sourcenameStack;

    protected Predicate<Map.Entry<Object, Object>> includePredicate;
    protected BiConsumer<Map.Entry<Object, Object>, Integer> entryConsumer;
    protected BiConsumer<Map.Entry<Object, Object>, Map.Entry<Object, Object>> entryLookaheadConsumer;
    protected EntrySupplier entrySupplier;
    protected String sourcename;

    private boolean keepEntries;

    public OrderedEntries() {
        clear();
    }

    public synchronized void clear() {
        entries = new ArrayList<>();
        linenumbers = new ArrayList<>();
        sourcenameStack = new ArrayList<>();

        includePredicate = entry -> entry.getKey().toString().toLowerCase().equals("include");
        entryConsumer = (e, l) -> {
        };
        entryLookaheadConsumer = (e1, e2) -> {
        };
        entrySupplier = new EntrySupplier();

        enableTabsInKey(false);
        enableDotsInKey(false);
        keepEntries = true;
    }

    public synchronized void load(String string) throws IOException {
        StringReader stringReader = new StringReader(string);
        load(stringReader);
        stringReader.close();
        System.out.println("load() StringReader closed");
    }

    public synchronized void load(InputStream inputStream) throws IOException {
        load(new InputStreamReader(inputStream));
        inputStream.close();
        System.out.println("load() InputStream closed");
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
            System.out.println("streams() FileInputStream closed");
        }
        return this;
    }


    public synchronized void load(Reader reader) throws IOException {
        entrySupplier.setReader(reader);
        Map.Entry<Object, Object> entry;
        do {
            entry = processEntry();
        } while (entry != null);
    }

    public synchronized Stream<Map.Entry<Object, Object>> stream(File file) throws IOException {
        setSourcename(file.getAbsolutePath());
        FileInputStream fileInputStream = new FileInputStream(file);
        return stream(fileInputStream);
    }

    public synchronized Stream<Stream<Map.Entry<Object, Object>>> streams(File file) throws IOException {
        setSourcename(file.getAbsolutePath());
        FileInputStream fileInputStream = new FileInputStream(file);
        return streams(fileInputStream);
    }


    public synchronized Stream<Map.Entry<Object, Object>> stream(InputStream inputStream) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream);
        getEntrySupplier().setReader(reader);
        return stream(() -> {
            try {
                inputStream.close();
                System.out.println("stream() InputStream closed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public synchronized Stream<Stream<Map.Entry<Object, Object>>> streams(InputStream inputStream) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream);
        getEntrySupplier().setReader(reader);
        return streams(() -> {
            try {
                inputStream.close();
                System.out.println("streams() InputStream closed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Stream<Map.Entry<Object, Object>> stream(Reader reader) throws IOException {
        getEntrySupplier().setReader(reader);
        return stream(() -> {
            try {
                reader.close();
                System.out.println("stream() Reader closed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public synchronized Stream<Stream<Map.Entry<Object, Object>>> streams() {
        return null;
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

    public OrderedEntries setEntryLookaheadConsumer(BiConsumer<Map.Entry<Object, Object>, Map.Entry<Object, Object>> entryLookaheadConsumer) {
        this.entryLookaheadConsumer = entryLookaheadConsumer;
        return this;
    }


    public OrderedEntries enableTabsInKey(boolean enabled) {
        entrySupplier.enableTabsInKey(enabled);
        return this;
    }

    public OrderedEntries enableDotsInKey(boolean enabled) {
        entrySupplier.enableDotsInKey(enabled);
        return this;
    }

    public EntrySupplier getEntrySupplier() {
        return entrySupplier;
    }

    public boolean isTabsInKeyEnabled() {
        return entrySupplier.isTabsInKeyEnabled();
    }

    public boolean isDotsInKeyEnabled() {
        return entrySupplier.isDotsInKeyEnabled();
    }


    public void setSourcename(String sourcename) {
        this.sourcename = sourcename;
    }

    public void enableKeepEntries(boolean enabled) {
        keepEntries = enabled;
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
            sb.append(new String((key + assignString + e.getValue() + "\n").getBytes(), StandardCharsets.ISO_8859_1));
        }
        return sb.toString();
    }

    public String getSourcename() {
        return sourcename;
    }

    protected Map.Entry<Object, Object> processEntry() {
        Map.Entry<Object, Object> entry = entrySupplier.get();
        if (entry != null) {
            boolean isInclude = includePredicate.test(entry);
            if (isInclude) {
                if (sourcename == null) {
                    throw new IllegalArgumentException("Source is missing");
                }
                includeProperties(entry);
            } else {
                addProperty(entry, entrySupplier.getCurrentLineNumber());
            }
        }
        return entry;
    }


    public void setPartitionPredicate(Predicate<EntrySupplier> partitionPredicate) {
        entrySupplier.setPartitionPredicate(partitionPredicate);
    }

    private void includeProperties(Map.Entry<Object, Object> entry) {
        OrderedEntries includedOrderedEntries = new OrderedEntries()
                .setEntryConsumer(entryConsumer)
                .setEntryLookaheadConsumer(entryLookaheadConsumer)
                .setIncludePredicate(includePredicate)
                .enableDotsInKey(isDotsInKeyEnabled())
                .enableTabsInKey(isTabsInKeyEnabled())
                .setSourcenameStack(sourcenameStack);

        String includeSourcename = getAbsolutePath(entry.getValue().toString());
        if (sourcenameStack.contains(includeSourcename)) {
            throw new IllegalArgumentException("Circular reference from " + sourcename + " to " + includeSourcename);
        }
        sourcenameStack.add(includeSourcename);
        try {
            includedOrderedEntries.load(new File(includeSourcename));
        } catch (IOException e) {
            throw new IllegalArgumentException(sourcename + ": " + e.getMessage(), e);
        }
        entries.addAll(includedOrderedEntries.entries);
        linenumbers.addAll(includedOrderedEntries.linenumbers);
        sourcenameStack.remove(includeSourcename);
    }


    private OrderedEntries setSourcenameStack(List<String> sourcenameStack) {
        this.sourcenameStack = sourcenameStack;
        return this;
    }

    protected synchronized void addProperty(Map.Entry<Object, Object> entry, Integer lineNumber) {
        if (keepEntries) {
            this.linenumbers.add(lineNumber);
            this.entries.add(entry);
        }
        entryConsumer.accept(entry, lineNumber);
        entryLookaheadConsumer.accept(entry, entrySupplier.getNextEntry());
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


    Stream<Map.Entry<Object, Object>> stream() {
        return new BaseSpliterator<Map.Entry<Object, Object>>()
                .setNextSupplier(() -> processEntry())
                .setIsLastSupplier(() -> isLastEntryInPartition())
                .stream();
    }

    Stream<Map.Entry<Object, Object>> stream(Runnable onClose) {
        return new BaseSpliterator<Map.Entry<Object, Object>>()
                .setNextSupplier(() -> processEntry())
                .setIsLastSupplier(() -> isLastEntryInPartition())
                .setOnClose(onClose)
                .stream();
    }

    Stream<Stream<Map.Entry<Object, Object>>> streams(Runnable onClose) {
        return new BaseSpliterator<Stream<Map.Entry<Object, Object>>>()
                .setNextSupplier(() -> stream())
                .setIsLastSupplier(() -> isLastEntry())
                .setOnClose(onClose)
                .stream();
    }

    boolean isLastEntry() {
        return entrySupplier.isLastEntry();
    }

    boolean isLastEntryInPartition() {
        return entrySupplier.isLastEntryInPartition();
    }
}

