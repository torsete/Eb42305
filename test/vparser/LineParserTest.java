package vparser;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by Torsten on 08.06.2018.
 */
public class LineParserTest {

    private LineParser lineParser;

    @Before
    public void before() {
        lineParser = new LineParser();
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

        variables = lineParser.setLine("").parse().getVariables();
        assertEquals(0, variables.size());

        variables = lineParser.setLine(" ").parse().getVariables();
        assertEquals(0, variables.size());

        variables = lineParser.setLine("  ").parse().getVariables();
        assertEquals(0, variables.size());

        variables = lineParser.setLine("a=b").parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));

        variables = lineParser.setLine(" a = b").parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));

        variables = lineParser.setLine("a= b").parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));

        variables = lineParser.setLine(" a=b").parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));

        variables = lineParser.setLine(" a='b'".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("b", variables.get("a"));


        variables = lineParser.setLine("a=b c=d").parse().getVariables();
        assertEquals(2, variables.size());
        assertEquals("b", variables.get("a"));
        assertEquals("d", variables.get("c"));

        variables = lineParser.setLine(" a=b c='Dette er en ret38328&// lang tekst'".replace("'", "\"")).parse().getVariables();
        assertEquals(2, variables.size());
        assertEquals("b", variables.get("a"));
        assertEquals("Dette er en ret38328&// lang tekst", variables.get("c"));

        variables = lineParser.setLine("a=''".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("", variables.get("a"));

        variables = lineParser.setLine("a=' '".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals(" ", variables.get("a"));

        variables = lineParser.setLine("a='x''z '".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("x\"z ", variables.get("a"));

        variables = lineParser.setLine("a=''''".replace("'", "\"")).parse().getVariables();
        assertEquals(1, variables.size());
        assertEquals("\"", variables.get("a"));


    }

    @Test
    public void parse_invalid() throws Exception {
        try {
            lineParser.setLine(",").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine("a").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine("a=").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine("a=,").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine("=b").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine("a,b").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine(" a=\"b").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine(" a=b , c=d   ,").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine(", a=b , c=d  ").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine(",").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine("xxxx").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine("xxxx").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine("xxxx").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }
        try {
            lineParser.setLine("a=b a=b").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }

    }

    @Test
    public void getValue() {
        lineParser.setLine("a=b, c=d");
        assertNull(lineParser.getValue(""));
        assertEquals("b", lineParser.getValue("a"));
        assertEquals("d", lineParser.getValue("c"));
        assertFalse(lineParser.isString("a"));
        assertFalse(lineParser.isString("c"));
        assertFalse(lineParser.isString(""));
        assertFalse(lineParser.isString("xxxx"));

        lineParser.setLine("a=''".replace("'", "\""));
        assertEquals("", lineParser.getValue("a"));
        assertTrue(lineParser.isString("a"));

        lineParser.setLine("Tekst='Dette er en tekst'".replace("'", "\""));
        assertEquals("Dette er en tekst", lineParser.getValue("Tekst"));
        assertTrue(lineParser.isString("Tekst"));

        lineParser.setLine("Tekst='Dette er en tekst'".replace("'", "\""));
        assertEquals("Dette er en tekst", lineParser.getValue("tekst"));
        assertTrue(lineParser.isString("TEkst"));

        lineParser.setLine("Tekst=\"Dette er en \"\"tekst\"\"\" og_dette_er_en_variabel=10");
        assertEquals("Dette er en \"tekst\"", lineParser.getValue("Tekst"));
        assertTrue(lineParser.isString("tekst"));
        assertEquals("10", lineParser.getValue("og_dette_er_en_variabel"));
        assertFalse(lineParser.isString("og_dette_er_en_variabel"));
        assertEquals(2, lineParser.getVariables().size());
    }

    @Test
    public void testLineType() {
        try {
            lineParser.setLine("xxx a=b c=d").parse();
            fail("Forventer en IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println(e.toString());
        }

        lineParser.setLine("xxx a=b c=d").enableLineType(true).parse();
        assertEquals(2, lineParser.getVariables().size());
        assertEquals("b", lineParser.getValue("a"));
        assertEquals("d", lineParser.getValue("c"));
        assertEquals("xxx", lineParser.getLineType());
    }

    @Test
    public void testNameCaseSensitive() {
        lineParser.setLine("xXx a=b C=d").enableLineType(true).enableCaseSensitiveName(true).parse();
        assertEquals(2, lineParser.getVariables().size());
        assertEquals("b", lineParser.getValue("a"));
        assertEquals("d", lineParser.getValue("C"));
        assertEquals("xXx", lineParser.getLineType());
        assertNull(lineParser.getValue("A"));
        assertNull(lineParser.getValue("c"));

        lineParser.setLine("xXx a=b C=d").enableLineType(true).enableCaseSensitiveName(false).parse();
        assertEquals(2, lineParser.getVariables().size());
        assertEquals("b", lineParser.getValue("a"));
        assertEquals("d", lineParser.getValue("c"));
        assertEquals("b", lineParser.getValue("A"));
        assertEquals("d", lineParser.getValue("C"));
        assertEquals("xxx", lineParser.getLineType());
    }

    private void tryThis(String line) {
        System.out.println("****************");
        System.out.println(line);
        System.out.println("****************");
        LineParser lineParser = new LineParser();
        lineParser.setLine(line).parse().getVariables().entrySet().stream().forEach(es -> {
            System.out.println(es.getKey() + "=" + es.getValue());
        });
        System.out.println("****************");

    }
}