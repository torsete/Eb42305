package vparser;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by Torsten on 08.06.2018.
 */
public class ItemParserTest {

    private ItemParser itemParser;

    @Before
    public void before() {
        itemParser = new ItemParser();
    }

    @Test
    public void parse_demo() throws Exception {
//
//        tryThis("a=b");
//        tryThis("a=");
        tryThis("a=fddfb ,c=d");
//        tryThis((""));
    }

    @Test
    public void parse_valid() throws Exception {
        Map<String, String> variables;

        variables = itemParser.setLine("").parse().getVariables();
        assertEquals(0, variables.size());

        variables = itemParser.setLine(" ").parse().getVariables();
        assertEquals(0, variables.size());

        variables = itemParser.setLine("  ").parse().getVariables();
        assertEquals(0, variables.size());

        variables = itemParser.setLine("a=b").parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));

        variables = itemParser.setLine(" a = b").parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));

        variables = itemParser.setLine("a= b").parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));

        variables = itemParser.setLine(" a=b").parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));

        variables = itemParser.setLine(" a='b'".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));

        variables = itemParser.setLine("a=b c=d").parse().getVariables();
        assertEquals(2, variables.size());
        assertEquals("b", variables.get("a"));
        assertEquals("d", variables.get("c"));

        variables = itemParser.setLine(" a=b c='Dette er en ret38328&// lang tekst'".replace("'", "\"")).parse().getVariables();
        assertEquals(2, variables.size());
        assertEquals("b", variables.get("a"));
        assertEquals("Dette er en ret38328&// lang tekst", variables.get("c"));

        variables = itemParser.setLine("a=''".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("", variables.get("a"));

        variables = itemParser.setLine("a=' '".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals(" ", variables.get("a"));

        variables = itemParser.setLine("a='x''z '".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("x\"z ", variables.get("a"));

        variables = itemParser.setLine("a=''''".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("\"", variables.get("a"));
    }

    @Test
    public void parse_invalid() throws Exception {
        try {
            itemParser.setLine(",").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine("a").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine("a=").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine("a=,").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine("=b").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine("a,b").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine(" a=\"b").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine(" a=b , c=d   ,").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine(", a=b , c=d  ").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine(",").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine("xxxx").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine("xxxx").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine("xxxx").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            itemParser.setLine("a=b a=b").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }

    }

    @Test
    public void getValue() {
        itemParser.setLine("a=b, c=d");
        assertNull(itemParser.getValue(""));
        assertEquals("b", itemParser.getValue("a"));
        assertEquals("d", itemParser.getValue("c"));
        assertFalse(itemParser.isLiteral("a"));
        assertFalse(itemParser.isLiteral("c"));
        assertFalse(itemParser.isLiteral(""));
        assertFalse(itemParser.isLiteral("xxxx"));

        itemParser.setLine("a=''".replace("'", "\""));
        assertEquals("", itemParser.getValue("a"));
        assertTrue(itemParser.isLiteral("a"));

        itemParser.setLine("Tekst='Dette er en tekst'".replace("'", "\""));
        assertEquals("Dette er en tekst", itemParser.getValue("Tekst"));
        assertTrue(itemParser.isLiteral("Tekst"));

        itemParser.setLine("Tekst='Dette er en tekst'".replace("'", "\""));
        assertEquals("Dette er en tekst", itemParser.getValue("tekst"));
        assertTrue(itemParser.isLiteral("TEkst"));

        itemParser.setLine("Tekst=\"Dette er en \"\"tekst\"\"\" og_dette_er_en_variabel=10");
        assertEquals("Dette er en \"tekst\"", itemParser.getValue("Tekst"));
        assertTrue(itemParser.isLiteral("tekst"));
        assertEquals("10", itemParser.getValue("og_dette_er_en_variabel"));
        assertFalse(itemParser.isLiteral("og_dette_er_en_variabel"));
        assertEquals(2, itemParser.getVariables().size());
    }

    @Test
    public void testLineType() {
        try {
            itemParser.setLine("xxx a=b c=d").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }

        try {
            itemParser.setLine("a=b").enableLineType(true).parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }

        itemParser.setLine("x a=b").enableLineType(true).parse();
        assertEquals(1, itemParser.getVariables().size());
        assertEquals("b", itemParser.getValue("a"));
        assertEquals("x", itemParser.getLineType());

        itemParser.setLine("xxx a=b c=d").enableLineType(true).parse();
        assertEquals(2, itemParser.getVariables().size());
        assertEquals("b", itemParser.getValue("a"));
        assertEquals("d", itemParser.getValue("c"));
        assertEquals("xxx", itemParser.getLineType());
    }

    @Test
    public void testNameCaseSensitive() {
        itemParser.setLine("xXx a=b C=d").enableLineType(true).enableCaseSensitiveName(true).parse();
        assertEquals(2, itemParser.getVariables().size());
        assertEquals("b", itemParser.getValue("a"));
        assertEquals("d", itemParser.getValue("C"));
        assertEquals("xXx", itemParser.getLineType());
        assertNull(itemParser.getValue("A"));
        assertNull(itemParser.getValue("c"));

        itemParser.setLine("xXx a=b C=d").enableLineType(true).enableCaseSensitiveName(false).parse();
        assertEquals(2, itemParser.getVariables().size());
        assertEquals("b", itemParser.getValue("a"));
        assertEquals("d", itemParser.getValue("c"));
        assertEquals("b", itemParser.getValue("A"));
        assertEquals("d", itemParser.getValue("C"));
        assertEquals("xxx", itemParser.getLineType());
    }

    private void tryThis(String line) {
        System.out.println("****************");
        System.out.println(line);
        System.out.println("****************");
        ItemParser itemParser = new ItemParser();
        itemParser.setLine(line).parse().getVariables().entrySet().stream().forEach(es -> {
            System.out.println(es.getKey() + "=" + es.getValue());
        });
        System.out.println("****************");

    }}