package org.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Ast {
    public enum JoinKind {
        INNER, OUTER
    }

    public enum JoinSide {
        LEFT, RIGHT, FULL
    }
    public static class Add extends BinaryOp {
        public Add(Expression left, Expression right) {super(left, right);}
    }

    public static class Alias extends Expression {
        @ToStringField
        Expression node;

        @ToStringField
        Expression alias;

        public Alias(Expression node, Expression alias) {
            this.node = node;
            this.alias=alias;
        }
    }

    abstract public static class Aliased extends Expression {
        @ToStringField
        public TableAlias alias;

        public Aliased() {}

    }

    public static class And extends BinaryOp {
        public And(Expression left, Expression right) { super(left, right); }
    }

    public static class FunctionCall extends Expression {
        @ToStringField
        String node;

        @ToStringField
        List<Expression> args;

        public FunctionCall(String node, List<Expression> args) {
            this.node = node;
            this.args = args;
        }
    }

    public static class Subquery extends Aliased {
        @ToStringField
        Select node;

        public Subquery(Select node) {
            super();
            this.node = node;
        }
    }

    public static class Limit extends Expression {
        @ToStringField
        Expression node;

        public Limit(Expression node) {
            this.node = node;
        }
    }

    public static class LT extends BinaryOp {
        public LT(Expression left, Expression right) {super(left, right);}
    }

    public static class Offset extends Expression {
        @ToStringField
        Expression node;

        public Offset(Expression node) {
            this.node = node;
        }
    }

    public static class Star extends Expression {

    }

    public static class NumericLiteral extends Expression {
        @ToStringField
        final int value;
        public NumericLiteral(String text) {
            this.value = Integer.parseInt(text); // parse string to int
        }
    }

    public static class Or extends BinaryOp {
        public Or(Expression left, Expression right) { super(left, right); }
    }

    public static class TableAlias extends Expression {
        @ToStringField
        Identifier node;

        public TableAlias(Identifier node) {
            this.node = node;
        }
    }

    public static class StringLiteral extends Expression {
        @ToStringField
        final String value;
        public StringLiteral(String text) {
            this.value = text; // parse string to int
        }
    }

    public static class Sub extends BinaryOp {
        public Sub(Expression left, Expression right) { super(left, right); }
    }

    public static class Mul extends BinaryOp {
        public Mul(Expression left, Expression right) { super(left, right); }
    }

    public static class Join extends Expression {
        @ToStringField
        Aliased node;

        @ToStringField
        JoinKind kind;

        @ToStringField
        JoinSide side;

        @ToStringField
        Expression on;  // join condition, e.g. ON a = b

        public Join(Aliased node) {
            this.node = node;
        }

        public Join(Aliased node, JoinKind kind) {
            this.node = node;
            this.kind = kind;
        }
    }

    public static class Having extends Expression {
        @ToStringField
        Expression node;

        public Having(Expression node) {
            this.node = node;
        }
    }

    public static class GT extends BinaryOp {
        public GT(Expression left, Expression right) {super(left, right);}
    }

    public static class Group extends Expression {
        @ToStringField
        List<Expression> expressions = new ArrayList<>();

        public Group(List<Expression> expressions) {
            this.expressions = expressions;
        }
    }

    public static class Dot extends BinaryOp {
        public Dot(Expression left, Expression right) { super(left, right); }
    }

    public static class Div extends BinaryOp {
        public Div(Expression left, Expression right) { super(left, right); }
    }


    public abstract static class BinaryOp extends Expression {
        @ToStringField
        public Expression left;

        @ToStringField
        public Expression right;

        public BinaryOp(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }
    }

    public static class Column extends Expression {
        @ToStringField
        Identifier catalog=null;
        @ToStringField
        Identifier db=null;
        @ToStringField
        Identifier table=null;

        @ToStringField
        public Identifier node;


        public Column(Identifier node) {
            this.node = node;
        }

        public Column(Identifier node, Identifier table, Identifier db, Identifier catalog) {
            this.node=node;
            this.table=table;
            this.db=db;
            this.catalog=catalog;
        }
    }

    public static class EQ extends BinaryOp {
        public EQ(Expression left, Expression right) {
            super(left, right);
        }
    }

    public abstract static class Expression {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface ToStringField {
        }


        public String toStringIndented() {
            return toStringIndented(0,2);
        };

        public String toStringIndented(int nameIndent, int fieldsIndent) {
            boolean simple = isSimple(this);
            String separator = simple ? ", " : ",\n" + " ".repeat(fieldsIndent);
            String className = this.getClass().getSimpleName();
            StringBuilder sb = new StringBuilder();

            sb.append(" ".repeat(nameIndent));
            sb.append(className).append("(");

            if (!simple) {
                sb.append("\n").append(" ".repeat(fieldsIndent));
            }

            List<Field> fields = collectAnnotatedFields(this.getClass());

            // Filter out null fields
            fields.removeIf(field -> {
                try {
                    Object value = field.get(this);
                    if (value == null) return true;
                    if (value instanceof List) {
                        return ((List<?>) value).isEmpty();
                    }
                    return false;
                } catch (IllegalAccessException e) {
                    return true; // remove on access error, matching original behavior
                }
            });


            Object value;

            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                sb.append(field.getName()).append('=');
                try {
                    value = field.get(this);
                } catch (IllegalAccessException e) {
                    sb.append("ERROR");
                    continue;
                }

                if (value instanceof Expression) {
                    Expression expr = (Expression) value;
                    sb.append(expr.toStringIndented(0,fieldsIndent+2));
                } else if (value instanceof List<?>) {
                    List<?> list = (List<?>) value;
                    if (list.isEmpty()) {
                        sb.append("[]");
                    } else {
                        // List opening bracket inline
                        sb.append("[");
                        for (int j = 0; j < list.size(); j++) {
                            Object item = list.get(j);
                            if (item instanceof Expression) {
                                Expression exprItem = (Expression) item;
                                sb.append("\n").append(exprItem.toStringIndented(fieldsIndent+2, fieldsIndent+4));
                            } else {
                                sb.append(item);
                            }
                            if (j < list.size() - 1) sb.append(",");
                        }
                        sb.append("]");
                    }
                } else {
                    sb.append(value);
                }

                if (i < fields.size() - 1)
                    sb.append(separator);
            }
            sb.append(")");
            return sb.toString();
        }

        // Helper - determine if Expression is simple (few primitive fields)
        private boolean isSimple(Expression expr) {
            List<Field> fields = collectAnnotatedFields(expr.getClass());
            for (Field f : fields) {
                try {
                if (f.get(this) instanceof Expression || f.get(this) instanceof List) {
                    return false;
                }
                } catch (IllegalAccessException e) {
                    // Skip
                }
            }
            return true;
        }

        private List<Field> collectAnnotatedFields(Class<?> cls) {
            List<Field> fields = new ArrayList<>();
            Class<?> current = cls;
            while (current != null && Expression.class.isAssignableFrom(current)) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(ToStringField.class)) {
                        fields.add(field);
                    }
                }
                current = current.getSuperclass();
            }
            // sort by field name for a deterministic order
            fields.sort(Comparator.comparing(Field::getName));
            return fields;
        }

        @Override
        public String toString() {
            return toStringIndented();
        }
    }

    public static class From extends Expression {
        @ToStringField
        Aliased node;

        public From(Aliased node) {
            this.node = node;
        }
    }

    public static class Identifier extends Expression {
        @ToStringField
        public String node;

        @ToStringField
        boolean quoted;

        public Identifier(String node, boolean quoted) {
            this.node = node;
            this.quoted = quoted;
        }
    }

    public static class Select extends Expression {
        @ToStringField
        List<Expression> expressions = new ArrayList<>();

        @ToStringField
        public From from;

        @ToStringField
        List<Join> joins = new ArrayList<>();

        @ToStringField
        public Where where;

        @ToStringField
        public Group group=null;

        @ToStringField
        public Having having;

        @ToStringField
        public Limit limit;

        @ToStringField
        public Offset offset;

        public Select() {

        };
    }

    public static class Table extends Aliased {
        @ToStringField
        Identifier node;

        @ToStringField
        Identifier db;

        public Table(Identifier node, Identifier db) {
            super();
            this.node = node;
            this.db = db;
        }
    }

    public static class Where extends Expression {
        @ToStringField
        public Expression node;

        public Where(Expression node) {
            this.node = node;
        }
    }
}
