package torsete.util.entry;

import org.junit.Test;
import torsete.util.entry.util.EntryIterator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Torsten on 14.06.2017.
 */
public class EntryIteratorTest {

    @Test
    public void test0Entries() {
        Map<String, String> map = new HashMap<>();

        List<LinkedEntry<String, String>> orderedEntries = map.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(es -> new LinkedEntry<>(es))
                .collect(Collectors.toList());

        EntryIterator iterator = new MyIterator(orderedEntries);

        assertFalse(iterator.hasNext());
        assertNull(iterator.lookAhead());
    }

    @Test
    public void test1Entries() {
        Map<String, String> map = new HashMap<>();
        map.put("0", "a");


        List<LinkedEntry<String, String>> orderedEntries = map.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(es -> new LinkedEntry<>(es))
                .collect(Collectors.toList());

        EntryIterator iterator = new MyIterator(orderedEntries);

        assertTrue(iterator.hasNext());
        assertEquals("0=a", iterator.lookAhead().toString());
        assertEquals("0=a", iterator.next().toString());
        assertNull(iterator.lookAhead());
        assertFalse(iterator.hasNext());

    }

    @Test
    public void test3Entries() {
        Map<String, String> map = new HashMap<>();
        map.put("0", "a");
        map.put("1", "b");
        map.put("2", "c");

        List<LinkedEntry<String, String>> orderedEntries = map.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(es -> new LinkedEntry<>(es))
                .collect(Collectors.toList());

        EntryIterator iterator = new MyIterator(orderedEntries);

        assertTrue(iterator.hasNext());
        assertEquals("0=a", iterator.lookAhead().toString());

        assertEquals("0=a", iterator.next().toString());
        assertEquals("1=b", iterator.lookAhead().toString());

        assertTrue(iterator.hasNext());
        assertEquals("1=b", iterator.next().toString());
        assertEquals("2=c", iterator.lookAhead().toString());

        assertEquals("2=c", iterator.next().toString());
        assertNull(iterator.lookAhead());
        assertFalse(iterator.hasNext());

    }


    class MyIterator extends EntryIterator<String, String> {
        private int i;
        private List<LinkedEntry<String, String>> orderedEntries;

        public MyIterator(List<LinkedEntry<String, String>> orderedEntries) {
            this.orderedEntries = orderedEntries;
            i = 0;
            open();
        }

        @Override
        protected LinkedEntry<String, String> readEntry() {
            if (i >= orderedEntries.size()) {
                return null;
            }
            return orderedEntries.get(i++);
        }
    }

}
