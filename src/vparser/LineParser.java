package vparser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Torsten on 08.06.2018.
 */
public class LineParser {
    private String line;
    private int pos;

    private StringBuilder leftSide;
    private StringBuilder rightSide;
    private Map<String, String> variables;

    public LineParser setLine(String line) {
        this.line = line;
        variables = null;
        return this;
    }

    public LineParser parse() {
        return parseLine();
    }

    public String getValue(String name) {
        parse();
        String value = variables.get(name);
        return isStringValue(value) ? value.substring(1) : value;
    }

    public boolean isString(String name) {
        parse();
        return isStringValue(variables.get(name));
    }

    public Map<String, String> getVariables() {
        Map<String, String> variables = new HashMap<>();
        this.variables.keySet().forEach(k -> variables.put(k, getValue(k)));
        return variables;
    }

    private boolean isStringValue(String value) {
        return value != null && value.startsWith("\"");
    }

    private LineParser parseLine() {
        if (variables != null) {
            return this;
        }
        leftSide = new StringBuilder();
        rightSide = new StringBuilder();
        variables = new HashMap<>();
        pos = 0;
        boolean isVariablePending = false;
        while (currentChar() > 0) {
            while (currentChar() == ' ') {
                nextPlease();
            }
            if (currentChar() == ',') {
                if (variables.size() == 0) {
                    reportError("Uventet komma");
                }
                nextPlease();
                isVariablePending = true;
            }
            while (currentChar() == ' ') {
                nextPlease();
            }
            if (currentChar() > 0) {
                parseLeftSide();
                if (leftSide.length() == 0) {
                    reportError("Venstreside mangler til værdien");
                }
                if (variables.get(leftSide.toString()) != null) {
                    reportError("Dubleret navn");
                }
                parseRightSide();
                if (rightSide.length() == 0) {
                    reportError("Værdien mangler");
                }
                variables.put(leftSide.toString(), rightSide.toString());
                leftSide = new StringBuilder();
                rightSide = new StringBuilder();
            } else {
                if (isVariablePending) {
                    reportError("Uventet afslutning på linien");
                }
            }

        }
        return this;
    }


    private void parseLeftSide() {
        while (currentChar() == ' ') {
            nextPlease();
        }
        while (isLegalNameChar(currentChar())) {
            leftSide.append(currentChar());
            nextPlease();
        }
        while (currentChar() == ' ') {
            nextPlease();
        }

        if (currentChar() != '=') {
            reportError((currentChar() == 0 ? "" : "Tegnet " + currentChar() + " er ugyldigt her. ") + "Forventer = her");
        }
        nextPlease();
    }


    private void parseRightSide() {
        while (currentChar() == ' ') {
            nextPlease();
        }
        if (currentChar() == '\"') {
            parseString();
        } else {
            while (isLegalNameChar(currentChar())) {
                rightSide.append(currentChar());
                nextPlease();
            }
        }
    }

    private void parseString() {
        while (true) {
            rightSide.append(currentChar());
            nextPlease();
            if (currentChar() == '\"' && nextChar() == '\"') {
                nextPlease();
            } else {
                if (currentChar() == '\"') {
                    break;
                }
            }

            if (currentChar() == 0) {
                break;
            }
        }
        if (currentChar() == 0) {
            reportError("Uventet afslutning på tekst. Der mangler en \"");
        }
        nextPlease();

    }

    private void nextPlease() {
        pos++;
    }

    private char currentChar() {
        if (pos >= line.length()) {
            return 0;
        }
        return line.charAt(pos);
    }

    private char nextChar() {
        if (pos + 1 >= line.length()) {
            return 0;
        }
        return line.charAt(pos + 1);
    }


    private String fill(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private boolean isLegalNameChar(char c) {
        final String legalNameChars = "abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMOPQRSTUVWXYZÆØÅ1234567890_";
        return legalNameChars.contains(String.valueOf(c));
    }

    private void reportError(String message) {
        throw new IllegalArgumentException(message + "\n" +
                line + "\n" +
                fill(pos) + "|" + "\n");

    }
}