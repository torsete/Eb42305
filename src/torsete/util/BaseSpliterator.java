package torsete.util;

import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by Torsten on 22.05.2017.
 */
public class BaseSpliterator<T> extends Spliterators.AbstractSpliterator<T> {
    private Stream<T> stream;
    private Supplier<T> nextSupplier;
    private Supplier<Boolean> isLastSupplier;
    private Runnable onClose;

    protected BaseSpliterator(long est, int additionalCharacteristics) {
        super(est, additionalCharacteristics);
        setNextSupplier(() -> null);
        setIsLastSupplier(() -> true);
        setOnClose(() -> {
        });
    }

    protected BaseSpliterator() {
        this(1, 2);
    }

    public BaseSpliterator setNextSupplier(Supplier<T> nextSupplier) {
        this.nextSupplier = nextSupplier;
        return this;
    }

    public BaseSpliterator setIsLastSupplier(Supplier<Boolean> isLastSupplier) {
        this.isLastSupplier = isLastSupplier;
        return this;
    }

    public BaseSpliterator setOnClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        action.accept(nextSupplier.get());
        if (isLastSupplier.get()) {
            stream.close();
            return false;
        }
        return true;
    }

    Stream<T> stream() {
        return stream = StreamSupport.stream(this, false).onClose(onClose);
    }

    /**
     * @param onClose Takes precedence over possible use {@link #onClose}
     * @return
     */
    Stream<T> stream(Runnable onClose) {
        return stream = StreamSupport.stream(this, false).onClose(onClose);
    }
}
