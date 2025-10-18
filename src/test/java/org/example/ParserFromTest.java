package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ParserFromTest {
    private Ast.From getFromClause(String sql) {
        Parser parser = new Parser(sql);
        Ast.Select ast = parser.parseSelect();
        if (ast.from == null) {
            throw new IllegalArgumentException("No FROM clause in SQL: " + sql);
        }
        return ast.from;
    }
    @Test
    public void testBareSelect() {
        Parser parser = new Parser("SELECT 1");
        Ast.Select ast = parser.parseSelect();
        assertNull(ast.from);
    }
    @Test
    public void testPlainTable() {
        Ast.From f = getFromClause("SELECT * FROM a");
        assertNotNull(f.node);
        assertInstanceOf(Ast.Table.class, f.node);
        Ast.Table t = (Ast.Table) f.node;
        assertNotNull(t.node);
        assertEquals("a", t.node.node);
        assertNull(t.alias);
    }

    @Test
    public void testAliasedTableImplicit() {
        Ast.From f = getFromClause("SELECT * FROM a b");
        assertNotNull(f.node);
        assertInstanceOf(Ast.Table.class, f.node);
        Ast.Table t = (Ast.Table) f.node;
        assertEquals("a", t.node.node);
        assertEquals("b", t.alias.node.node);       // TableAlias.node should hold alias identifier
    }

    @Test
    public void testAliasedTableExplicit() {
        Ast.From f = getFromClause("SELECT * FROM a AS b");
        assertNotNull(f.node);
        assertInstanceOf(Ast.Table.class, f.node);
        Ast.Table t = (Ast.Table) f.node;
        assertEquals("a", t.node.node);
        assertEquals("b", t.alias.node.node);
    }

    @Test
    public void testSubqueryWithAliasImplicit() {
        Ast.From f = getFromClause("SELECT * FROM (SELECT x FROM t) q");
        assertInstanceOf(Ast.Subquery.class, f.node);
        Ast.Subquery s = (Ast.Subquery) f.node;
        assertNotNull(s.node);
        assertEquals("q", s.alias.node.node);
    }

    @Test
    public void testSubqueryWithAliasExplicit() {
        Ast.From f = getFromClause("SELECT * FROM (SELECT x FROM t) AS q");
        assertInstanceOf(Ast.Subquery.class, f.node);
        Ast.Subquery s = (Ast.Subquery) f.node;
        assertNotNull(s.node);
        assertEquals("q", s.alias.node.node);
    }

    @Test
    public void testSubqueryWithoutAlias() {
        Parser parser = new Parser("SELECT * FROM (SELECT x FROM t)");
        Ast.Select ast = parser.parseSelect();
        Ast.From f = ast.from;
        assertInstanceOf(Ast.Subquery.class, f.node);
        Ast.Subquery s = (Ast.Subquery) f.node;
        assertNull(s.alias);
    }

    @Test
    public void testQualifiedIdentifier() {
        Ast.From f = getFromClause("SELECT * FROM db.tbl AS t");
        assertInstanceOf(Ast.Table.class, f.node);
        Ast.Table t = (Ast.Table) f.node;
        assertEquals("tbl", t.node.node);
        assertEquals("db", t.db.node);
        assertEquals("t", t.alias.node.node);
    }

    @Test
    public void testTableIdentifierWithEscapedQuotes() {
        Ast.From f = getFromClause("SELECT * FROM \"sch\"\"ema\".\"my\"\"tbl\" AS x");
        assertInstanceOf(Ast.Table.class, f.node);
        Ast.Table t = (Ast.Table) f.node;
        assertTrue(t.node.quoted);
        assertEquals("my\"tbl", t.node.node);
        assertTrue(t.db.quoted);
        assertEquals("sch\"ema", t.db.node);
    }

    @Test
    public void testImplicitJoin() {
        Parser parser = new Parser("SELECT * FROM a, b, (SELECT * FROM c)");
        Ast.Select ast = parser.parseSelect();
        Ast.Table t = (Ast.Table) ast.from.node;
        assertEquals("a", t.node.node);
        assertEquals(2, ast.joins.size());

        assertInstanceOf(Ast.Table.class, ast.joins.getFirst().node);
        Ast.Table bTable=(Ast.Table) ast.joins.getFirst().node;
        assertEquals("b", bTable.node.node);

        assertInstanceOf(Ast.Subquery.class, ast.joins.get(1).node);
    }
}
