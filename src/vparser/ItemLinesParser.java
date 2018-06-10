package vparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by Torsten on 10.06.2018.
 */
public class ItemLinesParser implements Iterator<Item> {


    private Reader reader;
    private BufferedReader bufferedReader;
    private String nextLine;

    private int lineCount;
    private int itemCount;
    private ItemParser parser;

    public ItemLinesParser(Reader reader) {
        this.reader = reader;
        parser = new ItemParser().enableCaseSensitiveName(false).enableLineType(true);

    }

    Stream<Item> stream() {
        open();
        Iterable<Item> iterable = () -> this;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    void open() {
        this.bufferedReader = new BufferedReader(reader);
        nextLine = readLine();
    }


    @Override
    public boolean hasNext() {
        return nextLine != null;
    }

    @Override
    public Item next() {
        String source = nextLine;
        int previousLineCount = lineCount;
        nextLine = readLine();
        while (hasNext() && (nextLine.length() == 0 || nextLine.charAt(0) == ' ')) {
            source = source.trim() + " " + nextLine.trim();
            nextLine = readLine();
        }

        Item item = new Item();
        parser.setLine(source);
        try {
            parser.parse();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Fejl i linie " + (previousLineCount - 1) + "-" + (lineCount - 2) + "\n" + e.getMessage());
        }
        item.setItemTypeName(parser.getLineType());
        item.setVariables(parser.getVariables());
        item.setSource(source);
        item.setLineCount(lineCount - 2);
        item.setCount(itemCount);
        itemCount++;
        return item;
    }

    private String readLine() {
        try {
            lineCount++;
            String line = bufferedReader.readLine();
            if (line == null) {
                bufferedReader.close();
            }
            return line;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isLiteral(String name) {
        return parser.isLiteral(name);
    }


}
