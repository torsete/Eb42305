package torsete.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertTrue;

/**
 * Created by Torsten on 18.05.2017.
 */
public class OrderedPropertiesTest {
    private String testFolderName;
    private Predicate<Map.Entry<Object, Object>> includeKeyPredicate;
    private OrderedProperties orderedProperties;

    private TestUtil testUtil;

    @Before
    public void setUp() {
        testUtil = new TestUtil(this);
        testUtil.setupTestFolder();
        testFolderName = testUtil.getTestFolderName();
        includeKeyPredicate = entry -> entry.toString().toLowerCase().contains("include");
        orderedProperties = new OrderedProperties().setIncludePredicate(includeKeyPredicate);
    }

    @After
    public void tearDown() {
        testUtil.teardownTestFolder();
    }


    @Test
    public void testInitial() throws IOException {
        writeFile("test", "");

        assertNull(orderedProperties.getSourcename());
        assertEquals(0, orderedProperties.size());
        assertEquals(0, orderedProperties.getEntries().size());
        assertEquals(-1, orderedProperties.getOrderedEntriesIndex("xxx"));

        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        assertEquals(testFolderName + File.separator + "test", orderedProperties.getSourcename());
        assertEquals(0, orderedProperties.size());
        assertEquals(0, orderedProperties.getEntries().size());
        assertEquals(-1, orderedProperties.getOrderedEntriesIndex("xxx"));
    }

    @Test
    public void testNormal1() throws IOException {
        writeFile("test",
                "key1=a",
                "",
                "key2=b",
                "  # dfd   ",
                "  ! dfd   ",
                "\"=cde",
                "");

        Properties properties = new Properties();
        properties.load(new FileInputStream(testFolderName + File.separator + "test"));
        assertEquals(3, properties.size());
        assertEquals("a", properties.get("key1"));
        assertEquals("b", properties.get("key2"));
        assertEquals("cde", properties.get("\""));


        orderedProperties.load(new File(testFolderName + File.separator + "test"));

        assertEquals(3, orderedProperties.size());
        assertEquals("a", orderedProperties.readFirst("key1"));
        assertEquals("b", orderedProperties.readFirst("key2"));
        assertEquals("cde", orderedProperties.readFirst("\""));

        List<Map.Entry<Object, Object>> ordered = orderedProperties.getEntries();
        assertEquals(3, ordered.size());
        assertEquals("a", ordered.get(0).getValue());
        assertEquals("b", ordered.get(1).getValue());
        assertEquals("cde", ordered.get(2).getValue());

        List<Integer> orderedLineNumbers = orderedProperties.getLinenumbers();
        assertEquals(3, orderedLineNumbers.size());
        assertEquals(Integer.valueOf(0), orderedLineNumbers.get(0));
        assertEquals(Integer.valueOf(2), orderedLineNumbers.get(1));
        assertEquals(Integer.valueOf(5), orderedLineNumbers.get(2));

        assertEquals("key1", ordered.get(0).getKey());
        assertEquals("key2", ordered.get(1).getKey());
        assertEquals("\"", ordered.get(2).getKey());

        assertTrue(orderedProperties.isUnique("key1"));
        assertTrue(orderedProperties.isUnique("key2"));
        assertTrue(orderedProperties.isUnique("\""));

        assertEquals(-1, orderedProperties.getOrderedEntriesIndex("xxx"));
    }

    @Test
    public void testNormal2() throws IOException {
        writeFile("test",
                "",
                "key1=a\\",
                "b\\",
                "   c",
                "   ",
                "#",
                "!",
                "",
                "key2=d",
                "");

        Properties properties = new Properties();
        properties.load(new FileInputStream(testFolderName + File.separator + "test"));
        assertEquals(2, properties.size());
        assertEquals("abc", properties.get("key1"));
        assertEquals("d", properties.get("key2"));

        orderedProperties.load(new File(testFolderName + File.separator + "test"));

        assertEquals(2, orderedProperties.size());
        assertEquals("abc", orderedProperties.readFirst("key1"));
        assertEquals("d", orderedProperties.readFirst("key2"));

        List<Map.Entry<Object, Object>> ordered = orderedProperties.getEntries();
        assertEquals(2, ordered.size());
        assertEquals("abc", ordered.get(0).getValue());
        assertEquals("d", ordered.get(1).getValue());


        List<Integer> orderedLineNumbers = orderedProperties.getLinenumbers();
        assertEquals(2, orderedLineNumbers.size());
        assertEquals(Integer.valueOf(1), orderedLineNumbers.get(0));
        assertEquals(Integer.valueOf(8), orderedLineNumbers.get(1));

    }

