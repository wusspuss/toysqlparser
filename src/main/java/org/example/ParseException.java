package org.example;

public class ParseException extends RuntimeException {
    private final Token token;
    private final String[] lines; // input SQL split by newline

    public ParseException(String message, Token token, String[] lines) {
        super(message);
        this.token = token;
        this.lines = lines;
    }

    public String getErrorContext() {
        int lineNum = token.line - 1;
        if (lineNum < 0 || lineNum >= lines.length) {
            return getMessage() + " (at unknown location)";
        }
        String line = lines[lineNum];
        StringBuilder sb = new StringBuilder();

        sb.append(line).append('\n');

        int colPos = token.col - token.text.length();
        for (int i = 0; i < colPos; i++) {
            sb.append(line.charAt(i) == '\t' ? '\t' : ' '); // keep tabs aligned
        }
        for (int i = 0; i < token.text.length(); i++) {
            sb.append(line.charAt(i) == '\t' ? '\t' : '^'); // keep tabs aligned
        }
        return sb.toString();
    }

    @Override
    public String getMessage() {
        if (token != null)
            return super.getMessage() + " at line " + token.line + ", col " + token.col;
        else
            return "No token?";
    }
}
