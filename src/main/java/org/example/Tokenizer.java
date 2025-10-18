package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Tokenizer {
    private final String input;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private static final Map<String, TokenType> SINGLE_KEYWORDS = Map.ofEntries(
            Map.entry("SELECT", TokenType.SELECT),
            Map.entry("FROM", TokenType.FROM),
            Map.entry("WHERE", TokenType.WHERE),
            Map.entry("JOIN", TokenType.JOIN),
            Map.entry("ON", TokenType.ON),
            Map.entry("AS", TokenType.ALIAS),
            Map.entry("LEFT", TokenType.LEFT),
            Map.entry("RIGHT", TokenType.RIGHT),
            Map.entry("FULL", TokenType.FULL),
            Map.entry("HAVING", TokenType.HAVING),
            Map.entry("GROUP BY", TokenType.GROUP_BY), // multi-word
            Map.entry("AND", TokenType.AND),
            Map.entry("OR", TokenType.OR),
            Map.entry("LIMIT", TokenType.LIMIT),
            Map.entry("OFFSET", TokenType.OFFSET),
            Map.entry("INNER", TokenType.INNER),
            Map.entry("OUTER", TokenType.OUTER)
    );
    public Tokenizer(String input) {
        this.input = input;
    }

    private char peek() {
        return peek(0);
    }

    private char peek(int offset) {
        int index = pos + offset;
        return index < input.length() ? input.charAt(index) : '\0';
    }

    private char advance() {
        char c = peek();
        pos++;
        col++;
        if (c == '\n') {
            line++;
            col = 1;
        }
        return c;
    }

    private boolean match(char expected) {
        if (peek() == expected) {
            advance();
            return true;
        }
        return false;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char c = peek();
            if (peek() == '-' && peek(1) == '-') {
                skipSingleLineComment();
                continue;
            }
            if (peek() == '/' && peek(1) == '*') {
                skipBlockComment();
                continue;
            }
            if (Character.isWhitespace(c)) {
                consumeWhitespace();
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                tokens.add(readWord());
            } else if (Character.isDigit(c)) {
                tokens.add(readNumber());
            } else {
                tokens.add(readSymbol());
            }
        }
        return tokens;
    }
    private void skipSingleLineComment() {
        // Skip '--'
        advance(); // skip '-'
        advance(); // skip '-'
        while (pos < input.length() && peek() != '\n') {
            advance();
        }
        // Consume the newline character if present
        if (peek() == '\n') {
            advance();
        }
    }

    private void skipBlockComment() {
        // Skip '/*'
        advance(); // skip '/'
        advance(); // skip '*'
        while (pos < input.length()) {
            if (peek() == '*' && peek(1) == '/') {
                advance(); // '*'
                advance(); // '/'
                break;
            } else {
                advance();
            }
        }
    }

    private void consumeWhitespace() {
        while (pos < input.length() && Character.isWhitespace(peek())) {
            advance();
        }
    }

    private Token readWord() {
        int start = pos, startLine = line;
        String word = readSingleWord();

        if ((word.equalsIgnoreCase("GROUP") || word.equalsIgnoreCase("ORDER")) && peekNextWord().equalsIgnoreCase("BY")) {
            consumeWhitespace();
            readSingleWord();
            String combinedText = input.substring(start, pos);
            TokenType type = word.equalsIgnoreCase("GROUP") ? TokenType.GROUP_BY : TokenType.ORDER_BY;
            return new Token(type, combinedText, start, pos - 1, startLine, col - 1);
        }

        TokenType type = SINGLE_KEYWORDS.getOrDefault(word.toUpperCase(), TokenType.VAR);
        return new Token(type, word, start, pos - 1, startLine, col - 1);
    }

    private String readSingleWord() {
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(advance());
        }
        return sb.toString();
    }

    private String peekNextWord() {
        int tempPos = pos;
        int len = input.length();

        // Skip whitespace
        while (tempPos < len && Character.isWhitespace(input.charAt(tempPos))) {
            char c = input.charAt(tempPos);
            tempPos++;
        }

        // Collect word characters
        StringBuilder sb = new StringBuilder();
        while (tempPos < len) {
            char c = input.charAt(tempPos);
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
                tempPos++;
            } else {
                break;
            }
        }

        return sb.toString();
    }


    private Token readNumber() {
        int start = pos, startCol = col, startLine = line;
        while (Character.isDigit(peek())) advance();
        return new Token(TokenType.NUMBER, input.substring(start, pos),
                start, pos - 1, startLine, col - 1);
    }

    private Token readSymbol() {
        char c = advance();
        int start = pos - 1, tokenLine = line, tokenCol = col - 1;
        TokenType type;
        String text = String.valueOf(c);

        switch (c) {
            case '.': type = TokenType.DOT; break;
            case ',': type = TokenType.COMMA; break;
            case '(': type = TokenType.L_PAREN; break;
            case ')': type = TokenType.R_PAREN; break;
            case '+': type = TokenType.PLUS; break;
            case '-': type = TokenType.DASH; break;
            case '/': type = TokenType.SLASH; break;
            case '=': type = TokenType.EQ; break;
            case '>': type = TokenType.GT; break;
            case '<': type = TokenType.LT; break;
            case '*': type = TokenType.STAR; break;
            case ';': type = TokenType.SEMICOLON; break;
            case '"':
            case '\'': // STRING
                int startLine = line, startCol = col;    // position of first char inside string
                char quote = input.charAt(pos - 1);
                TokenType resultType = (quote == '\'') ? TokenType.STRING : TokenType.IDENTIFIER;
                StringBuilder sb = new StringBuilder();
                while (peek() != '\0') {
                    if (peek() == quote) {
                        advance(); // consume '
                        if (peek() == quote)  {
                            // escaped single quote '' -> append one '
                            sb.append(quote);
                            advance(); // consume second '
                            continue;
                        } else {
                            // closing quote found
                            String value = sb.toString();
                            return new Token(resultType, value, start, pos - 1, tokenLine, tokenCol);
                        }
                    } else {
                        sb.append(advance());
                    }
                }
                // EOF reached before closing quote — throw or return an error token
                throw new IllegalArgumentException("Unterminated string starting at line " + startLine + " col " + startCol);
            default:
                throw new IllegalArgumentException("Unrecognized character: " + c);
        }
        return new Token(type, text, start, start, tokenLine, tokenCol);
    }
}
