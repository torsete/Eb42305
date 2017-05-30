package torsete.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by Torsten on 18.05.2017.
 */
public class OrderedPropertiesTest {
    private Predicate<OrderedEntry<Object, Object>> includeKeyPredicate;
    private OrderedProperties orderedProperties;

    private TestUtil testUtil;

    @Before
    public void setUp() {
        testUtil = new TestUtil(this);
        testUtil.setupTestFolder();
        includeKeyPredicate = entry -> entry.toString().toLowerCase().contains("include");
        orderedProperties = new OrderedProperties();
        orderedProperties.setIncludePredicate(includeKeyPredicate);
    }

    @After
    public void tearDown() {
        testUtil.teardownTestFolder();
    }

    @Test
    public void testDuplicates() throws IOException {
        testUtil.writeFile("test",
                "key=value",
                "key1=a",
                "key1=a",
                "key1=b",
                "key1=c",
                "key2=d",
                "include=testA",
                "");
        testUtil.writeFile("testA",
                "key1=1",
                "key1=2",
                "key1=3",
                "key1=4",
                "key2=5",
                "");

        orderedProperties.load(testUtil.getFile("test"));

        assertEquals(3, orderedProperties.size());
        assertEquals(1, (orderedProperties.get("key")).size());
        assertEquals("[value]", orderedProperties.get("key").toString());
        assertEquals(8, (orderedProperties.get("key1")).size());
        assertEquals("[a, a, b, c, 1, 2, 3, 4]", orderedProperties.get("key1").toString());
        assertEquals(2, (orderedProperties.get("key2")).size());
        assertEquals("[d, 5]", orderedProperties.get("key2").toString());

        Set<Map.Entry<Object, Object>> entries = orderedProperties.entrySet();
        assertEquals(3, entries.size());
        Enumeration<?> enumeration = orderedProperties.propertyNames();
        assertEquals("key", enumeration.nextElement().toString().substring(0, 3));
        assertEquals("key", enumeration.nextElement().toString().substring(0, 3));
        assertEquals("key", enumeration.nextElement().toString().substring(0, 3));
        assertFalse(enumeration.hasMoreElements());
    }
}
