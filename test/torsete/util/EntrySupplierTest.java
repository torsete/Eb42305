package torsete.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

/**
 * Created by Torsten on 18.05.2017.
 */
public class EntrySupplierTest {
    private TestUtil testUtil;
    private EntrySupplier entrySupplier;

    @Before
    public void setUp() {
        testUtil = new TestUtil(this);
        testUtil.setupTestFolder();
        entrySupplier = new EntrySupplier();
    }

    @After
    public void tearDown() {
        testUtil.teardownTestFolder();
    }

    @Test
    public void testGetNone() throws IOException {
        testUtil.writeFile("test");
        BufferedReader reader = testUtil.getBufferedReader("test");
        entrySupplier.setReader(reader);
        Map.Entry<Object, Object> entry;

        entry = entrySupplier.get();
        assertNull(entry);
        assertNull(entrySupplier.getCurrentEntry());
        assertEquals(Integer.valueOf(-1), entrySupplier.getCurrentLineNumber());
    }

    @Test
    public void testGetOne() throws IOException {
        testUtil.writeFile("test", "key1=a");
        BufferedReader reader = testUtil.getBufferedReader("test");
        entrySupplier.setReader(reader);
        Map.Entry<Object, Object> entry;

        entry = entrySupplier.get();
        assertEquals("key1=a", entry.toString());
        assertEquals("key1=a", entrySupplier.getCurrentEntry().toString());
        assertEquals(Integer.valueOf(0), entrySupplier.getCurrentLineNumber());

        entry = entrySupplier.get();
        assertNull(entry);
        assertNull(entrySupplier.getCurrentEntry());
        assertEquals(Integer.valueOf(-1), entrySupplier.getCurrentLineNumber());
    }

    @Test
    public void testGetTwo() throws IOException {
        testUtil.writeFile("test", "key1=a", "key2=b");
        BufferedReader reader = testUtil.getBufferedReader("test");
        entrySupplier.setReader(reader);
        Map.Entry<Object, Object> entry;

        entry = entrySupplier.get();
        assertEquals("key1=a", entry.toString());
        assertEquals("key1=a", entrySupplier.getCurrentEntry().toString());
        assertEquals(Integer.valueOf(0), entrySupplier.getCurrentLineNumber());

        entry = entrySupplier.get();
        assertEquals("key2=b", entry.toString());
        assertEquals("key2=b", entrySupplier.getCurrentEntry().toString());
        assertEquals(Integer.valueOf(1), entrySupplier.getCurrentLineNumber());

        entry = entrySupplier.get();
        assertNull(entry);
        assertNull(entrySupplier.getCurrentEntry());
        assertEquals(Integer.valueOf(-1), entrySupplier.getCurrentLineNumber());
    }


    @Test
    public void testGetNoneWithLookahead() throws IOException {
        testUtil.writeFile("test");
        BufferedReader reader = testUtil.getBufferedReader("test");
        entrySupplier.setReader(reader);
        Map.Entry<Object, Object> entry;

        entry = entrySupplier.get();
        assertNull(entry);
        assertNull(entrySupplier.getCurrentEntry());
        assertNull(entrySupplier.getNextEntry());
        assertEquals(Integer.valueOf(-1), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(-1), entrySupplier.getNextLineNumber());
    }

    @Test
    public void testGetOneWithLookahead() throws IOException {
        testUtil.writeFile("test", "key1=a");
        BufferedReader reader = testUtil.getBufferedReader("test");
        entrySupplier.setReader(reader);
        Map.Entry<Object, Object> entry;

        entry = entrySupplier.get();
        assertEquals("key1=a", entry.toString());
        assertEquals("key1=a", entrySupplier.getCurrentEntry().toString());
        assertNull(entrySupplier.getNextEntry());
        assertEquals(Integer.valueOf(0), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(-1), entrySupplier.getNextLineNumber());

        entry = entrySupplier.get();
        assertNull(entry);
        assertNull(entrySupplier.getCurrentEntry());
        assertNull(entrySupplier.getNextEntry());
        assertEquals(Integer.valueOf(-1), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(-1), entrySupplier.getNextLineNumber());
    }

    @Test
    public void testGetTwoWithLookahead() throws IOException {
        testUtil.writeFile("test", "key1=a", "key2=b");
        BufferedReader reader = testUtil.getBufferedReader("test");
        entrySupplier.setReader(reader);
        Map.Entry<Object, Object> entry;

        entry = entrySupplier.get();
        assertEquals("key1=a", entry.toString());
        assertEquals("key1=a", entrySupplier.getCurrentEntry().toString());
        assertEquals("key2=b", entrySupplier.getNextEntry().toString());
        assertEquals(Integer.valueOf(0), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(1), entrySupplier.getNextLineNumber());

        entry = entrySupplier.get();
        assertEquals("key2=b", entry.toString());
        assertEquals("key2=b", entrySupplier.getCurrentEntry().toString());
        assertNull(entrySupplier.getNextEntry());
        assertEquals(Integer.valueOf(1), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(-1), entrySupplier.getNextLineNumber());

        entry = entrySupplier.get();
        assertNull(entry);
        assertNull(entrySupplier.getCurrentEntry());
        assertNull(entrySupplier.getNextEntry());
        assertEquals(Integer.valueOf(-1), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(-1), entrySupplier.getNextLineNumber());
    }

    @Test
    public void testMisc1() throws IOException {
        testUtil.writeFile("test",
                "key1=a", // 0
                "", // 1
                "key2=b", // 2
                "  # dfd   ", // 3
                "  ! dfd   ", // 4
                "\"=cde", // 5
                ""); // 6


        BufferedReader reader = testUtil.getBufferedReader("test");
        entrySupplier.setReader(reader);
        Map.Entry<Object, Object> entry;

        entry = entrySupplier.get();
        assertEquals("key1=a", entry.toString());
        assertEquals("key1=a", entrySupplier.getCurrentEntry().toString());
        assertEquals("key2=b", entrySupplier.getNextEntry().toString());
        assertEquals(Integer.valueOf(0), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(2), entrySupplier.getNextLineNumber());

        entry = entrySupplier.get();
        assertEquals("key2=b", entry.toString());
        assertEquals("key2=b", entrySupplier.getCurrentEntry().toString());
        assertEquals("\"=cde", entrySupplier.getNextEntry().toString());
        assertEquals(Integer.valueOf(2), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(5), entrySupplier.getNextLineNumber());

        entry = entrySupplier.get();
        assertEquals("\"=cde", entry.toString());
        assertEquals("\"=cde", entrySupplier.getCurrentEntry().toString());
        assertNull(entrySupplier.getNextEntry());
        assertEquals(Integer.valueOf(5), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(-1), entrySupplier.getNextLineNumber());

        entry = entrySupplier.get();
        assertNull(entry);
        assertNull(entrySupplier.getCurrentEntry());
        assertNull(entrySupplier.getNextEntry());
        assertEquals(Integer.valueOf(-1), entrySupplier.getCurrentLineNumber());
        assertEquals(Integer.valueOf(-1), entrySupplier.getNextLineNumber());

    }


}
