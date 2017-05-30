package torsete.util;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class OrderedEntries<K, V> {
    private EntryInput entryInput;
    private EntryReaderStack entryReaderStack;
    private Predicate<OrderedEntry<K, V>> includePredicate;
    private Consumer<OrderedEntry<K, V>> entryConsumer;
    private Function<OrderedEntry<K, V>, Boolean> endOfStreamFunction;

    private Consumer<OrderedEntry<K, V>> handleDotsInKey;
    private boolean isTabsInKeyEnabled;


    public OrderedEntries() {
        clear();
    }


    public synchronized OrderedEntries clear() {
        entryInput = new EntryInput(false);
        entryReaderStack = new EntryReaderStack();
        enableDotsInKey(true);
        enableTabsInKey(true);
        includePredicate = entry -> entry.getKey().toString().toLowerCase().contains("include");
        entryConsumer = oe -> {
        };
        endOfStreamFunction = oe -> false;
        return this;
    }

    public OrderedEntries enableDotsInKey(boolean enabled) {
        handleDotsInKey = enabled ? new DottedEntryKey() : e -> {
        };
        return this;
    }

    public OrderedEntries enableTabsInKey(boolean enabled) {
        isTabsInKeyEnabled = enabled;
        return this;
    }


    public String getOrderedAsString() {
        StringBuilder sb = new StringBuilder();
        OrderedEntry<K, V> orderedEntry = entryInput.getFirstOrderedEntry();
        while (orderedEntry != null) {
            sb.append(orderedEntry.toString() + "\n");
            orderedEntry = orderedEntry.getSuccessor();
        }

        return sb.toString();
    }

    public OrderedEntries setEndOfStreamFunction(Function<OrderedEntry<K, V>, Boolean> endOfStreamFunction) {
        this.endOfStreamFunction = endOfStreamFunction;
        return this;
    }


    /**
     * Predicate for keys identifying another property file to be included
     *
     * @param includePredicate
     */
    public synchronized OrderedEntries setIncludePredicate(Predicate<OrderedEntry<K, V>> includePredicate) {
        this.includePredicate = includePredicate;
        return this;
    }


    public OrderedEntries setEntryConsumer(Consumer<OrderedEntry<K, V>> entryConsumer) {
        this.entryConsumer = entryConsumer;
        return this;
    }

    public OrderedEntry<K, V> getFirstOrderedEntry() {
        return entryInput.getFirstOrderedEntry();
    }

    public OrderedEntries load(File file) throws IOException {
        entryInput.setKeepEntries(true);
        entryReaderStack.push(newEntryReader((V) file.getAbsolutePath()));
        do {
            entryInput.read();
        } while (entryInput.getCurrentOrderedEntry() != null);
        return this;
    }

    public synchronized OrderedEntries load(Reader reader) throws IOException {
        entryInput.setKeepEntries(true);
        entryReaderStack.push(newEntryReader(reader));
        do {
            entryInput.read();
        } while (entryInput.getCurrentOrderedEntry() != null);
        return this;
    }

    public synchronized OrderedEntries load(String string) throws IOException {
        return load(new StringReader(string));
    }

    public synchronized OrderedEntries load(InputStream inputStream) throws IOException {
        return load(new InputStreamReader(inputStream));
    }

    public Stream<OrderedEntry<K, V>> stream(Reader reader) throws IOException {
        entryReaderStack.push(newEntryReader(reader));
        return readingStream();
    }

    public synchronized Stream<Stream<OrderedEntry<K, V>>> streams(Reader reader) throws IOException {
        entryReaderStack.push(newEntryReader(reader));
        return readingStreams();
    }

    public synchronized Stream<OrderedEntry<K, V>> stream(File file) throws IOException {
        entryReaderStack.push(newEntryReader((V) file.getAbsolutePath()));
        return readingStream();
    }

    public synchronized Stream<Stream<OrderedEntry<K, V>>> streams(File file) throws IOException {
        entryReaderStack.push(newEntryReader((V) file.getAbsolutePath()));
        return readingStreams();
    }

    public synchronized Stream<OrderedEntry<K, V>> stream(String string) throws IOException {
        return stream(new StringReader(string));
    }

    public synchronized Stream<Stream<OrderedEntry<K, V>>> streams(String string) throws IOException {
        return streams(new StringReader(string));
    }

    public Stream<OrderedEntry<K, V>> stream() {
        return stream(new StreamEntryReader(getFirstOrderedEntry()));
    }

    public synchronized Stream<OrderedEntry<K, V>> stream(InputStream inputStream) throws IOException {
        return stream(new InputStreamReader(inputStream));
    }

    public synchronized Stream<Stream<OrderedEntry<K, V>>> streams(InputStream inputStream) throws IOException {
        return streams(new InputStreamReader(inputStream));
    }

    private Stream<Stream<OrderedEntry<K, V>>> readingStreams() throws IOException {
        return readingStreams(() -> {
        });
    }

    private Stream<OrderedEntry<K, V>> readingStream() {
        return readingStream(() -> {
        });
    }

    private Stream<OrderedEntry<K, V>> readingStream(Runnable onClose) {
        return new BaseSpliterator<OrderedEntry<K, V>>()
                .setNextSupplier(() -> {
                    try {
                        return entryInput.read();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .setIsLastSupplier(() ->
                        entryInput.getNextOrderedEntry() == null || endOfStreamFunction.apply(entryInput.getCurrentOrderedEntry()))
                .setOnClose(onClose)
                .stream();
    }

    private Stream<Stream<OrderedEntry<K, V>>> readingStreams(Runnable onClose) {
        return new BaseSpliterator<Stream<OrderedEntry<K, V>>>()
                .setNextSupplier(() -> readingStream())
                .setIsLastSupplier(() -> entryInput.getNextOrderedEntry() == null)
                .setOnClose(onClose)
                .stream();
    }


    private Stream<OrderedEntry<K, V>> stream(StreamEntryReader streamEntryReader) {
        return new BaseSpliterator<OrderedEntry<K, V>>()
                .setNextSupplier(() -> streamEntryReader.readEntry())
                .setIsLastSupplier(() -> streamEntryReader.getNextEntry() == null || endOfStreamFunction.apply(streamEntryReader.getCurrentEntry()))
                .stream();

    }

    public synchronized Stream<Stream<OrderedEntry<K, V>>> streams() {
        StreamEntryReader streamEntryReader = new StreamEntryReader(getFirstOrderedEntry());
        return new BaseSpliterator<Stream<OrderedEntry<K, V>>>()
                .setNextSupplier(() -> stream(streamEntryReader))
                .setIsLastSupplier(() -> streamEntryReader.getNextEntry() == null)
                .stream();

    }

    /**
     * Fetches the next input value from the currently active {@link EntryReader} (the one which is on top of {@link EntryReaderStack}
     *
     * @return
     * @throws IOException
     */
    private OrderedEntry<K, V> readNext() throws IOException {
        OrderedEntry<K, V> orderedEntry = entryReaderStack.top().readEntry();
        if (orderedEntry != null && includePredicate.test(orderedEntry)) {
            V source = orderedEntry.getValue();
            entryReaderStack.push(newEntryReader(source));
            return readNext();
        }
        while (!entryReaderStack.empty() && orderedEntry == null) {
            entryReaderStack.pop().close();
            if (!entryReaderStack.empty()) {
                orderedEntry = entryReaderStack.top().readEntry();
                if (orderedEntry != null && includePredicate.test(orderedEntry)) {
                    V source = orderedEntry.getValue();
                    entryReaderStack.push(newEntryReader(source));
                    return readNext();
                }

            }
        }
        return orderedEntry;
    }

    private EntryReader newEntryReader(V source) throws FileNotFoundException {
        String sourceName = source.toString().toLowerCase();
        String fileName = sourceName;
        if (!entryReaderStack.empty()) {
            Object parentSource = entryReaderStack.top().getSource();
            if (parentSource == null) {
                throw new IllegalArgumentException("Included source could not be located");
            }
            String fileLocation = new File(parentSource.toString()).getParent();
            fileName = fileLocation + File.separator + sourceName;
        }
        File file = new File(fileName);
        Reader reader = new FileReader(file);
        EntryReader entryReader = new EntryReader(reader, fileName).enableTabsInKey(isTabsInKeyEnabled);
        return entryReader;
    }

    private EntryReader newEntryReader(Reader reader) {
        EntryReader entryReader = new EntryReader(reader).enableTabsInKey(isTabsInKeyEnabled);
        return entryReader;
    }

    /**
     * Provides reading ef entries and sustain link to the next succeding  entry
     */
    private class EntryInput {
        private OrderedEntry<K, V> firstOrderedEntry;
        private OrderedEntry<K, V> currentOrderedEntry;
        private OrderedEntry<K, V> nextOrderedEntry;
        private boolean keepEntries;

        /**
         * @param keepEntries If true all entries will be available for later access (with {@link #getFirstOrderedEntry()} as the entrance.
         *                    If false the entries will be a matter for GC.
         */
        public EntryInput(boolean keepEntries) {
            this.keepEntries = keepEntries;
        }


        /**
         * Each call results a new value in {@link #currentOrderedEntry} and {@link #nextOrderedEntry}.
         * {@link #currentOrderedEntry}==null means end of input
         * <p>
         * {@link #currentOrderedEntry} gets the value {@link #nextOrderedEntry},
         * and {@link #nextOrderedEntry} gets "the next" input value (fetched with  {@link #readNext()} .
         */
        public OrderedEntry<K, V> read() throws IOException {
            if (currentOrderedEntry == null) {  // First read
                currentOrderedEntry = readNext();
                if (currentOrderedEntry != null) {
                    handleDotsInKey.accept(currentOrderedEntry);
                }
                if (keepEntries) {
                    firstOrderedEntry = currentOrderedEntry;
                }
            } else {  // Next reads
                currentOrderedEntry = nextOrderedEntry;
            }

            // Right here a current entry is set. It may be null if end of file

            if (currentOrderedEntry == null) {  // End of input
                nextOrderedEntry = null;
            } else {
                nextOrderedEntry = readNext();
            }
            if (currentOrderedEntry != null) {
                currentOrderedEntry.setSuccessor(nextOrderedEntry);
            }

            // Rigth her we still have current entry.
            // And we have the next entry (it may be null if end of file)
            // And the current entry has link to the next entry (so to say...)

            if (currentOrderedEntry != null) {
                handleDotsInKey.accept(currentOrderedEntry);
                entryConsumer.accept(currentOrderedEntry);
            }
            return currentOrderedEntry;
        }

        public void setKeepEntries(boolean keepEntries) {
            this.keepEntries = keepEntries;
        }

        public OrderedEntry<K, V> getFirstOrderedEntry() {
            return firstOrderedEntry;
        }

        public OrderedEntry<K, V> getCurrentOrderedEntry() {
            return currentOrderedEntry;
        }

        public OrderedEntry<K, V> getNextOrderedEntry() {
            return nextOrderedEntry;
        }

    }

    /**
     * Reading of {@link  OrderedEntry<K, V>}'s
     */
    private class StreamEntryReader {
        OrderedEntry<K, V> nextEntry;
        OrderedEntry<K, V> currentEntry;

        /**
         * Identifies where to start reading
         */
        public StreamEntryReader(OrderedEntry<K, V> firstOrderedEntry) {
            nextEntry = firstOrderedEntry;
        }

        private OrderedEntry<K, V> readEntry() {
            currentEntry = nextEntry;
            nextEntry = nextEntry.getSuccessor();
            return currentEntry;
        }

        public OrderedEntry<K, V> getNextEntry() {
            return nextEntry;
        }

        public OrderedEntry<K, V> getCurrentEntry() {
            return currentEntry;
        }
    }


}

