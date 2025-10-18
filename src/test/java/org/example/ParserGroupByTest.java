package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ParserGroupByTest {
    @Test
    public void testGroupSingleField() {
        Parser parser = new Parser("SELECT department, COUNT(*) FROM employees GROUP BY department;");
        Ast.Select ast = parser.parseSelect();
        assertEquals(1, ast.group.expressions.size());
        assertInstanceOf(Ast.Column.class, ast.group.expressions.getFirst());
        Ast.Column column = (Ast.Column) ast.group.expressions.getFirst();
        assertEquals("department", column.node.node);
    }

    @Test
    public void testGroupMultipleFields() {
        Parser parser = new Parser("SELECT department, job_title, COUNT(*) FROM employees GROUP BY department, job_title;");

        Ast.Select ast = parser.parseSelect();

        assertEquals(2, ast.group.expressions.size());

        assertInstanceOf(Ast.Column.class, ast.group.expressions.get(0));
        Ast.Column departmentColumn = (Ast.Column) ast.group.expressions.get(0);
        assertEquals("department", departmentColumn.node.node);

        assertInstanceOf(Ast.Column.class, ast.group.expressions.get(1));
        Ast.Column jobTitleColumn = (Ast.Column) ast.group.expressions.get(1);
        assertEquals("job_title", jobTitleColumn.node.node);
    }

    @Test
    public void testHaving() {
        Parser parser = new Parser("SELECT COUNT(*)" +
                "FROM orders " +
                "HAVING COUNT(*) > 10;");
        Ast.Select ast = parser.parseSelect();
        assertInstanceOf(Ast.GT.class, ast.having.node);
    }
    @Test
    public void testLimit() {
        Parser parser = new Parser("SELECT * FROM table LIMIT 10;");
        Ast.Select ast = parser.parseSelect();
        assertInstanceOf(Ast.NumericLiteral.class, ast.limit.node);
        Ast.NumericLiteral literal = (Ast.NumericLiteral) ast.limit.node;
        assertEquals(10, literal.value);
        assertNull(ast.offset);
    }

    @Test
    public void testLimitWithOffset() {
        Parser parser = new Parser("SELECT * FROM table LIMIT 10 OFFSET 5;");
        Ast.Select ast = parser.parseSelect();

        // Verify that the limit is parsed correctly
        assertInstanceOf(Ast.NumericLiteral.class, ast.limit.node);
        Ast.NumericLiteral limitLiteral = (Ast.NumericLiteral) ast.limit.node;
        assertEquals(10, limitLiteral.value);

        // Verify that the offset is parsed correctly
        assertInstanceOf(Ast.NumericLiteral.class, ast.offset.node);
        Ast.NumericLiteral offsetLiteral = (Ast.NumericLiteral) ast.offset.node;
        assertEquals(5, offsetLiteral.value);
    }

}