    @Test
    public void testNormalBackslash() throws IOException {
        writeFile("test",
                "key1=a \\",
                "1",
                "key2=b\\\\",
                "key3=c \\",
                "  d \\",
                "e \\",
                "");

        orderedProperties.load(new File(testFolderName + File.separator + "test"));

        assertEquals(3, orderedProperties.size());
        assertEquals(3, orderedProperties.getEntries().size());

        assertEquals("a1", orderedProperties.readFirst("key1"));
        assertEquals("b\\", orderedProperties.readFirst("key2"));
        assertEquals("cde", orderedProperties.readFirst("key3"));

        assertEquals("a1", orderedProperties.getEntries().get(0).getValue());
        assertEquals("b\\", orderedProperties.getEntries().get(1).getValue());
        assertEquals("cde", orderedProperties.getEntries().get(2).getValue());

        assertEquals("key1", orderedProperties.getEntries().get(0).getKey());
        assertEquals("key2", orderedProperties.getEntries().get(1).getKey());
        assertEquals("key3", orderedProperties.getEntries().get(2).getKey());
    }

    @Test
    public void demoDoubleProperty() throws IOException {
        InputStream inputStream;
        /**
         * Lav en properties-fil:
         *
         * key=abd
         * key=def
         */
        String filename = testFolderName + File.separator + "test";

        Writer writer = new BufferedWriter(new FileWriter(filename));
        writer.write("key=abc");
        writer.write("\n");
        writer.write("key=def");
        writer.close();

        /**
         * Sådan gøres det med "den sædvanlige" Properties:
         */
        Properties properties = new Properties();
        inputStream = new FileInputStream(filename);
        properties.load(inputStream);
        assertEquals(1, properties.size());
        assertEquals("def", properties.get("key"));

        /**
         * Og sådan gøres det med SequentialProperties
         */
        OrderedProperties orderedProperties = new OrderedProperties();
        inputStream = new FileInputStream(filename);
        orderedProperties.load(inputStream);

        /**
         * Begge properties kan aflæses:
         */
        assertEquals(2, orderedProperties.getEntries().size());
        assertEquals("abc", orderedProperties.getEntries().get(0).getValue());
        assertEquals("def", orderedProperties.getEntries().get(1).getValue());
        assertEquals("key", orderedProperties.getEntries().get(1).getKey());
        assertEquals("key", orderedProperties.getEntries().get(0).getKey());
        assertEquals(0, orderedProperties.getOrderedEntriesIndex("key"));
        assertFalse(orderedProperties.isUnique("key"));

        /**
         * And an {@link OrderedProperties} is still a {@link Properties}:
         */
        assertEquals(1, orderedProperties.size());
        assertEquals("def", orderedProperties.get("key"));
    }

    @Test
    public void demoIncludeFiles() throws IOException {
        Writer writer;
        /**
         * Lav  properties-filer:
         */
        String filename = testFolderName + File.separator + "test";
        writer = new BufferedWriter(new FileWriter(filename));
        writer.write("include=test1");
        writer.write("\n");
        writer.write("include=test2");
        writer.close();
        writer = new BufferedWriter(new FileWriter(testFolderName + File.separator + "test1"));
        writer.write("key1=value1");
        writer.close();
        writer = new BufferedWriter(new FileWriter(testFolderName + File.separator + "test2"));
        writer.write("key2=value2");
        writer.write("\n");
        writer.write("key3=value3");
        writer.close();

        OrderedProperties orderedProperties = new OrderedProperties();
        File inputFile = new File(filename);
        orderedProperties.load(inputFile);  // File fordi der skal være et filsysten at læse fra

        assertEquals(3, orderedProperties.getEntries().size());

        assertEquals("value1", orderedProperties.readFirst("key1"));
        assertEquals("value2", orderedProperties.readFirst("key2"));
        assertEquals("value3", orderedProperties.readFirst("key3"));
    }

