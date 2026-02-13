/*
    Chapter 5 Question 3 Challenge Problem
 */

package com.craftinginterpreters.lox;

public class Main {
    public static void main(String[] args) {
        // (1 + 2) * (4 - 3)
        Expr expression =
            new Expr.Binary(
                new Expr.Binary(new Expr.Literal(1),
                                new Token(TokenType.PLUS, "+", null, 1),
                                new Expr.Literal(2)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(new Expr.Literal(4),
                                new Token(TokenType.MINUS, "-", null, 1),
                                new Expr.Literal(3))
            );

        RpnPrinter printer = new RpnPrinter();
        // should print "1 2 + 4 3 - *"
        System.out.println(printer.print(expression)); 
    }
}
