//> Representing Code ast-printer
package com.craftinginterpreters.lox;
//> omit

import java.util.List;

/* Representing Code ast-printer < Statements and State omit
class AstPrinter implements Expr.Visitor<String> {
*/
//> Statements and State omit
class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

  String print(Expr expr) {
    return expr.accept(this);
  }
//> Statements and State omit

  String print(Stmt stmt) {
    return stmt.accept(this);
  }

  @Override
  public String visitBlockStmt(Stmt.Block stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(block ");
    for (Stmt statement : stmt.statements) {
      builder.append(statement.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }

  // Ch 12 Q.1: Print both instance methods and class methods
  @Override
  public String visitClassStmt(Stmt.Class stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(class ").append(stmt.name.lexeme);

    if (stmt.superclass != null) {
      builder.append(" < ").append(print(stmt.superclass));
    }
//< Inheritance omit

    for (Stmt.Function method : stmt.methods) {
      builder.append(" ").append(print(method));
    }

    // Ch 12 Q.1: Also print class (static) methods
    for (Stmt.Function method : stmt.classMethods) {
      builder.append(" class ").append(print(method));
    }

    builder.append(")");
    return builder.toString();
  }
//< Classes omit
//> Statements and State omit

  @Override
  public String visitExpressionStmt(Stmt.Expression stmt) {
    return parenthesize(";", stmt.expression);
  }
//< Statements and State omit
//> Functions omit

  // Ch 12 Q.2: params may be null for getter methods
  @Override
  public String visitFunctionStmt(Stmt.Function stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(fun ").append(stmt.name.lexeme).append("(");

    if (stmt.params != null) {
      for (Token param : stmt.params) {
        if (param != stmt.params.get(0)) builder.append(" ");
        builder.append(param.lexeme);
      }
    }

    builder.append(") ");
    for (Stmt body : stmt.body) {
      builder.append(body.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }
//< Functions omit
//> Control Flow omit

  @Override
  public String visitIfStmt(Stmt.If stmt) {
    if (stmt.elseBranch == null) {
      return parenthesize2("if", stmt.condition, stmt.thenBranch);
    }
    return parenthesize2("if-else", stmt.condition, stmt.thenBranch, stmt.elseBranch);
  }
//< Control Flow omit
//> Statements and State omit

  @Override
  public String visitPrintStmt(Stmt.Print stmt) {
    return parenthesize("print", stmt.expression);
  }
//< Statements and State omit
//> Functions omit

  @Override
  public String visitReturnStmt(Stmt.Return stmt) {
    if (stmt.value == null) return "(return)";
    return parenthesize("return", stmt.value);
  }
//< Functions omit
//> Statements and State omit

  @Override
  public String visitVarStmt(Stmt.Var stmt) {
    if (stmt.initializer == null) {
      return parenthesize2("var", stmt.name);
    }
    return parenthesize2("var", stmt.name, "=", stmt.initializer);
  }
//< Statements and State omit
//> Control Flow omit

  @Override
  public String visitWhileStmt(Stmt.While stmt) {
    return parenthesize2("while", stmt.condition, stmt.body);
  }

  // Ch 9 C.3: Break has no sub-expressions
  @Override
  public String visitBreakStmt(Stmt.Break stmt) {
    return "(break)";
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesize2("=", expr.name.lexeme, expr.value);
  }
//< Statements and State omit

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }
//> Functions omit

  @Override
  public String visitCallExpr(Expr.Call expr) {
    return parenthesize2("call", expr.callee, expr.arguments);
  }

  // Ch 6 Q.2: Ternary conditional expression
  @Override
  public String visitConditionalExpr(Expr.Conditional expr) {
    return parenthesize2("?:", expr.condition, expr.thenBranch, expr.elseBranch);
  }

  // Ch 10 Q.2: Anonymous function expression
  @Override
  public String visitFunctionExpr(Expr.Function expr) {
    StringBuilder builder = new StringBuilder();
    builder.append("(fun (");

    if (expr.params != null) {
      for (Token param : expr.params) {
        if (param != expr.params.get(0)) builder.append(" ");
        builder.append(param.lexeme);
      }
    }

    builder.append(") ");
    for (Stmt body : expr.body) {
      builder.append(body.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitGetExpr(Expr.Get expr) {
    return parenthesize2(".", expr.object, expr.name.lexeme);
  }
//< Classes omit

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }
//> Control Flow omit

  @Override
  public String visitLogicalExpr(Expr.Logical expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }
//< Control Flow omit
//> Classes omit

  @Override
  public String visitSetExpr(Expr.Set expr) {
    return parenthesize2("=", expr.object, expr.name.lexeme, expr.value);
  }
//< Classes omit
//> Inheritance omit

  @Override
  public String visitSuperExpr(Expr.Super expr) {
    return parenthesize2("super", expr.method);
  }
//< Inheritance omit
//> Classes omit

  @Override
  public String visitThisExpr(Expr.This expr) {
    return "this";
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }
//> Statements and State omit

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }
//< print-utilities
//> omit
  // Note: AstPrinting other types of syntax trees is not shown in the
  // book, but this is provided here as a reference for those reading
  // the full code.
  private String parenthesize2(String name, Object... parts) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    transform(builder, parts);
    builder.append(")");
    return builder.toString();
  }

  private void transform(StringBuilder builder, Object... parts) {
    for (Object part : parts) {
      builder.append(" ");
      if (part instanceof Expr) {
        builder.append(((Expr)part).accept(this));
//> Statements and State omit
      } else if (part instanceof Stmt) {
        builder.append(((Stmt) part).accept(this));
//< Statements and State omit
      } else if (part instanceof Token) {
        builder.append(((Token) part).lexeme);
      } else if (part instanceof List) {
        // Error that is originally in the book
        transform(builder, ((List) part).toArray());
      } else {
        builder.append(part);
      }
    }
  }
}