package torsete.util.entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import torsete.util.TestUtil;

import java.io.IOException;
import java.util.Properties;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Torsten on 14.06.2017.
 */
public class ReaderOrderedEntryIteratorTest {

    private TestUtil testUtil;
    private int testcount;

    @Before
    public void setUp() {
        testUtil = new TestUtil(this);
        testUtil.setupTestFolder();
    }

    @After
    public void tearDown() {
        testUtil.teardownTestFolder();
    }

    @Test
    public void test3Entries() {

        testUtil.writeFile("test",
                "0=a",
                "1=b",
                "2=c");


        OrderedEntryIterator iterator = new ReaderOrderedEntryIterator()
                .setReader(testUtil.getFileReader("test"))
                .open();

        assertTrue(iterator.hasNext());
        OrderedEntry next = iterator.next();
        assertEquals("0=a", next.getEntry().toString());
        assertEquals("0: 0=a", next.toString());
        assertEquals("1: 1=b", iterator.lookAhead().toString());

        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals("1=b", next.getEntry().toString());
        assertEquals("1: 1=b", next.toString());
        assertEquals("2: 2=c", iterator.lookAhead().toString());

        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals("2=c", next.getEntry().toString());
        assertEquals("2: 2=c", next.toString());

        assertNull(iterator.lookAhead());
        assertFalse(iterator.hasNext());

    }

    @Test
    public void test4WierdEntries() {
        testUtil.writeFile("test",
                "",
                "",
                "key1=a",  // 2
                "",
                "key2=b",  // 4
                "  # dfd   ",
                "  ! dfd   ",
                "\"=cde\\",
                "f",    // 8
                "k3=v3"); //9


        OrderedEntryIterator iterator = new ReaderOrderedEntryIterator()
                .setReader(testUtil.getFileReader("test"))
                .open();

        assertTrue(iterator.hasNext());
        OrderedEntry next = iterator.next();
        assertEquals("2: key1=a", next.toString());
        assertEquals("4: key2=b", iterator.lookAhead().toString());

        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals("4: key2=b", next.toString());
        assertEquals("8: \"=cdef", iterator.lookAhead().toString());

        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals("8: \"=cdef", next.toString());
        assertEquals("9: k3=v3", iterator.lookAhead().toString());

        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals("9: k3=v3", next.toString());

        assertNull(iterator.lookAhead());
        assertFalse(iterator.hasNext());

    }


    @Test
    public void testGenericEntries() {
        verify();
        verify("",
                "",
                "key1=a",
                "",
                "key2=b",
                "  # dfd   ",
                "  ! dfd   ",
                "\"=cde \\",
                "f",
                "k3=v3");
        verify("root=0",
                "\tkey1=1",
                "\t\tkey2=2",
                "\t\tkey3=3",
                "\txkey4=4",
                "\t\t\tkey5=5",
                "2root=6",
                ".key4=7",
                "...key5=8",
                "root2=9",
                "\tkey4=10",
                "\t\t\tkxxxey5=11",
                "");
        verify("test",
                "key1=a \\",
                "1",
                "key2=b\\\\",
                "key3=c \\",
                "  d \\",
                "e \\",
                "");
    }

    /**
     * Verifis that one set of lines vil give the same (key,value) pair in {@link Properties} and {@link ReaderOrderedEntryIterator
     *
     * @param lines
     */
    public void verify(String... lines) {
        String filename = "verify" + testcount++;
        testUtil.writeFile(filename, lines);

        Properties properties = new Properties();
        try {
            properties.load(testUtil.getFileReader(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        OrderedEntryIterator<String, String> iterator = new ReaderOrderedEntryIterator<String, String>()
                .setReader(testUtil.getFileReader(filename))
                .open();

        int count = 0;
        while (iterator.hasNext()) {
            OrderedEntry<String, String> next = iterator.next();
            String key = next.getKey().trim();
            String expectedValue = properties.getProperty(key);
            assertEquals(key + ":", expectedValue, next.getValue());
            count++;
        }
        assertEquals(properties.size(), count);

    }


}
