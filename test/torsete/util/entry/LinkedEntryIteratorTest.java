package torsete.util.entry;

import org.junit.Test;
import torsete.util.entry.util.EntryIterator;
import torsete.util.entry.util.LinkedEntryIterator;

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
public class LinkedEntryIteratorTest {

    @Test
    public void test0Entries() {
        EntryIterator iterator = new LinkedEntryIterator(null).open();
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

        for (int i = 0; i < orderedEntries.size(); i++) {
            if (i < orderedEntries.size() - 1) {
                orderedEntries.get(i).setSuccessor(orderedEntries.get(i + 1));
            }
        }
        EntryIterator iterator = new LinkedEntryIterator(orderedEntries.get(0)).open();

        assertTrue(iterator.hasNext());
        assertEquals("0=a", iterator.lookAhead().getEntry().toString());
        assertEquals("0=a", iterator.next().getEntry().toString());
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
        for (int i = 0; i < orderedEntries.size(); i++) {
            if (i < orderedEntries.size() - 1) {
                orderedEntries.get(i).setSuccessor(orderedEntries.get(i + 1));
            }
        }
        EntryIterator iterator = new LinkedEntryIterator(orderedEntries.get(0)).open();


        assertTrue(iterator.hasNext());
        assertEquals("0=a", iterator.lookAhead().getEntry().toString());

        assertEquals("0=a", iterator.next().getEntry().toString());
        assertEquals("1=b", iterator.lookAhead().getEntry().toString());

        assertTrue(iterator.hasNext());
        assertEquals("1=b", iterator.next().getEntry().toString());
        assertEquals("2=c", iterator.lookAhead().getEntry().toString());

        assertEquals("2=c", iterator.next().getEntry().toString());
        assertNull(iterator.lookAhead());
        assertFalse(iterator.hasNext());

    }


}
