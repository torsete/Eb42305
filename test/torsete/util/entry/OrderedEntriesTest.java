package torsete.util.entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import torsete.util.TestUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Torsten on 18.05.2017.
 */
public class OrderedEntriesTest {
    private OrderedEntries<Object, Object> orderedEntries;

    private TestUtil testUtil;

    @Before
    public void setUp() {
        testUtil = new TestUtil(this);
        testUtil.setupTestFolder();
        orderedEntries = new OrderedEntries();
    }

    @After
    public void tearDown() {
        testUtil.teardownTestFolder();
    }


    @Test
    public void testEmpty() throws IOException {
        testUtil.writeFile("test", "");
    }

    @Test
    public void testInitial() throws IOException {
        testUtil.writeFile("test", "");


        orderedEntries.load(testUtil.getFile("test"));
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


        orderedEntries.load(testUtil.getFileReader("test"));

        String orderedAsString = orderedEntries.getOrderedAsString();
        System.out.println(orderedAsString);


        OrderedEntry<Object, Object> orderedEntry = orderedEntries.getFirstOrderedEntry();


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
        Runnable verify = () -> {

            OrderedEntry<Object, Object> orderedEntry = orderedEntries.getFirstOrderedEntry();


            assertEquals("key1=a", orderedEntry.getEntry().toString());
            orderedEntry = orderedEntry.getSuccessor();
            assertEquals("key2=b", orderedEntry.getEntry().toString());
            orderedEntry = orderedEntry.getSuccessor();
            assertEquals("\"=cde", orderedEntry.getEntry().toString());
        };

        orderedEntries.clear();
        orderedEntries.load(testUtil.getFile("test"));
        verify.run();

        orderedEntries.clear();
        orderedEntries.load(testUtil.getInputStream("test"));
        verify.run();

        orderedEntries.clear();
        orderedEntries.load(testUtil.getBufferedReader("test"));
        verify.run();

        orderedEntries.clear();
        orderedEntries.load(testUtil.getContent("test"));
        verify.run();

        orderedEntries.clear();
        orderedEntries.load(testUtil.getFileReader("test"));
        verify.run();

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


        orderedEntries.load(testUtil.getFile("test"));
        verify(orderedEntries, "key1=1",
                "keyB1=B1",
                "keyB2=B2",
                "keyA1=A1",
                "keyB1=B1",
                "keyB2=B2",
                "keyA2=A2",
                "keyD=d",
                "key2=2",
                "key3=3");

        String orderedAsString = orderedEntries.getOrderedAsString();
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
            orderedEntries.load(testUtil.getFile("test"));
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
            orderedEntries.load(testUtil.getFile("test"));
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

        orderedEntries.load(testUtil.getFile("test"));
        verify(orderedEntries, "key1=a 1",
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


        orderedEntries.load(testUtil.getFile("test"));
        verify(orderedEntries, "key1=a",
                "key2=\\ ",
                "key3=/");

    }

    private void verify(OrderedEntries<Object, Object> orderedEntries, String... expected) {
        OrderedEntry<Object, Object> orderedEntry = orderedEntries.getFirstOrderedEntry();
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
                "include=testB",
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
            orderedEntries.load(testUtil.getFile("test"));
            fail("Expects an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    @Test
    public void testIncludeMissingFilename() throws IOException {
        testUtil.writeFile("test", "include=");

        try {
            orderedEntries.load(testUtil.getFile("test"));
            fail("Expects an RuntimeException");
        } catch (RuntimeException e) {
            assertEquals(FileNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testIncludeMissingFile() throws IOException {
        testUtil.writeFile("test", "include=xxxx");

        try {
            orderedEntries.load(testUtil.getFile("test"));
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

        orderedEntries.load(testUtil.getFile("test"));
        verify(orderedEntries, "a=0",
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

        orderedEntries.enableDotsInKey(true);
        orderedEntries.enableTabsInKey(true);
        orderedEntries.load(testUtil.getFile("test"));
        verify(orderedEntries,
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

        orderedEntries.enableDotsInKey(true);
        orderedEntries.load(testUtil.getFile("test"));
        verify(orderedEntries,
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

        orderedEntries.enableDotsInKey(true);
        orderedEntries.enableTabsInKey(true);
        orderedEntries.load(testUtil.getFile("test"));
        verify(orderedEntries,
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

        String orderedAsString = orderedEntries.getOrderedAsString();
        System.out.println(orderedAsString);

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
        orderedEntries.setEntryConsumer(myConsumer);

        orderedEntries.load(testUtil.getFile("test"));
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
        orderedEntries.enableDotsInKey(true);
        orderedEntries.stream(testUtil.getFile("test"))
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

        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).stream(testUtil.getInputStream("test")));
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).load(testUtil.getInputStream("test")).stream());
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).stream(testUtil.getFile("test")));
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).load(testUtil.getFile("test")).stream());
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).stream(testUtil.getFileReader("test")));
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).load(testUtil.getFileReader("test")).stream());
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).stream(testUtil.getBufferedReader("test")));
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).load(testUtil.getBufferedReader("test")).stream());
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).stream(testUtil.getContent("test")));
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).load(testUtil.getContent("test")).stream());

        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).stream(testUtil.getFile("testInclude")));
        verifyCount.accept(new OrderedEntries().enableDotsInKey(true).load(testUtil.getFile("testInclude")).stream());

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


        Consumer<Consumer<OrderedEntries<Object, Object>>> verifyNull = r -> {
            OrderedEntries orderedEntries;
            orderedEntries = new OrderedEntries().enableDotsInKey(true);
            r.accept(orderedEntries);
            assertNull(orderedEntries.getFirstOrderedEntry());
        };
        Consumer<Consumer<OrderedEntries<Object, Object>>> verifyNotNull = r -> {
            OrderedEntries orderedEntries;
            orderedEntries = new OrderedEntries().enableDotsInKey(true);
            r.accept(orderedEntries);
            assertNotNull(orderedEntries.getFirstOrderedEntry());
        };

        verifyNull.accept(oes -> {
            try {
                oes.stream(testUtil.getInputStream("test"));
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
        verifyNull.accept(oes -> {
            oes.stream(testUtil.getFile("test"));
        });
        verifyNull.accept(oes -> {
            try {
                oes.stream(testUtil.getContent("test"));
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
        verifyNull.accept(oes -> {
            try {
                oes.stream(testUtil.getBufferedReader("test"));
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });

        verifyNotNull.accept(oes -> {
            try {
                oes.load(testUtil.getInputStream("test")).stream();
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
        verifyNotNull.accept(oes -> {

                oes.load(testUtil.getFile("test")).stream();

        });
        verifyNotNull.accept(oes -> {

                oes.load(testUtil.getFileReader("test")).stream();

        });
        verifyNotNull.accept(oes -> {
            try {
                oes.load(testUtil.getContent("test")).stream();
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
        verifyNotNull.accept(oes -> {
            try {
                oes.load(testUtil.getBufferedReader("test")).stream();
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });

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
                "form      Name of form 2",
                ".id       formId1",
                ".h        Header 1",
                ".text     dsasadadsasd",
                ".text     dds erre fgg54 ghg",
                ".....text     dds erre fgg54 ghg",
                ".text     1123 vfd  bffgfgfgggb  ffg",
                ".input    Enter text",
                "..id      inputId",
                "..validation code1",
                ".trailerr  A text",
                "");

        Stream<OrderedEntry<Object, Object>> stream = orderedEntries.stream(testUtil.getInputStream("test"));
        stream.forEach(e -> {
            System.out.println("-------------------");
            System.out.println("" + e);
            System.out.println("-------------------");
        });

    }

    @Test
    public void test3Entries() {
        testUtil.writeFile("test",
                "0=a",
                "1=b",
                "2=c");

        orderedEntries.load(testUtil.getFile("test"));
        OrderedEntryIterator<Object, Object> iterator = orderedEntries.iterator();

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
