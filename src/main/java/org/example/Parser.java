package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Parser {
    private final TokenStream tokens;
    public TokenStream getTokens() {
        return tokens;
    }

    public Parser(String sqlText) {
        this.tokens = new TokenStream(sqlText);
    }

    public Ast.Select parseSelect() {
        tokens.expect(TokenType.SELECT);
        Ast.Select select=new Ast.Select();
        select.expressions = parseExpressionList();

        if (!tokens.match(TokenType.FROM))
            return select;
        // Parse first table
        Ast.Aliased fromTable = parseTable();
        select.from = new Ast.From(fromTable);

        // Parse implicit joins (comma-separated tables)
        while (tokens.match(TokenType.COMMA)) {
            Ast.Aliased joinTable = parseTable();
            select.joins.add(new Ast.Join(joinTable));  // default join kind & side = null
        }

        // Parse explicit joins with ON clauses and join kinds/sides
        select.joins.addAll(parseExplicitJoins());
        select.where=parseWhere();
        select.group=parseGroup();
        select.having = parseHaving();
        select.limit = parseLimit();
        select.offset=parseOffset();

        tokens.match(TokenType.SEMICOLON);

        return select;
    }

    private List<Ast.Join> parseExplicitJoins() {
        List<Ast.Join> explicitJoins = new ArrayList<>();
        loop:
        while (true) {
            Token token = tokens.peek();
            if (token == null) break;
            Ast.JoinKind joinKind = null;
            Ast.JoinSide joinSide = null;

            switch (token.type) {
                case LEFT, RIGHT, FULL -> {
                    joinSide = switch (token.type) {
                        case LEFT -> Ast.JoinSide.LEFT;
                        case RIGHT -> Ast.JoinSide.RIGHT;
                        case FULL -> Ast.JoinSide.FULL;
                        default -> throw new IllegalStateException("Unexpected token type: " + token.type);
                    };
                    tokens.consume();
                    token = tokens.peek();
                }
            }

            switch (token.type) {
                case INNER, OUTER -> {
                    joinKind = switch (token.type) {
                        case INNER -> Ast.JoinKind.INNER;
                        case OUTER -> Ast.JoinKind.OUTER;
                        default -> throw new IllegalStateException("Unexpected token type: " + token.type);
                    };
                    tokens.consume();
                    tokens.expect(TokenType.JOIN);
                }
                case JOIN -> tokens.consume();
                default -> {
                    break loop;
                }
            }

            Ast.Aliased joinTable = parseTable();

            Ast.Join join = new Ast.Join(joinTable, joinKind);
            join.side = joinSide;

            if (tokens.match(TokenType.ON)) {
                join.on = parseExpression();
            }

            explicitJoins.add(join);
        }
        return explicitJoins;
    }

    public Ast.Aliased parseTable() {
        Ast.Aliased table;
        Token token = tokens.peek();

        if (tokens.match(TokenType.L_PAREN)) {
            // Parse the inner SELECT statement as a subquery table
            Ast.Select subquery = parseSelect();
            tokens.expect(TokenType.R_PAREN);
            table = new Ast.Subquery(subquery);
        } else {
            Ast.Identifier firstId = parseIdentifier();
            if (firstId == null) {
                tokens.error("Expected table name", token);
                return null;
            }

            Ast.Identifier db = null;
            Ast.Identifier tableId;

            if (tokens.match(TokenType.DOT)) {
                // Expect the next identifier (table part). Use parseIdentifier to support quoted identifiers.
                Ast.Identifier secondId = parseIdentifier();
                if (secondId == null) {
                    tokens.error("Expected table name after '.'", tokens.peek());
                    return null;
                }

                db = firstId;
                tableId = secondId;
            } else {
                tableId = firstId;
            }

            table = new Ast.Table(tableId, db);
        }

        // Optional alias parsing (e.g., AS alias)
        boolean aliasIsExplicit =  tokens.match(TokenType.ALIAS);
        Ast.Identifier alias = parseIdentifier();
        if (aliasIsExplicit & alias==null)
            tokens.error("Expected identifier", tokens.previous());
        if (alias!=null)
            table.alias=new Ast.TableAlias(alias);

        return table;
    }


    private Ast.Identifier parseIdentifier() {
        Token t = tokens.peek();
        if (t == null) {
            return null;
        }
        return switch (t.type) {
            case TokenType.VAR -> {
                tokens.consume();
                yield new Ast.Identifier(t.text, false);
            }
            case TokenType.IDENTIFIER -> {
                tokens.consume();
                yield new Ast.Identifier(t.text, true);
            }
            default -> null;
        };
    }

    private Ast.Where parseWhere() {
        if (!tokens.match(TokenType.WHERE))
            return null;
        Ast.Expression expr = parseExpression();
        return new Ast.Where(expr);
    }

    private Ast.Group parseGroup() {
        if (!tokens.match(TokenType.GROUP_BY))
            return null;
        List<Ast.Expression> expressions = parseExpressionList();
        return new Ast.Group(expressions);
    }

    private Ast.Having parseHaving() {
        if (!tokens.match(TokenType.HAVING))
            return null;
        Ast.Expression node = parseExpression();
        return new Ast.Having(node);
    }

    private Ast.Limit parseLimit() {
        if (!tokens.match(TokenType.LIMIT))
            return null;
        Ast.Expression node = parseExpression();
        return new Ast.Limit(node);
    }
    private Ast.Offset parseOffset() {
        if (!tokens.match(TokenType.OFFSET))
            return null;
        Ast.Expression node = parseExpression();
        return new Ast.Offset(node);
    }

    public List<Ast.Expression> parseExpressionList() {
        List<Ast.Expression> expressions = new ArrayList<>();

        do {
            Ast.Expression expression =  parseExpression();
            boolean aliasIsExplicit =  tokens.match(TokenType.ALIAS);
            Ast.Identifier alias = parseIdentifier();
            if (aliasIsExplicit & alias==null)
                tokens.error("Expected identifier", tokens.previous());
            if (alias!=null)
                expression= new Ast.Alias(expression,alias);
            expressions.add(expression);
        } while (tokens.match(TokenType.COMMA));

        return expressions;
    }

    private static final Map<TokenType, Integer> PRECEDENCE_MAP = Map.of(
            TokenType.OR, 1,
            TokenType.AND, 2,
            TokenType.EQ, 3,
            TokenType.LT, 3,
            TokenType.GT, 3,
            TokenType.PLUS, 4,
            TokenType.DASH, 4,
            TokenType.SLASH, 5,
            TokenType.STAR, 5,
            TokenType.DOT, 5
    );

    private Ast.Expression parseExpression() {
        return parseExpression(0);
    }

    private Ast.Expression parseExpression(int minPrecedence) {
        Ast.Expression left = parsePrimary();

        while (true) {
            Token next = tokens.peek();
            if (next == null) break;

            Integer prec = PRECEDENCE_MAP.get(next.type);

            if (prec == null || prec < minPrecedence) {
                break;
            }

            tokens.consume();

            Ast.Expression right = parseExpression(prec + 1);

            switch (next.type) {
                case OR:
                    left = new Ast.Or(left, right);
                    break;
                case AND:
                    left = new Ast.And(left, right);
                    break;
                case EQ:
                    left = new Ast.EQ(left, right);
                    break;
                case LT:
                    left = new Ast.LT(left, right);
                    break;
                case GT:
                    left = new Ast.GT(left, right);
                    break;
                case PLUS:
                    left=new Ast.Add(left, right);
                    break;
                case DASH:
                    left = new Ast.Sub(left, right);
                    break;
                case STAR:
                    left = new Ast.Mul(left, right);
                    break;
                case SLASH:
                    left = new Ast.Div(left, right);
                    break;
                case DOT:
                    left = handleDotOperator(left, right);
                    break;

                default:
                    tokens.error("Unknown operator", next);
                    break;
            }
        }
        return left;
    }

    private Ast.Expression handleDotOperator(Ast.Expression left, Ast.Expression right) {
        // shift fields, and if all fields are filled, fall back to wrapping in Dots
        Ast.Column leftCol = (Ast.Column) left;
        Ast.Column rightCol = (Ast.Column) right;
        if (leftCol.catalog !=null) {
            return new Ast.Dot(leftCol, rightCol.node);
        }
        rightCol.table=leftCol.node;
        rightCol.db=leftCol.table;
        rightCol.catalog=leftCol.db;

        return rightCol;
    }

    private Ast.Expression parsePrimary() {
        Token token = tokens.peek();

        if (token == null) {
            tokens.error("Unexpected end of input", null);
            return null; // unreachable
        }

        if (token.type == TokenType.STAR) {
            tokens.consume();
            return new Ast.Star();
        }

        Ast.Identifier identifier=parseIdentifier();
        if (identifier!=null) {
            if (tokens.match(TokenType.L_PAREN)) {
                List<Ast.Expression> args = parseExpressionList();
                tokens.expect(TokenType.R_PAREN);
                return new Ast.FunctionCall(identifier.node, args);
            }
            return new Ast.Column(identifier);
        }

        // Numeric literals
        if (token.type == TokenType.NUMBER) {
            tokens.consume();
            return new Ast.NumericLiteral(token.text);
        }

        // String literals
        if (token.type == TokenType.STRING) {
            tokens.consume();
            return new Ast.StringLiteral(token.text);
        }

        if (token.type == TokenType.L_PAREN) {
            tokens.consume();
            // Look ahead to check if this is a subquery
            if (tokens.peek().type == TokenType.SELECT) {
                Ast.Select subquerySelect = parseSelect();
                tokens.expect(TokenType.R_PAREN);
                return new Ast.Subquery(subquerySelect);
            }
            Ast.Expression expr = parseExpression(0);
            tokens.expect(TokenType.R_PAREN);
            return expr;
        }

        tokens.error("Expected expression", token);
        return null;  // unreachable
    }
}