    @Test
    public void testSequence() throws IOException {
        writeFile("test",
                "key2=abc",
                "key1=def",
                "");

        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        assertEquals(2, orderedProperties.size());
        assertEquals(2, orderedProperties.getEntries().size());

        assertEquals("def", orderedProperties.readFirst("key1"));
        assertEquals("abc", orderedProperties.readFirst("key2"));

        assertEquals("abc", orderedProperties.getEntries().get(0).getValue());
        assertEquals("def", orderedProperties.getEntries().get(1).getValue());
        assertEquals("key2", orderedProperties.getEntries().get(0).getKey());
        assertEquals("key1", orderedProperties.getEntries().get(1).getKey());
        assertEquals(0, orderedProperties.getOrderedEntriesIndex("key2"));
        assertEquals(1, orderedProperties.getOrderedEntriesIndex("key1"));
    }

    @Test
    public void testFilenames() throws IOException {
        writeFile("test",
                "IwrdDocFolder=IWRD/",
                "DefaultDocFolder=GAIA/",
                "GaiaTemp=D:\\\\Javapgm\\\\GaiaTemp\\\\",
                "Lessor=\\\\\\\\10.10.20.18\\\\Lessor4\\\\DATA\\\\",
                "Notes=N:\\\\",
                "Notesdata=N:\\\\data\\\\",
                "BREVSYSTEMFOLDER=ALL/GAIA/",
                "# Når data fra JTS-processer skal skrives lokalt m/u/backup",
                "JTSDATAEJBACKUP=D:\\\\GSPTEMP\\\\",
                "JTSDATABACKUP=D:\\\\GSPDATA\\\\",
                "");

        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        assertEquals(9, orderedProperties.size());
        assertEquals(9, orderedProperties.getEntries().size());

        verifyValue("IwrdDocFolder", "IWRD/");
        verifyValue("DefaultDocFolder", "GAIA/");
        verifyValue("GaiaTemp", "D:\\Javapgm\\GaiaTemp\\");
        verifyValue("Lessor", "\\\\10.10.20.18\\Lessor4\\DATA\\");
        verifyValue("Notes", "N:\\");
        verifyValue("Notesdata", "N:\\data\\");
        verifyValue("BREVSYSTEMFOLDER", "ALL/GAIA/");
        verifyValue("JTSDATAEJBACKUP", "D:\\GSPTEMP\\");
        verifyValue("JTSDATABACKUP", "D:\\GSPDATA\\");

    }

    @Test
    public void testContinuation() throws IOException {
        writeFile("test",
                "key1=\\",
                "a",
                "key2=\\\\",
                "key3=\\/\\",
                "");

        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        verifyValues();

    }

    @Test
    public void test3() throws IOException {
        writeFile("test",
                "key",
                "");

        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        verifyValues();

    }

