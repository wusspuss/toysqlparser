package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ParserWhereTest {
    private Ast.Expression getWhereExpression(String sql) {
        Parser parser = new Parser(sql);
        Ast.Select ast = parser.parseSelect();
        if (ast.where == null) {
            throw new IllegalArgumentException("No WHERE clause in SQL: " + sql);
        }
        return ast.where.node;
    }

    @Test
    public void testSimpleComparison() {
        Ast.Expression expr = getWhereExpression("SELECT a FROM b WHERE c = d");
        assertInstanceOf(Ast.EQ.class, expr);
        Ast.EQ eq = (Ast.EQ) expr;
        assertEquals("c", ((Ast.Column) eq.left).node.node);
        assertEquals("d", ((Ast.Column) eq.right).node.node);
    }

    @Test
    public void testAndOperator() {
        Ast.Expression expr = getWhereExpression("SELECT a FROM b WHERE c < d AND e > f");
        assertInstanceOf(Ast.And.class, expr);
        Ast.And andExpr = (Ast.And) expr;
        assertInstanceOf(Ast.LT.class, andExpr.left);
        assertInstanceOf(Ast.GT.class, andExpr.right);
    }

    @Test
    public void testOrAndPrecedence() {
        Ast.Expression expr = getWhereExpression("SELECT a FROM b WHERE c < d AND e > f OR g = h");
        assertInstanceOf(Ast.Or.class, expr);
        Ast.Or orExpr = (Ast.Or) expr;
        assertInstanceOf(Ast.And.class, orExpr.left);
        assertInstanceOf(Ast.EQ.class, orExpr.right);
    }

    @Test
    public void testParenthesesPrecedence() {
        Ast.Expression expr = getWhereExpression("SELECT a FROM b WHERE (c < d OR e > f) AND g = h");
        assertInstanceOf(Ast.And.class, expr);
        Ast.And andExpr = (Ast.And) expr;
        assertInstanceOf(Ast.Or.class, andExpr.left);
        assertInstanceOf(Ast.EQ.class, andExpr.right);
    }
}
