package vparser;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Torsten on 10.06.2018.
 */
public class Item {
    /**
     * One up per Item
     */
    private int count;

    /**
     * One up per input line (one Item may be implemented by several input lines)
     */
    private int lineCount;

    /**
     * Source lines
     */
    private String source;

    /**
     * Type og input item
     */
    private String itemTypeName;

    /**
     * Variables in the Item. If null the item is a comment
     */
    private Map<String, String> variables;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getItemTypeName() {
        return itemTypeName;
    }

    public void setItemTypeName(String itemTypeName) {
        this.itemTypeName = itemTypeName;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(count + "/");
        sb.append(lineCount + " ");
        sb.append(itemTypeName + " ");
        sb.append(variables.keySet().stream().sorted().map(k -> {
            String v = variables.get(k);
            if (v.startsWith("\"")) {
                v = v + "\"";
            }
            return k + "=" + v;
        }).collect(Collectors.toList()).toString());
        return sb.toString();
    }
}