    @Test
    public void testIncludeFiles() throws IOException {
        writeFile("test",
                "include=test1",
                "include=test2",
                "key=value",
                "");
        writeFile("test1",
                "key1=value1",
                "");
        writeFile("test2",
                "key2=value2",
                "");


        orderedProperties.load(new File(testFolderName + File.separator + "test"));
//        assertEquals(1, orderedProperties.size(false));
        assertEquals(3, orderedProperties.size());
        assertEquals(3, orderedProperties.getEntries().size());

        assertEquals("value", orderedProperties.readFirst("key"));
        assertEquals("value1", orderedProperties.readFirst("key1"));
        assertEquals("value2", orderedProperties.readFirst("key2"));

        /**
         * Bemærk at sekvensen er bestemt af rekursionen
         */
        assertEquals("value1", orderedProperties.getEntries().get(0).getValue());
        assertEquals("value2", orderedProperties.getEntries().get(1).getValue());
        assertEquals("value", orderedProperties.getEntries().get(2).getValue());
        assertEquals("key1", orderedProperties.getEntries().get(0).getKey());
        assertEquals("key2", orderedProperties.getEntries().get(1).getKey());
        assertEquals("key", orderedProperties.getEntries().get(2).getKey());
        assertEquals(2, orderedProperties.getOrderedEntriesIndex("key"));
        assertEquals(0, orderedProperties.getOrderedEntriesIndex("key1"));
        assertEquals(1, orderedProperties.getOrderedEntriesIndex("key2"));


        assertEquals(testFolderName + File.separator + "test", orderedProperties.getSourcename());


    }

    @Test
    public void testIncludeFiles2() throws IOException {
        writeFile("test",
                "include=test2",
                "include=test1",
                "key=value",
                "");
        writeFile("test1",
                "key1=value1",
                "");
        writeFile("test2",
                "key2=value2",
                "include=test3",
                "");
        writeFile("test3",
                "key10=10",
                "key11=11",
                "");


        orderedProperties.load(new File(testFolderName + File.separator + "test"));


        traverseSequentialProperties(orderedProperties, 0);

        assertEquals(5, orderedProperties.size());
        assertEquals(5, orderedProperties.getEntries().size());

        assertEquals("value", orderedProperties.readFirst("key"));
        assertEquals("value1", orderedProperties.readFirst("key1"));
        assertEquals("value2", orderedProperties.readFirst("key2"));
        assertEquals("10", orderedProperties.readFirst("key10"));
        assertEquals("11", orderedProperties.readFirst("key11"));



        assertEquals(testFolderName + File.separator + "test", orderedProperties.getSourcename());

    }


    @Test
    public void testWeiredKeys() throws IOException {
        writeFile("test",
                "a=0",
                "b",
                "c ",
                "d 1",
                "e  2",
                "");

        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        assertEquals(5, orderedProperties.size());
        assertEquals(5, orderedProperties.getEntries().size());
        assertEquals("0", orderedProperties.readFirst("a"));
        assertEquals("", orderedProperties.readFirst("b"));
        assertEquals("", orderedProperties.readFirst("c"));
        assertEquals("1", orderedProperties.readFirst("d"));
        assertEquals("2", orderedProperties.readFirst("e"));
        verifyEntry("a", "0", orderedProperties.getEntries().get(0));
        verifyEntry("b", "", orderedProperties.getEntries().get(1));
        verifyEntry("c", "", orderedProperties.getEntries().get(2));
        verifyEntry("d", "1", orderedProperties.getEntries().get(3));
        verifyEntry("e", "2", orderedProperties.getEntries().get(4));
    }

    @Test
    public void testInputFunctionTabs() throws IOException {
        BiFunction<OrderedProperties, String, String> inputFunction = (op, l) -> {
            return l.replace('\t', '.');
        };

        writeFile("test",
                "=0",
                "\t=1",
                "\ta\t=2",
                "");

        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        assertEquals(2, orderedProperties.size());
        assertEquals("2", orderedProperties.readFirst("a"));
        assertEquals("0", orderedProperties.readFirst(""));

        assertEquals(3, orderedProperties.getEntries().size());
        verifyEntry("", "0", orderedProperties.getEntries().get(0));
        verifyEntry("", "1", orderedProperties.getEntries().get(1));
        verifyEntry("a", "2", orderedProperties.getEntries().get(2));


        orderedProperties.clear();
        orderedProperties.enableDotsInKey(true);
        orderedProperties.enableTabsInKey(true);
        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        assertEquals(3, orderedProperties.getEntries().size());
        assertEquals("0", orderedProperties.readFirst(""));
        assertEquals("1", orderedProperties.readFirst("."));
        assertEquals("2", orderedProperties.readFirst(".a"));

        assertEquals(3, orderedProperties.getEntries().size());
        verifyEntry("", "0", orderedProperties.getEntries().get(0));
        verifyEntry(".", "1", orderedProperties.getEntries().get(1));
        verifyEntry(".a", "2", orderedProperties.getEntries().get(2));
    }

