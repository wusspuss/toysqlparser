package org.example;

import java.util.List;
import java.util.stream.Collectors;

public class TokenStream {
    private final List<Token> tokens;
    private int pos = 0;
    private final String[] sqlLines;

    public TokenStream(String sqlText) {
        this.sqlLines = sqlText.split("\n", -1);
        Tokenizer tokenizer = new Tokenizer(sqlText);
        this.tokens = tokenizer.tokenize();
    }

    public Token peek() {
        if (pos >= tokens.size()) return null;
        return tokens.get(pos);
    }

    public Token consume() {
        if (pos >= tokens.size()) return null;
        return tokens.get(pos++);
    }

    public Token previous() {
        return tokens.get(pos-1);
    }

    public boolean match(TokenType type) {
        Token t = peek();
        if (t != null && t.type == type) {
            pos++;
            return true;
        }
        return false;
    }

    public Token expect(TokenType type) {
        Token t = consume();
        if (t == null || t.type != type) {
            error("Expected token " + type + " but got " + (t == null ? "EOF" : t.type), t);
        }
        return t;
    }

    public void error(String message, Token token) {
        throw new ParseException(message, token, sqlLines);
    }

    @Override
    public String toString() {
        return tokens == null ? "" :
                tokens.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining("\n"));
    }
}
