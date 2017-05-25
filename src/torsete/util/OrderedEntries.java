package torsete.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class OrderedEntries {
    protected List<Map.Entry<Object, Object>> entries;
    protected List<Integer> linenumbers;
    protected List<String> sourcenameStack;

    protected Predicate<Map.Entry<Object, Object>> includePredicate;
    protected BiConsumer<Map.Entry<Object, Object>, Map.Entry<Object, Object>> entryConsumer;
    protected BiFunction<Map.Entry<Object, Object>, Map.Entry<Object, Object>, Boolean> endOfStreamFunction;
    protected EntrySupplier entrySupplier;
    protected String sourcename;
    private boolean keepEntries;
    private int nextEntryIndex;


    public OrderedEntries() {
        clear();
    }

    public synchronized OrderedEntries clear() {
        entries = new ArrayList<>();
        linenumbers = new ArrayList<>();
        sourcenameStack = new ArrayList<>();
        includePredicate = entry -> entry.getKey().toString().toLowerCase().equals("include");
        entryConsumer = (e1, e2) -> {
        };
        endOfStreamFunction = (e1, e2) -> e2 == null;
        entrySupplier = new EntrySupplier();
        enableTabsInKey(false);
        enableDotsInKey(false);
        keepEntries = true;
        nextEntryIndex = 0;


        setEntryConsumer((e1, e2) -> {
            es = new ArrayList<>();
            es.add(e1);
            es.add(e2);
        });
        return this;
    }


    List<Map.Entry<Object, Object>> es = new ArrayList<>();

    public synchronized OrderedEntries load(String string) throws IOException {
        StringReader stringReader = new StringReader(string);
        load(stringReader);
        stringReader.close();
//        System.out.println("load() StringReader closed");
        return this;
    }

    public synchronized OrderedEntries load(InputStream inputStream) throws IOException {
        load(new InputStreamReader(inputStream));
        inputStream.close();
//        System.out.println("load() InputStream closed");
        return this;
    }

    /**
     * Required if using includes
     *
     * @param file
     * @throws IOException
     */
    public OrderedEntries load(File file) throws IOException {
        sourcename = file.getAbsolutePath();
        sourcenameStack.add(sourcename); // Push
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            load(fileInputStream);
        } finally {
            fileInputStream.close();
//            System.out.println("load() FileInputStream closed");
        }
        sourcenameStack.remove(sourcename); //Pop
        return this;
    }


    public synchronized OrderedEntries load(Reader reader) throws IOException {
        entrySupplier.setReader(reader);
        Map.Entry<Object, Object> entry;
        do {
            entry = readNextEntry();
        } while (entry != null);
        return this;
    }

    public synchronized Stream<Map.Entry<Object, Object>> stream(String string) throws IOException {
        StringReader stringReader = new StringReader(string);
        return stream(stringReader);
    }

    public synchronized Stream<Stream<Map.Entry<Object, Object>>> streams(String string) throws IOException {
        StringReader reader = new StringReader(string);
        getEntrySupplier().setReader(reader);
        return readingStreams(() -> {
            reader.close();
//            System.out.println("streams() Reader closed");
        });
    }

    public synchronized Stream<Stream<Map.Entry<Object, Object>>> streams(Reader reader) throws IOException {
        getEntrySupplier().setReader(reader);
        return readingStreams(() -> {
            try {
                reader.close();
//                System.out.println("streams() Reader closed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
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
        return readingStream(() -> {
            try {
                inputStream.close();
//                System.out.println("stream() InputStream closed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public synchronized Stream<Stream<Map.Entry<Object, Object>>> streams(InputStream inputStream) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream);
        getEntrySupplier().setReader(reader);
        return readingStreams(() -> {
            try {
                inputStream.close();
//                System.out.println("streams() InputStream closed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Stream<Map.Entry<Object, Object>> stream(Reader reader) throws IOException {
        getEntrySupplier().setReader(reader);
        return readingStream(() -> {
            try {
                reader.close();
//                System.out.println("stream() Reader closed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
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


    public OrderedEntries setEntryConsumer(BiConsumer<Map.Entry<Object, Object>, Map.Entry<Object, Object>> entryConsumer) {
        this.entryConsumer = entryConsumer;
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

    public OrderedEntries enableKeepEntries(boolean enabled) {
        keepEntries = enabled;
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
        return entryOptional.get().getValue();
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

    protected Map.Entry<Object, Object> readNextEntry() {
        Map.Entry<Object, Object> entry = entrySupplier.get();
        if (entry != null) {
            boolean isInclude = includePredicate.test(entry);
            if (isInclude) {
                includeProperties(entry);
            } else {
                addProperty(entry, entrySupplier.getCurrentLineNumber());
            }
        }
        return entry;
    }

    private void includeProperties(Map.Entry<Object, Object> entry) {
        OrderedEntries includedOrderedEntries = new OrderedEntries()
                .setEntryConsumer(entryConsumer)
                .setIncludePredicate(includePredicate)
                .setEndOfStreamFunction(endOfStreamFunction)
                .enableKeepEntries(keepEntries)
                .enableDotsInKey(isDotsInKeyEnabled())
                .enableTabsInKey(isTabsInKeyEnabled())
                .setSourcenameStack(sourcenameStack);

        String includeSourcename = getAbsolutePath(entry.getValue().toString());
        if (sourcenameStack.contains(includeSourcename)) {
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


    protected OrderedEntries setSourcenameStack(List<String> sourcenameStack) {
        this.sourcenameStack = sourcenameStack;
        return this;
    }

    protected synchronized void addProperty(Map.Entry<Object, Object> entry, Integer lineNumber) {
        if (keepEntries) {
            this.linenumbers.add(lineNumber);
            this.entries.add(entry);
        }
        entryConsumer.accept(entry, entrySupplier.getNextEntry());
    }


    private String getAbsolutePath(String filename) {
        filename = new File(this.sourcename).getParent() + File.separator + filename;
        return new File(filename).getAbsolutePath();
    }


    public OrderedEntries setEndOfStreamFunction(BiFunction<Map.Entry<Object, Object>, Map.Entry<Object, Object>, Boolean> endOfStreamFunction) {
        this.endOfStreamFunction = endOfStreamFunction;
        return this;
    }


    Stream<Map.Entry<Object, Object>> readingStream() {
        return readingStream(() -> {
        });
    }

    Stream<Map.Entry<Object, Object>> readingStream(Runnable onClose) {
//        return new BaseSpliterator<Map.Entry<Object, Object>>()
//                .setNextSupplier(() -> {
//                    readNextEntry();
//                    return es.get(0);
//                })
//                .setIsLastSupplier(() ->
//                        (es.get(0) == null || endOfStreamFunction.apply(es.get(0), es.get(1))))
//                .setOnClose(onClose)
//                .stream();
//    }
//
//    Stream<Stream<Map.Entry<Object, Object>>> readingStreams(Runnable onClose) {
//        return new BaseSpliterator<Stream<Map.Entry<Object, Object>>>()
//                .setNextSupplier(() -> readingStream())
//                .setIsLastSupplier(() -> es.get(0) == null)
//                .setOnClose(onClose)
//                .stream();
//    }
        return new BaseSpliterator<Map.Entry<Object, Object>>()
                .setNextSupplier(() -> readNextEntry())
                .setIsLastSupplier(() ->
                        (entrySupplier.getNextEntry() == null || endOfStreamFunction.apply(entrySupplier.getCurrentEntry(), entrySupplier.getNextEntry())))
                .setOnClose(onClose)
                .stream();
    }

    Stream<Stream<Map.Entry<Object, Object>>> readingStreams(Runnable onClose) {
        return new BaseSpliterator<Stream<Map.Entry<Object, Object>>>()
                .setNextSupplier(() -> readingStream())
                .setIsLastSupplier(() -> entrySupplier.getNextEntry() == null)
                .setOnClose(onClose)
                .stream();
    }

    Stream<Map.Entry<Object, Object>> stream() {
        return new BaseSpliterator<Map.Entry<Object, Object>>()
                .setNextSupplier(() -> entries.get(nextEntryIndex++))
                .setIsLastSupplier(() -> nextEntryIndex >= entries.size() || endOfStreamFunction.apply(entries.get(nextEntryIndex - 1), entries.get(nextEntryIndex)))
                .stream();
    }


    public synchronized Stream<Stream<Map.Entry<Object, Object>>> streams() {
        return new BaseSpliterator<Stream<Map.Entry<Object, Object>>>()
                .setNextSupplier(() -> stream())
                .setIsLastSupplier(() -> nextEntryIndex >= entries.size())
                .stream();
    }


}