    @Test
    public void testDotsKeys() throws IOException {
        writeFile("test",
                "=0",
                ".=1",
                ".a.=2",
                "");

        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        assertEquals(3, orderedProperties.size());
        assertEquals("0", orderedProperties.readFirst(""));
        assertEquals("1", orderedProperties.readFirst("."));
        assertEquals("2", orderedProperties.readFirst(".a."));

        assertEquals(3, orderedProperties.getEntries().size());
        verifyEntry("", "0", orderedProperties.getEntries().get(0));
        verifyEntry(".", "1", orderedProperties.getEntries().get(1));
        verifyEntry(".a.", "2", orderedProperties.getEntries().get(2));


        orderedProperties.clear();
        orderedProperties.enableDotsInKey(true);
        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        assertEquals(3, orderedProperties.getEntries().size());
        assertEquals("0", orderedProperties.readFirst(""));
        assertEquals("1", orderedProperties.readFirst("."));
        assertEquals("2", orderedProperties.readFirst(".a."));

        assertEquals(3, orderedProperties.getEntries().size());
        verifyEntry("", "0", orderedProperties.getEntries().get(0));
        verifyEntry(".", "1", orderedProperties.getEntries().get(1));
        verifyEntry(".a.", "2", orderedProperties.getEntries().get(2));
    }

    @Test
    public void testDotsKeys0() throws IOException {
        writeFile("test",
                ".=1",
                "");

        orderedProperties.enableDotsInKey(true);
        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        assertEquals(1, orderedProperties.size());
        assertEquals("1", orderedProperties.readFirst("."));
    }

    @Test
    public void testManyDotsKeys() throws IOException {
        writeFile("test",
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

        orderedProperties.enableDotsInKey(true);
        orderedProperties.enableTabsInKey(true);
        orderedProperties.load(new File(testFolderName + File.separator + "test"));
        List<Map.Entry<Object, Object>> sequential = orderedProperties.getEntries();
        assertEquals(12, sequential.size());
        int i = 0;
        verifyEntry("root", "0", sequential.get(i++));
        verifyEntry("root.key1", "1", sequential.get(i++));
        verifyEntry("root.key1.key2", "2", sequential.get(i++));
        verifyEntry("root.key1.key3", "3", sequential.get(i++));
        verifyEntry("root.key4", "4", sequential.get(i++));
        verifyEntry("root.key4..key5", "5", sequential.get(i++));
        verifyEntry("root", "6", sequential.get(i++));
        verifyEntry("root.key4", "7", sequential.get(i++));
        verifyEntry("root.key4..key5", "8", sequential.get(i++));
        verifyEntry("root2", "9", sequential.get(i++));
        verifyEntry("root2.key4", "10", sequential.get(i++));
        verifyEntry("root2.key4..key5", "11", sequential.get(i++));


        String s = orderedProperties.toString().replace(',', '\n');
        System.out.println(s);
    }

    private void verifyEntry(String expectedKey, String expectedValue, Map.Entry<Object, Object> entry) {
        assertEquals(expectedKey, entry.getKey().toString());
        assertEquals(expectedValue, entry.getValue().toString());
    }


    /**
     * Tjekker at værdier er ok ift. Properties indlæst via fil til en sædvanlig Properties
     *
     * @param orderedProperties
     */
    private void verifyValuesIsEqualsToStandardProperties(OrderedProperties orderedProperties) {
        File file = new File(orderedProperties.getSourcename());
        Properties standardProperties = new Properties();
        try {
            standardProperties.load(new FileInputStream(file));
        } catch (IOException e) {
            fail(e.getMessage());
        }


        assertTrue(orderedProperties.getEntries().size() >= orderedProperties.size());

        Enumeration<?> enumeration = standardProperties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement().toString();
            if (key.contains("include")) {
                continue;
            }
            verifyValue(orderedProperties, key, orderedProperties.readFirst(key).toString());
        }

    }

