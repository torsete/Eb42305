package torsete.util.stream;

import org.junit.Test;

import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Created by Torsten on 22.05.2017.
 */
public class BaseSpliteratorTest {

    @Test
    public void testDemo() {
        char[] chars = {'a', 'b', 'c'};
        int[] i = new int[1];
        Stream stream = new BaseSpliterator<Character>()
                .setNextSupplier(() -> chars[i[0]++])
                .setIsLastSupplier(() -> i[0] == chars.length)
                .stream();

        StringBuilder sb = new StringBuilder();
        stream.forEach(c -> sb.append(c));
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testOnClose1() {
        char[] chars = {'a', 'b', 'c'};
        boolean[] onCloseDone = {false};
        int[] i = new int[1];
        Stream stream = new BaseSpliterator<Character>()
                .setNextSupplier(() -> chars[i[0]++])
                .setIsLastSupplier(() -> i[0] == chars.length)
                .stream(() -> {
                    onCloseDone[0] = true;
                });

        StringBuilder sb = new StringBuilder();
        stream.forEach(c -> sb.append(c));
        assertEquals("abc", sb.toString());
        assertTrue(onCloseDone[0]);
    }

    @Test
    public void testOnClose2() {
        char[] chars = {'a', 'b', 'c'};
        boolean[] onCloseDone = {false};
        int[] i = new int[1];
        Stream stream = new BaseSpliterator<Character>()
                .setNextSupplier(() -> chars[i[0]++])
                .setIsLastSupplier(() -> i[0] == chars.length)
                .setOnClose(() -> {
                    onCloseDone[0] = true;
                })
                .stream();

        StringBuilder sb = new StringBuilder();
        stream.forEach(c -> sb.append(c));
        assertEquals("abc", sb.toString());
        assertTrue(onCloseDone[0]);
    }

    @Test
    public void testOnClose3() {
        char[] chars = {'a', 'b', 'c'};
        boolean[] onCloseDone0 = {false};
        boolean[] onCloseDone1 = {false};
        int[] i = new int[1];
        Stream stream = new BaseSpliterator<Character>()
                .setNextSupplier(() -> chars[i[0]++])
                .setIsLastSupplier(() -> i[0] == chars.length)
                .setOnClose(() -> {
                    onCloseDone0[0] = true;
                })
                .stream(() -> {
                    onCloseDone1[0] = true;
                });

        StringBuilder sb = new StringBuilder();
        stream.forEach(c -> sb.append(c));
        assertEquals("abc", sb.toString());
        assertFalse(onCloseDone0[0]);
        assertTrue(onCloseDone1[0]);
    }

}
