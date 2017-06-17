package torsete.util.entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import torsete.util.TestUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Torsten on 18.05.2017.
 */
public class EntriesTest {
    private Entries<Object, Object> entries;

    private TestUtil testUtil;

    @Before
    public void setUp() {
        testUtil = new TestUtil(this);
        testUtil.setupTestFolder();
        entries = new Entries<>().setIncludePredicate(entry -> entry.getKey().toString().toLowerCase().contains("include"));
    }

    @After
    public void tearDown() {
        testUtil.teardownTestFolder();
    }


    public String testHashMapKey(String readKey, String key, String value) throws IOException {
        HashMap<Object, Object> map = new HashMap<>();
        map.put(key, value);
        System.out.println("testHashMapKey " + asString(map.entrySet().iterator().next()));
        return (String) map.get(readKey);
    }

    public String testPropertiesKey(String readKey, String key, String value) throws IOException {
        Properties properties = new Properties();
        properties.put(key, value);
        System.out.println("testPropertiesKey " + asString(properties.entrySet().iterator().next()));
        return (String) properties.get(readKey);
    }

    public String testPropertiesReader(String readKey, String key, String value, String assign) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(key + assign + value));
        System.out.println("testPropertiesReader " + "\"" + assign + "\" " + asString(properties.entrySet().iterator().next()));
        return (String) properties.get(readKey);
    }

    private String asString(Map.Entry<Object, Object> entry) {
        return ("Key:>" + entry.getKey() + "<" + "Value:>" + entry.getValue() + "<").replace('\t', '.');
    }


    public void testKey(String readKey, String key, String value) throws IOException {
        assertEquals("HashMap", value, testHashMapKey(readKey, key, value));
        assertEquals("Properties", value, testPropertiesKey(readKey, key, value));
    }

    public void testKeyBlank(String expected, String readKey, String key, String value) throws IOException {
        assertEquals("Properties Blank", expected, testPropertiesReader(readKey, key, value, " "));
    }

    public void testKeyEqual(String expected, String readKey, String key, String value) throws IOException {
        assertEquals("Properties Equals", expected, testPropertiesReader(readKey, key, value, "="));
    }

    @Test
    public void testKeys() throws IOException {
        testKey("key", "key", "value");
        testKey("\tkey", "\tkey", "value");
        testKey("x\tkey", "x\tkey", "value");
        testKey("x\tkey\t\t", "x\tkey\t\t", "value");

        testKeyEqual("value", "key", "key", "value");
        testKeyEqual("value", "key", "\tkey", "value");
        testKeyEqual("value", "key", "\t\tkey", "value");
        testKeyEqual("y=value", "x", "x\ty", "value");
        testKeyEqual("y\t=value", "x", "x\ty\t", "value");
        testKeyEqual("y\t\t=value", "x", "x\ty\t\t", "value");
        testKeyEqual("y=value", "x", "x=y", "value");
        testKeyEqual("y\t=value", "x", "x=y\t", "value");
        testKeyEqual("y\t\t=value", "x", "x=y\t\t", "value");

        testKeyBlank("value", "key", "key", "value");
        testKeyBlank("value", "key", "\tkey", "value");
        testKeyEqual("value", "key", "\t\tkey", "value");
        testKeyBlank("y value", "x", "x\ty", "value");
        testKeyBlank("y\t value", "x", "x\ty\t", "value");
        testKeyBlank("y\t\t value", "x", "x\ty\t\t", "value");
        testKeyBlank("y value", "x", "x=y", "value");
        testKeyBlank("y\t value", "x", "x=y\t", "value");
        testKeyBlank("y\t\t value", "x", "x=y\t\t", "value");

    }

    @Test
    public void testEmpty() throws IOException {
        testUtil.writeFile("test", "");
    }

    @Test
    public void testInitial() throws IOException {
        testUtil.writeFile("test", "");


        entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
    }

    @Test
    public void testNormal() throws IOException {
        testUtil.writeFile("test",
                "key1=a",
                "",
                "key2=b",
                "  # dfd   ",
                "  ! dfd   ",
                "\"=cde",
                "");

        Properties properties = new Properties();
        properties.load(testUtil.getInputStream("test"));
        assertEquals(3, properties.size());
        assertEquals("a", properties.get("key1"));
        assertEquals("b", properties.get("key2"));
        assertEquals("cde", properties.get("\""));

        entries.clear();
        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFileReader("test")).load().getFirstEntry();


        String orderedAsString = entries.getEntriesAsString();
        System.out.println(orderedAsString);


        OrderedEntry<Object, Object> orderedEntry = firstEntry;


        assertEquals("key1=a", orderedEntry.getEntry().toString());
        orderedEntry = orderedEntry.getSuccessor();
        assertEquals("key2=b", orderedEntry.getEntry().toString());
        orderedEntry = orderedEntry.getSuccessor();
        assertEquals("\"=cde", orderedEntry.getEntry().toString());
    }

    @Test
    public void testLoad() throws IOException {
        testUtil.writeFile("test",
                "key1=a",
                "",
                "key2=b",
                "  # dfd   ",
                "  ! dfd   ",
                "\"=cde",
                "");


        Properties properties = new Properties();
        properties.load(testUtil.getFileReader("test"));
        properties.entrySet().forEach(e -> System.out.println("--->" + e));

        Consumer<OrderedEntry<Object, Object>> verify = (firstEntry) -> {

            OrderedEntry<Object, Object> orderedEntry = firstEntry;


            assertEquals("key1=a", orderedEntry.getEntry().toString());
            orderedEntry = orderedEntry.getSuccessor();
            assertEquals("key2=b", orderedEntry.getEntry().toString());
            orderedEntry = orderedEntry.getSuccessor();
            assertEquals(orderedEntry.toString(), "\"=cde", orderedEntry.getEntry().toString());
        };

        entries.clear();
        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
        verify.accept(firstEntry);

        entries.clear();
        firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
        verify.accept(firstEntry);

        entries.clear();
        firstEntry = entries.setSource(testUtil.getBufferedReader("test")).load().getFirstEntry();
        verify.accept(firstEntry);

        entries.clear();
        firstEntry = entries.setSource(testUtil.getContent("test")).load().getFirstEntry();
        verify.accept(firstEntry);

        entries.clear();
        firstEntry = entries.setSource(testUtil.getFileReader("test")).load().getFirstEntry();
        verify.accept(firstEntry);

    }

    @Test
    public void testInclude() throws IOException {
        testUtil.writeFile("test",
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


        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();


        verify(
                firstEntry, "key1=1",
                "keyB1=B1",
                "keyB2=B2",
                "keyA1=A1",
                "keyB1=B1",
                "keyB2=B2",
                "keyA2=A2",
                "keyD=d",
                "key2=2",
                "key3=3");

        String orderedAsString = entries.getEntriesAsString();
        System.out.println(orderedAsString);

    }

    @Test
    public void testFailingInclude() throws IOException {
        testUtil.writeFile("test",
                "key1=1",
                "include=testA",
                "key2=2",
                "key3=3",
                "");

        testUtil.writeFile("testB",
                "keyA1=A1",
                "");


        try {
            OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
            fail("Expects an RuntimeException");
        } catch (RuntimeException e) {
            assertEquals(FileNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testSelfReferencingInclude() throws IOException {
        testUtil.writeFile("test",
                "key1=1",
                "include=testA",
                "key2=2",
                "key3=3",
                "");

        testUtil.writeFile("testA",
                "include=testA",
                "");


        try {
            OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
            fail("Expects an RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("is self referencing"));
        }
    }

    @Test
    public void testNormalBackslash() throws IOException {
        testUtil.writeFile("test",
                "key1=a \\",
                "1",
                "key2=b\\\\",
                "key3=c \\",
                "  d \\",
                "e \\",
                "");

        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
        verify(firstEntry, "key1=a 1",
                "key2=b\\",
                "key3=c d e ");

    }

    @Test
    public void testContinuation() throws IOException {
        testUtil.writeFile("test",
                "key1=\\",
                "a",
                "key2=\\\\ ",
                "key3=\\/\\",
                " ");

        Properties properties = new Properties();
        properties.load(testUtil.getBufferedReader("test"));
        Object key2 = properties.get("key2");


        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
        verify(firstEntry, "key1=a",
                "key2=\\ ",
                "key3=/");

    }

    private void verify(OrderedEntry<Object, Object> firstEntry, String... expected) {
        OrderedEntry<Object, Object> orderedEntry = firstEntry;
        int i = 0;
        while (orderedEntry != null) {
            assertEquals("" + i, expected[i++], orderedEntry.getEntry().toString());
            orderedEntry = orderedEntry.getSuccessor();
        }
    }


    @Test
    public void testIncludeSelfReferencingFiles() throws IOException {
        testUtil.writeFile("test",
                "key1=1",
                "include=testA",
                "key2=2",
                "key3=3",
                "");

        testUtil.writeFile("testA",
                "include=testA",
                "include=testD",
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
                "include=testA",
                "");
        testUtil.writeFile("testD",
                "keyD=d",
                "");


        try {
            OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
            fail("Expects an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    @Test
    public void testIncludeMissingFilename() throws IOException {
        testUtil.writeFile("test", "include=");

        try {
            OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
            fail("Expects an RuntimeException");
        } catch (RuntimeException e) {
            assertEquals(FileNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testIncludeMissingFile() throws IOException {
        testUtil.writeFile("test", "include=xxxx");

        try {
            OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
            fail("Expects an RuntimeException");
        } catch (RuntimeException e) {
            assertEquals(FileNotFoundException.class, e.getCause().getClass());
        }
    }


    @Test
    public void testWeiredKeys() throws IOException {
        testUtil.writeFile("test",
                "a=0",
                "b",
                "c ",
                "d 1",
                "e  2",
                "");

        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
        verify(firstEntry, "a=0",
                "b=",
                "c=",
                "d=1",
                "e=2");

    }

    @Test
    public void testInputFunctionTabs() throws IOException {

        testUtil.writeFile("test",
                "x=0",
                "\t=1",
                "\ta\t=2",
                "");

        entries.enableTabsInKey(true);
        entries.addEntryConsumer(new DottedEntryKeyConsumer<>());
        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
        verify(firstEntry,
                "x=0",
                "x.=1",
                "x.a=2");
    }

    @Test
    public void testDotsKeys() throws IOException {
        testUtil.writeFile("test",
                "x=0",
                ".=1",
                ".a.=2",
                "");

        entries.addEntryConsumer(new DottedEntryKeyConsumer<>());
        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
        verify(firstEntry,
                "x=0",
                "x.=1",
                "x.a.=2");
    }


    @Test
    public void testManyDotsKeys() throws IOException {
        testUtil.writeFile("test",
                "root=0",
                "\tkey1=1",
                "\t\tkey2=2",
                "\t\tkey3=3",
                "\tkey4=4",
                "\t\t\tkey5=5",
                "root=6",
                ".key4=7",
                "...key5=8",
                "root2=9",
                "\tkey4=10",
                "\t\t\tkey5=11",
                "");

        entries.addEntryConsumer(new DottedEntryKeyConsumer<>());
        entries.enableTabsInKey(true);
        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
        verify(firstEntry,
                "root=0",
                "root.key1=1",
                "root.key1.key2=2",
                "root.key1.key3=3",
                "root.key4=4",
                "root.key4..key5=5",
                "root=6",
                "root.key4=7",
                "root.key4..key5=8",
                "root2=9",
                "root2.key4=10",
                "root2.key4..key5=11");

        String orderedAsString = entries.getEntriesAsString();
        System.out.println(orderedAsString);


        Properties properties = entries.properties();
        properties.entrySet().stream().forEach(es -> System.out.println(">>>" + es + "<<<"));
        properties = entries.properties();
        properties.entrySet().stream().forEach(es -> System.out.println(">>>" + es + "<<<"));


    }


    @Test
    public void testEntryConsumer() throws IOException {
        testUtil.writeFile("test",
                "key00=a",
                "key01=b",
                ".include=test1",
                ".include=test2",
                ".include=test2",
                "key01=e",
                "key02=f",
                "key03=g"
        );
        testUtil.writeFile("test1", "key1=c");
        testUtil.writeFile("test2", "key2=d");


        class MyConsumer implements Consumer<OrderedEntry<Object, Object>> {
            private int i;

            public void accept(OrderedEntry<Object, Object> oe) {
                System.out.println(oe + "  " + oe.getSuccessor());
                switch (i) {
                    case 0:
                        assertEquals("key00=a", oe.getEntry().toString());
                        assertEquals(Integer.valueOf(0), oe.getLineNumber());
                        break;
                    case 2:
                        assertEquals("key1=c", oe.getEntry().toString());
                        assertEquals(Integer.valueOf(0), oe.getLineNumber());
                        break;
                    case 7:
                        assertEquals("key03=g", oe.getEntry().toString());
                        assertEquals(Integer.valueOf(7), oe.getLineNumber());
                        break;
                    default:
                        break;
                }
                i++;
            }
        }

        MyConsumer myConsumer = new MyConsumer();
        entries.addEntryConsumer(myConsumer);

        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
    }

    @Test
    public void testStream() throws IOException {
        testUtil.writeFile("test",
                "",
                "key1=a",
                ".key2=b",
                "key2=b",
                "key2=c" +
                        "");


        int[] i = new int[1];
        entries.addEntryConsumer(new DottedEntryKeyConsumer<>());
        entries.setSource(testUtil.getFile("test"));
        entries.stream()
                .forEach(e -> {
                    System.out.println("" + e);
                    switch (i[0]++) {
                        case 0:
                            assertEquals("key1", e.getKey());
                            assertEquals("a", e.getValue());
                            break;
                        case 1:
                            assertEquals("key1.key2", e.getKey());
                            assertEquals("b", e.getValue());
                            break;
                        case 2:
                            assertEquals("key2", e.getKey());
                            assertEquals("b", e.getValue());
                            break;
                        case 3:
                            assertEquals("key2", e.getKey());
                            assertEquals("c", e.getValue());
                            break;
                        default:
                            break;
                    }
                });


    }

    @Test
    public void testSomeStream() throws IOException {
        testUtil.writeFile("test",
                "key1=a",
                ".key11=b",
                ".key12=c",
                "key2=d",
                ".key21=e",
                ".key22=e",
                ".key23=e",
                ".key24=e",
                ".key24=e",
                ".key25=25");

        testUtil.writeFile("testInclude",
                "key1=a",
                "include test11",
                "include test12");
        testUtil.writeFile("test11",
                ".key11=b");
        testUtil.writeFile("test12",
                "include testC");
        testUtil.writeFile("testC",
                ".key12=c",
                "key2=d",
                ".key21=e",
                ".key22=e",
                ".key23=e",
                ".key24=e",
                ".key24=e",
                ".key25=25");

        // File test and file tstInclude should now define the same entries

        Consumer<Stream<OrderedEntry<Object, Object>>> verifyCount = s -> {
            int[] i = {0};

            s.forEach(oe -> {
                switch (i[0]) {
                    case 0:
                        assertEquals("key1=a", oe.getEntry().toString());
                        break;
                    case 1:
                        assertEquals("key1.key11=b", oe.getEntry().toString());
                        break;
                    case 2:
                        assertEquals("key1.key12=c", oe.getEntry().toString());
                        break;
                    case 3:
                        assertEquals("key2=d", oe.getEntry().toString());
                        break;
                    case 7:
                        assertEquals("key2.key24=e", oe.getEntry().toString());
                        break;
                    case 8:
                        assertEquals("key2.key24=e", oe.getEntry().toString());
                        break;
                    case 9:
                        assertEquals("key2.key25=25", oe.getEntry().toString());
                        break;
                    default:
                        break;
                }
                i[0]++;
            });
            assertEquals(10, i[0]);
        };

//        verifyCount.accept(new Entries().enableDotsInKey(true).stream(testUtil.getInputStream("test")));
//        verifyCount.accept(new Entries().enableDotsInKey(true).orderedEntry(testUtil.getInputStream("test")).stream());
//        verifyCount.accept(new Entries().enableDotsInKey(true).stream(testUtil.getFile("test")));
//        verifyCount.accept(new Entries().enableDotsInKey(true).orderedEntry(testUtil.getFile("test")).stream());
//        verifyCount.accept(new Entries().enableDotsInKey(true).stream(testUtil.getFileReader("test")));
//        verifyCount.accept(new Entries().enableDotsInKey(true).orderedEntry(testUtil.getFileReader("test")).stream());
//        verifyCount.accept(new Entries().enableDotsInKey(true).stream(testUtil.getBufferedReader("test")));
//        verifyCount.accept(new Entries().enableDotsInKey(true).orderedEntry(testUtil.getBufferedReader("test")).stream());
//        verifyCount.accept(new Entries().enableDotsInKey(true).stream(testUtil.getContent("test")));
//        verifyCount.accept(new Entries().enableDotsInKey(true).orderedEntry(testUtil.getContent("test")).stream());
//
//        verifyCount.accept(new Entries().enableDotsInKey(true).stream(testUtil.getFile("testInclude")));
//        verifyCount.accept(new Entries().enableDotsInKey(true).orderedEntry(testUtil.getFile("testInclude")).stream());

    }

    @Test
    public void testKeepEntries() throws IOException {
        testUtil.writeFile("test",
                "key1=a",
                ".key11=b",
                ".key12=c",
                "key2=d",
                ".key21=e",
                ".key22=e",
                ".key23=e",
                ".key24=e",
                ".key24=e",
                ".key25=25");
//
//
//        Consumer<Consumer<Entries<Object, Object>>> verifyNull = r -> {
//            Entries entries;
//            entries = new Entries().enableDotsInKey(true);
//            r.accept(entries);
//            assertNull(entries.getFirstOrderedEntry());
//        };
//        Consumer<Consumer<Entries<Object, Object>>> verifyNotNull = r -> {
//            Entries entries;
//            entries = new Entries().enableDotsInKey(true);
//            r.accept(entries);
//            assertNotNull(entries.getFirstOrderedEntry());
//        };
//
//        verifyNull.accept(oes -> {
//            try {
//                oes.stream(testUtil.getInputStream("test"));
//            } catch (IOException e) {
//                fail(e.getMessage());
//            }
//        });
//        verifyNull.accept(oes -> {
//            oes.stream(testUtil.getFile("test"));
//        });
//        verifyNull.accept(oes -> {
//            try {
//                oes.stream(testUtil.getContent("test"));
//            } catch (IOException e) {
//                fail(e.getMessage());
//            }
//        });
//        verifyNull.accept(oes -> {
//            try {
//                oes.stream(testUtil.getBufferedReader("test"));
//            } catch (IOException e) {
//                fail(e.getMessage());
//            }
//        });
//
//        verifyNotNull.accept(oes -> {
//            try {
//                oes.orderedEntry(testUtil.getInputStream("test")).stream();
//            } catch (IOException e) {
//                fail(e.getMessage());
//            }
//        });
//        verifyNotNull.accept(oes -> {
//
//            oes.orderedEntry(testUtil.getFile("test")).stream();
//
//        });
//        verifyNotNull.accept(oes -> {
//
//            oes.orderedEntry(testUtil.getFileReader("test")).stream();
//
//        });
//        verifyNotNull.accept(oes -> {
//            try {
//                oes.orderedEntry(testUtil.getContent("test")).stream();
//            } catch (IOException e) {
//                fail(e.getMessage());
//            }
//        });
//        verifyNotNull.accept(oes -> {
//            try {
//                oes.orderedEntry(testUtil.getBufferedReader("test")).stream();
//            } catch (IOException e) {
//                fail(e.getMessage());
//            }
//        });

    }


    @Test
    public void showStreamCase() throws IOException {
        testUtil.writeFile("test",
                // This is stream 1:
                "form      Name of form 1",
                ".id       formId1",
                ".h        Header 1",
                ".input    Enter text",
                "..id      inputId",
                "..validation code1",
                // This is stream2 :
                ".form      Name of form 2",
                ".id       formId1",
                ".h        Header 1",
                ".text     dsasadadsasd",
                ".text     dds erre fgg54 ghg",
                ".....text     dds erre fgg54 ghg",
                ".text     1123 vfd  bffgfgfgggb  ffg",
                ".input    Enter text",
                "..id      inputId",
                "wwww.xxxxx.validation code1",
                ".....trailerr  A text",
                "........c c");
        entries.clear();
        entries.setSource(testUtil.getInputStream("test"));
        OrderedEntryIterator<Object, Object> iterator = entries.addEntryConsumer(new DottedEntryKeyConsumer<>()).iterator();
        iterator.forEachRemaining(e -> {
            System.out.println("-------------------");
            System.out.println("" + e);
            System.out.println("-------------------");
        });
        entries.clear();
        entries.setSource(testUtil.getInputStream("test"));
        Stream<OrderedEntry<Object, Object>> stream = entries.addEntryConsumer(new DottedEntryKeyConsumer<>()).stream();
        stream.forEach(e -> {
            System.out.println("-------------------");
            System.out.println("" + e);
            System.out.println("-------------------");
        });
        stream.close();

        entries.clear();
        entries.setSource(testUtil.getInputStream("test"));
        stream = entries.addEntryConsumer(new DottedEntryKeyConsumer<>()).stream();
        Iterator<OrderedEntry<Object, Object>> spelieratorIterator = stream.iterator();

        while (spelieratorIterator.hasNext()) {
            System.out.println("" + spelieratorIterator.next());
        }

    }

    @Test
    public void test3Entries() throws FileNotFoundException {
        testUtil.writeFile("test",
                "0=a",
                "1=b",
                "2=c");

        OrderedEntry<Object, Object> firstEntry = entries.setSource(testUtil.getFile("test")).load().getFirstEntry();
        OrderedEntryIterator<Object, Object> iterator = entries.iterator();

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
