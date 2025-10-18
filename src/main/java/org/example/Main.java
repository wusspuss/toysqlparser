package org.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final Map<Integer, String> examples = new LinkedHashMap<>();

    static {
        examples.put(1, "SELECT a, b AS b_alias FROM table1;");
        examples.put(2, "SELECT * FROM A, B, C;");
        examples.put(3, "SELECT * FROM A INNER JOIN B ON A.id = B.a_id;");
        examples.put(4, "SELECT * FROM A LEFT JOIN B ON A.id = B.a_id;");
        examples.put(5, "SELECT * FROM A WHERE a = 1 AND b > 100;");
        examples.put(6, "SELECT * FROM (SELECT * FROM A) a_alias;");
        examples.put(7, "SELECT a, COUNT(*) FROM A GROUP BY a;");
        examples.put(8, "SELECT a FROM A ORDER BY a DESC, b ASC;");
        examples.put(9, "SELECT * FROM A LIMIT 10 OFFSET 5;");
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Toy SQL Parser Demo ===");
        System.out.println("Built-in examples:");
        examples.forEach((key, sql) -> System.out.println(key + ": " + sql));
        while (true) {
            System.out.println("Type the number of an example to parse it,");
            System.out.println("or just your SQL query and press Enter.");
            System.out.print("Your input: ");

            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting demo.");
                break;
            }

            String sql;

            try {
                int choice = Integer.parseInt(input);
                if (examples.containsKey(choice)) {
                    sql = examples.get(choice);
                    System.out.println("Parsing built-in example #" + choice + ": " + sql);
                } else {
                    System.out.println("No example for number " + choice + ". Parsing input as SQL.");
                    sql = input;
                    if (!sql.endsWith(";")) {
                        sql += ";";
                    }
                }
            } catch (NumberFormatException e) {
                // Not a number: treat input as custom SQL
                sql = input;
                if (!sql.endsWith(";")) {
                    sql += ";";
                }
                System.out.println("Parsing custom SQL query: " + sql);
            }

            Parser parser = new Parser(sql);
//            System.out.println("Tokens: " + parser.getTokens());

            try {
                Ast.Select ast = parser.parseSelect();
                System.out.println("Parsed AST:\n" + ast);
            } catch (ParseException e) {
                System.err.println("Parse error:\n" + e.getMessage());
                System.err.println(e.getErrorContext());
            }

            System.out.println();
        }

        scanner.close();
    }
}
