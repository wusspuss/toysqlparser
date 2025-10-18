package org.example;

public class Token {

    public final TokenType type;
    public final int start;
    public final int end;
    public final int line;
    public final int col;
    public final String text;

    public Token(TokenType type, String text, int start, int end, int line, int col) {
        this.type = type;
        this.text = text;
        this.start = start;this.end = end;
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        return String.format("Token type: %s, text: %s, line: %d, col: %d, start: %d, end: %d",
                type, text, line, col, start, end);
    }
}
