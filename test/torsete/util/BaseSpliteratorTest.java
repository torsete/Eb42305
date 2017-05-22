package torsete.util;

import org.junit.Test;

import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

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

}
