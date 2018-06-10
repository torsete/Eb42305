package vparser;

import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by Torsten on 10.06.2018.
 */
public class ItemLinesParserTest {


    @Test
    public void hasNext() {
        Reader reader;
        ItemLinesParser itemLinesParser;


        reader = new StringReader("");
        itemLinesParser = new ItemLinesParser(reader);
        itemLinesParser.open();
        assertFalse(itemLinesParser.hasNext());

        reader = new StringReader("x a=b");
        itemLinesParser = new ItemLinesParser(reader);
        itemLinesParser.open();
        assertTrue(itemLinesParser.hasNext());
        assertEquals("0/0 x [a=b]", itemLinesParser.next().toString());
        assertFalse(itemLinesParser.hasNext());

    }

    @Test
    public void stream_demo_valid() {
        String source = "x a=b\n" +
                "x c=d\n" +
                " e=f\n" +
                " tekst=\"Dette er en længere tekst\"\n" +
                " xxxe=f\n" +
                "xxxxx g=h\n" +
                "xxxxx g=h\n" +
                "xxxxx g=h\n" +
                "xxxxx g=h\n" +
                "   x=y\n" +
                "xxxxx g=h\n" +
                "xxxxx g=h\n" +
                "";
        Reader reader = new StringReader(source);

        int[] lc = {0};
        Arrays.stream(source.split("\n"))
                .forEach(l -> System.out.println("Line " + (lc[0]++) + ": " + l));
        int[] ic = {0};
        new ItemLinesParser(new StringReader(source)).stream()
                .forEach(i -> System.out.println("Item " + (ic[0]++) + ": " + i.getSource()));
        new ItemLinesParser(reader).stream()
                .forEach(i -> System.out.println(i.toString()));
    }

    @Test
    public void stream_demo_invalid() {
        String source = "x a=b\n" +
                "x c=d\n" +
                " e=f\n" +
                " tekst=\"Dette er en længere tekst\"\n" +
                " xxxe=f\n" +
                "xxxxx g=h\n" +
                "xxxxx g=h\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "xxxxx g=h\n" +
                "xxxxx g=h\n" +
                "   ?=y\n" +
                "xxxxx g=h\n" +
                "xxxxx g=h\n" +
                "";
        Reader reader = new StringReader(source);

        int[] lc = {0};
        Arrays.stream(source.split("\n"))
                .forEach(l -> System.out.println("Line " + (lc[0]++) + ": " + l));
        try {
            int[] ic = {0};
            new ItemLinesParser(new StringReader(source)).stream().count();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    public void isLiteral() {
        String source;
        ItemLinesParser itemLinesParser;
        Item item;

        source = "demo a=\"b\"" + "";
        new ItemLinesParser(new StringReader(source)).stream()
                .forEach(i -> System.out.println(i.getSource()));
        new ItemLinesParser(new StringReader(source)).stream()
                .forEach(i -> System.out.println(i.toString()));
        itemLinesParser = new ItemLinesParser(new StringReader(source));
        itemLinesParser.open();
        item = itemLinesParser.next();
        assertEquals(0, item.getCount());
        assertEquals(0, item.getLineCount());
        assertEquals(1, item.getVariables().size());
        assertEquals("b", item.getVariables().get("a"));
        assertTrue(itemLinesParser.isLiteral("a"));

        source = "demo literal=                     \n" +   // 0
                " \"Dette                           \n" +   // 1
                "      er et eksempel på en         \n" +   // 2
                " tekst der                         \n" +   // 3
                "\n" +                                      // 4
                "\n" +                                      // 5
                " er delt over                      \n" +   // 6
                " flere linier\"                    \n" +   // 7
                "";
        new ItemLinesParser(new StringReader(source)).stream()
                .forEach(i -> System.out.println(i.getSource()));
        new ItemLinesParser(new StringReader(source)).stream()
                .forEach(i -> System.out.println(i.toString()));
        itemLinesParser = new ItemLinesParser(new StringReader(source));
        itemLinesParser.open();
        item = itemLinesParser.next();
        assertEquals(0, item.getCount());
        assertEquals(7, item.getLineCount());
        assertEquals(1, item.getVariables().size());
        assertEquals("Dette er et eksempel på en tekst der er delt over flere linier", item.getVariables().get("literal"));
        assertTrue(itemLinesParser.isLiteral("literal"));

        source = "demo a=b" + "";
        new ItemLinesParser(new StringReader(source)).stream()
                .forEach(i -> System.out.println(i.getSource()));
        new ItemLinesParser(new StringReader(source)).stream()
                .forEach(i -> System.out.println(i.toString()));
        itemLinesParser = new ItemLinesParser(new StringReader(source));
        itemLinesParser.open();
        item = itemLinesParser.next();
        assertEquals(0, item.getCount());
        assertEquals(0, item.getLineCount());
        assertEquals(1, item.getVariables().size());
        assertEquals("b", item.getVariables().get("a"));
        assertFalse(itemLinesParser.isLiteral("a"));
    }

}