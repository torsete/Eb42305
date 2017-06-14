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
public class IncludingFileOrderedEntryIteratorTest {

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


        OrderedEntryIterator iterator = new IncludingFileOrderedEntryIterator<>()
                .setSource(testUtil.getFile("test").getAbsolutePath())
                .open();

        assertTrue(iterator.hasNext());
        OrderedEntry next = iterator.next();
        assertEquals("0=a", next.getEntry().toString());
        assertEquals("0=a", next.getEntry().toString());
        assertEquals("1=b", iterator.lookAhead().getEntry().toString());

        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals("1=b", next.getEntry().toString());
        assertEquals("1=b", next.getEntry().toString());
        assertEquals("2=c", iterator.lookAhead().getEntry().toString());

        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals("2=c", next.getEntry().toString());

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
    }


    @Test
    public void testInclude() throws IOException {
        testUtil.writeFile("test",
                "key0=0",
                "key1=1",
                "include=testA",
                "key2=2",
                "key3=3",
                "");

        testUtil.writeFile("testA",
                "include=testB",
                "keyA1=A1",
                "include=testB",
                "keyA2=A2",
                "include=testC",
                "");
        testUtil.writeFile("testB",
                "keyB1=B1",
                "keyB2=B2",
                "");
        testUtil.writeFile("testC",
                "include=testD",
                "");
        testUtil.writeFile("testD",
                "keyD=d",
                "");

        OrderedEntryIterator iterator = new IncludingFileOrderedEntryIterator<>()
                .setSource(testUtil.getFile("test").getAbsolutePath())
                .open();

        while (iterator.hasNext()) {
            OrderedEntry next = iterator.next();
            System.out.println("" + next);
        }

    }

    /**
     * Verifies that one set of lines vil give the same (key,value) pair in {@link Properties} and {@link IncludingFileOrderedEntryIterator
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

        OrderedEntryIterator iterator = new IncludingFileOrderedEntryIterator<>()
                .setSource(testUtil.getFile(filename).getAbsolutePath())
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