    private void verifyValues() {
        assertTrue(orderedProperties.size() > 0);
        assertEquals(orderedProperties.size(), orderedProperties.getEntries().size());
        Enumeration<?> enumeration = orderedProperties.propertyNames();
        int count = 0;
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement().toString();
            verifyValue(key, orderedProperties.readFirst(key).toString());
            count++;
        }
        assertEquals(orderedProperties.size(), count);
        verifyValuesIsEqualsToStandardProperties(orderedProperties);

    }

    private void verifyValue(OrderedProperties orderedProperties, String key, String expctedValue) {
        assertEquals(orderedProperties.getSourcename() + " key=" + key, expctedValue, orderedProperties.readFirst(key));
        int index = orderedProperties.getOrderedEntriesIndex(key);
        assertTrue(orderedProperties.getSourcename() + " key=" + key, index >= 0);
        assertEquals(orderedProperties.getSourcename() + " key=" + key, key, orderedProperties.getEntries().get(index).getKey());
        if (orderedProperties.isUnique(key)) {
            assertEquals(orderedProperties.getSourcename() + " key=" + key, expctedValue, orderedProperties.getEntries().get(index).getValue());

        }
    }

    private void verifyValue(String key, String expctedValue) {
        verifyValue(orderedProperties, key, expctedValue);
    }

    List<String> filenames = new ArrayList<>();

    private void testConfig(String foldername) throws IOException {
        for (File file : new File(foldername).listFiles()) {
            String filename = file.getAbsolutePath();
            if (file.isDirectory()) {
                testConfig(filename);
            } else {
                testConfigfile(filename);
                filename = filename.replaceAll("\\\\", "\\\\\\\\");
//                if (!filenames.contains(filename)) {
                filenames.add(filename);
//                }
            }
        }
    }

    private void testConfigfile(String filename) throws IOException {
        File file = new File(filename);
        OrderedProperties orderedProperties = new OrderedProperties().setIncludePredicate(includeKeyPredicate);
        orderedProperties.load(file);

        assertTrue(filename, orderedProperties.getEntries().size() >= orderedProperties.size());
        if (orderedProperties.size() > 0) {
            assertTrue(filename, orderedProperties.getEntries().size() > 0);
        }

        System.out.println();
        System.out.println("Undersøger " + orderedProperties.getSourcename());

        traverseSequentialProperties(orderedProperties, 0);
    }

    private void traverseSequentialProperties(OrderedProperties orderedProperties, int level) {

        String vistFilnavn = new File(orderedProperties.getSourcename()).getParentFile().getName() + "/" + new File(orderedProperties.getSourcename()).getName();
        File file = new File(orderedProperties.getSourcename());
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
        } catch (IOException e) {
            fail(e.getMessage());
        }


        String indent = "";// indentTextHelper.trim("", level * 2, ' ');
        System.out.println(indent + vistFilnavn +
                " global=" + (orderedProperties.size() == properties.size() ? "" : (orderedProperties.size() + " ")) +
                "lokal=" + properties.size() +
                (orderedProperties.getEntries().size() != orderedProperties.size() ? " dublerede=" + (orderedProperties.getEntries().size() - orderedProperties.size()) : "") +
                "");

        verifyValuesIsEqualsToStandardProperties(orderedProperties);
    }


    private void writeFile(String filename, String content) throws IOException {
        Writer writer = new BufferedWriter(new FileWriter(testFolderName + File.separator + filename));
        writer.write(content);
        writer.close();
        System.out.println("******************************");
        System.out.println(new File(filename).getAbsoluteFile() + ":");
        System.out.println(content);
        System.out.println("******************************");
    }

    private void writeFile(String filename, String... lines) throws IOException {
        writeFile(filename, Arrays.stream(lines).collect(Collectors.joining("\n")));
    }

}
