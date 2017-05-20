package torsete.util;

import java.util.Map;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by Torsten on 20.05.2017.
 */
class StreamSpliterator extends Spliterators.AbstractSpliterator<Stream<Map.Entry<Object, Object>>> {

    private OrderedEntries orderedEntries;
    private Stream<Stream<Map.Entry<Object, Object>>> streams;

    protected StreamSpliterator(OrderedEntries orderedEntries, long est, int additionalCharacteristics) {
        super(est, additionalCharacteristics);
        this.orderedEntries = orderedEntries;
    }


    @Override
    public boolean tryAdvance(Consumer<? super Stream<Map.Entry<Object, Object>>> action) {
        action.accept(orderedEntries.newStream(() -> {
            System.out.println("Stream onClose void");
        }));
        if (orderedEntries.isLastEntry()) {
            streams.close();
            return false;
        }
        return true;
    }

    public Stream<Stream<Map.Entry<Object, Object>>> createStreams(Runnable onClose) {
        return streams = StreamSupport.stream(this, false).onClose(onClose);
    }
}
