package torsete.util;

import java.util.Map;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by Torsten on 20.05.2017.
 */
class EntrySpliterator extends Spliterators.AbstractSpliterator<Map.Entry<Object, Object>> {

    private OrderedEntries orderedEntries;
    private Stream<Map.Entry<Object, Object>> stream;

    protected EntrySpliterator(OrderedEntries orderedEntries, long est, int additionalCharacteristics) {
        super(est, additionalCharacteristics);
        this.orderedEntries = orderedEntries;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Map.Entry<Object, Object>> action) {
        action.accept(orderedEntries.processEntry());
        if (orderedEntries.isLastEntryInPartition()) {
            stream.close();
            return false;
        }
        return true;
    }

    Stream<Map.Entry<Object, Object>> createStream(Runnable onClose) {
        return stream = StreamSupport.stream(this, false).onClose(onClose);
    }
}
