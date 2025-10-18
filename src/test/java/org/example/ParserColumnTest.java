package org.example;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class ParserColumnTest {
    private Ast.Column getColumn(String sql) {
        Parser parser = new Parser(sql);
        Ast.Select ast = parser.parseSelect();
        assertEquals(1, ast.expressions.size());
        assertInstanceOf(Ast.Column.class, ast.expressions.getFirst());
        return  (Ast.Column) ast.expressions.getFirst();
    }

    @Test
    public void testSimpleColumn() {
        Ast.Column column = getColumn("SELECT a FROM b");
        assertEquals("a", column.node.node);
        assertNull(column.table);
    }

    @Test
    public void testColumnWithDb() {
        Ast.Column column = getColumn("SELECT table.node FROM b");
        assertEquals("table", column.table.node);
        assertEquals("node", column.node.node);
    }

    @Test
    public void TestTooManyDots() {
        Parser parser = new Parser("SELECT catalog.db.table.node.suffix");
        Ast.Select ast = parser.parseSelect();
        assertEquals(1, ast.expressions.size());
        Ast.Dot dot = (Ast.Dot) ast.expressions.getFirst();
        Ast.Column column = (Ast.Column) dot.left;
        assertEquals("catalog", column.catalog.node);
        assertEquals("db", column.db.node);
        assertEquals("table", column.table.node);
        assertEquals("node", column.node.node);
        Ast.Identifier suffix = (Ast.Identifier) dot.right;
        assertEquals("suffix", suffix.node);
    }
}
