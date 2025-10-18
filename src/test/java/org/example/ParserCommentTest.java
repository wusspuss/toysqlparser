package org.example;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ParserCommentTest {
    @Test
    void testSingleLineComment() {
        Parser parser = new Parser("""
                SELECT *
                FROM table -- comment ignored
                WHERE a=b""");
        Ast.Select ast = parser.parseSelect();
        Ast.Table table = (Ast.Table)ast.from.node;
        assertEquals("table", table.node.node);
        assertNotNull(ast.where);
    }

    @Test
    void testMultilineComment() {
        Parser parser = new Parser("""
        SELECT *
        FROM table /* comment
        ignored*/
        WHERE a=b""");
        Ast.Select ast = parser.parseSelect();
        Ast.Table table = (Ast.Table)ast.from.node;
        assertEquals("table", table.node.node);
        assertNotNull(ast.where);
    }

}
