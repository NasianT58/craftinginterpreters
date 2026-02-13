/*
    Chapter 5 Question 3 Challenge Problem
*/
package com.craftinginterpreters.lox;

public class RpnPrinter implements Expr.Visitor<String> {
    // create new visitor class
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value.toString();
    }

    @Override 
    public String visitGroupingExpr(Expr.Grouping expr) {
        return expr.expression.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        String left = expr.left.accept(this);
        String right = expr.right.accept(this);
        return left + " " + right + " " + expr.operator.lexeme;
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return expr.right.accept(this) + " " + expr.operator.lexeme;
    }

    // Method Stubs
    // Not required, but have to implement for Expr Interface
    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return ""; 
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return ""; 
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return ""; 
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return ""; 
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return ""; 
    }

    @Override
    public String visitSuperExpr(Expr.Super expr) {
        return ""; 
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return "";
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return "";
    }
    
    
}
