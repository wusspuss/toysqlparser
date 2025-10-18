package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class ParserJoinTest {
    public void testJoin(String sql, Ast.JoinKind expectedKind, Ast.JoinSide expectedSide) {
        Parser parser = new Parser(sql);
        Ast.Select ast = parser.parseSelect();
        Ast.Join join = ast.joins.getFirst();
        assertEquals(expectedKind, join.kind);
        assertEquals(expectedSide, join.side);
    }
    public void testJoin(Ast.Join join, Ast.JoinKind expectedKind, Ast.JoinSide expectedSide, String expectedAlias) {
        assertEquals(expectedKind, join.kind);
        assertEquals(expectedSide, join.side);
        if (expectedAlias == null)
            assertNull(join.node.alias);
        else
            assertEquals(expectedAlias, join.node.alias.node.node);
    }

    @Test
    public void testJoinSidesAndKinds() {
        testJoin(
                "SELECT * FROM table1 INNER JOIN table2",
                Ast.JoinKind.INNER,
                null
        );


        testJoin(
                "SELECT * FROM table1 LEFT JOIN table2",
                null,
                Ast.JoinSide.LEFT
        );


        testJoin(
                "SELECT * FROM table1  LEFT OUTER JOIN table2",
                Ast.JoinKind.OUTER,
                Ast.JoinSide.LEFT
        );

        testJoin(
                "SELECT * FROM table1 FULL OUTER JOIN table2",
                Ast.JoinKind.OUTER,
                Ast.JoinSide.FULL
        );

        testJoin(
                "SELECT * FROM table1 FULL JOIN table2",
                null,
                Ast.JoinSide.FULL
        );
    }

    @Test
    public void testMultipleJoin() {
        Parser parser = new Parser(
                """
                SELECT a FROM b
                LEFT JOIN c ON d=e
                RIGHT OUTER JOIN f f_alias ON g=h
                LEFT OUTER JOIN i AS i_alias
                FULL JOIN l ON m=n
                """);
        Ast.Select ast = parser.parseSelect();
        testJoin(ast.joins.get(0), null, Ast.JoinSide.LEFT, null);
        testJoin(ast.joins.get(1), Ast.JoinKind.OUTER, Ast.JoinSide.RIGHT, "f_alias");
        testJoin(ast.joins.get(2), Ast.JoinKind.OUTER, Ast.JoinSide.LEFT, "i_alias");
        testJoin(ast.joins.get(3), null, Ast.JoinSide.FULL, null);
    }
}