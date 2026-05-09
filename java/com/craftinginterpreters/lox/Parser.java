//> Parsing Expressions parser
package com.craftinginterpreters.lox;

//> Statements and State parser-imports
import java.util.ArrayList;
//< Statements and State parser-imports
//> Control Flow import-arrays
import java.util.Arrays;
//< Control Flow import-arrays
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
//> parse-error
  private static class ParseError extends RuntimeException {}

//< parse-error
  private final List<Token> tokens;
  private int current = 0;
  // Ch 9 C.3 Loop Depth for counting inner/outer
  private int loopDepth = 0;

  // Chapter 8 C.1 Fields
  private boolean allowExpression;
  private boolean foundExpression = false;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  // Chapter 8 C.1, allows parser to return statement or expr
  Object parseRepl() {
    allowExpression = true;
    List<Stmt> statements = new ArrayList<>();

    while (!isAtEnd()) {
      statements.add(declaration());

      if (foundExpression) {
        Stmt last = statements.get(statements.size() - 1);
        return ((Stmt.Expression) last).expression;
      }
      allowExpression = false;
    }
    return statements;
  }

//> Statements and State parse
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
//> parse-declaration
      statements.add(declaration());
//< parse-declaration
    }

    return statements; // [parse-error-handling]
  }
//< Statements and State parse
//> expression
  private Expr expression() {
//> Statements and State expression
    return comma();
//< Statements and State expression
  }

// CS 4080 Ch 6. Q. 1
private Expr comma() {
  // Ch 6 Q.2: comma delegates to assignment (which delegates to conditional)
  // so ternary binds tighter than comma
  Expr expr = assignment();
  while (match(COMMA)) {
    Token operator = previous();
    Expr right = assignment();
    expr = new Expr.Binary(expr, operator, right);
  }
  return expr;
}

//< expression
//> Statements and State declaration


// Chapter 10. Q.2: "fun" followed by identifier is a named function declaration
// "fun" NOT followed by identifier falls through to expressionStatement where
// primary() picks it up as an anonymous function expression
private Stmt declaration() {
    try {
//> Classes match-class
      if (match(CLASS)) return classDeclaration();
//< Classes match-class

      if (check(FUN) && checkNext(IDENTIFIER)) {
        advance(); // consume "fun"
        return function("function");
      }

      if (match(VAR)) return varDeclaration();
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

//< Statements and State declaration
//> Classes parse-class-declaration
  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name.");
//> Inheritance parse-superclass

    Expr.Variable superclass = null;
    if (match(LESS)) {
      consume(IDENTIFIER, "Expect superclass name.");
      superclass = new Expr.Variable(previous());
    }

//< Inheritance parse-superclass

    // Chapter 12 Q.1: Separate lists for instance methods and class methods
    List<Stmt.Function> methods = new ArrayList<>();
    List<Stmt.Function> classMethods = new ArrayList<>();

    consume(LEFT_BRACE, "Expect '{' before class body.");

    // Chapter 12 Q.1: "class" keyword before method name -> static/class method
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      boolean isClassMethod = match(CLASS);
      (isClassMethod ? classMethods : methods).add(function("method"));
    }

    consume(RIGHT_BRACE, "Expect '}' after class body.");

//> Inheritance construct-class-ast

    // Chapter 12 Q.1: Pass classMethods as 4th argument
    return new Stmt.Class(name, superclass, methods, classMethods);

//< Inheritance construct-class-ast
  }
//< Classes parse-class-declaration
//> Statements and State parse-statement
  private Stmt statement() {
//> Control Flow match-for
    if (match(FOR)) return forStatement();
//< Control Flow match-for
//> Control Flow match-if
    if (match(IF)) return ifStatement();
//< Control Flow match-if
    if (match(PRINT)) return printStatement();
//> Functions match-return
    if (match(RETURN)) return returnStatement();
//< Functions match-return
//> Control Flow match-while
    if (match(WHILE)) return whileStatement();
//< Control Flow match-while
//> parse-block
    if (match(LEFT_BRACE)) return new Stmt.Block(block());
//< parse-block

    // Ch 9. C.3 Break Statement
    if (match(BREAK)) return breakStatement();

    return expressionStatement();
  }
//< Statements and State parse-statement

// Ch 9. C.3 Break statement parser
  private Stmt breakStatement() {
    if (loopDepth == 0) {
      error(previous(), "Must be inside a loop to use 'break'.");
    }
    consume(SEMICOLON, "Expect ';' after 'break'.");
    return new Stmt.Break();
  }

//> Control Flow for-statement
  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'for'.");

//> for-initializer
    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }
//< for-initializer
//> for-condition

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expect ';' after loop condition.");
//< for-condition
//> for-increment

    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");
//< for-increment
//> for-body
// Chapter 9 C. 3 Parsing for For Loop Changes
    try {
      loopDepth++;
      Stmt body = statement();

      if (increment != null) {
        body = new Stmt.Block(Arrays.asList(
            body,
            new Stmt.Expression(increment)));
      }

      if (condition == null) condition = new Expr.Literal(true);
      body = new Stmt.While(condition, body);

      if (initializer != null) {
        body = new Stmt.Block(Arrays.asList(initializer, body));
      }

      return body;
    } finally {
      loopDepth--;
    }
  }
//< Control Flow for-statement
//> Control Flow if-statement
  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition."); // [parens]

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }
//< Control Flow if-statement
//> Statements and State parse-print-statement
  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }
//< Statements and State parse-print-statement
//> Functions parse-return-statement
  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(SEMICOLON)) {
      value = expression();
    }

    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }
//< Functions parse-return-statement
//> Statements and State parse-var-declaration
  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }
//< Statements and State parse-var-declaration
//> Control Flow while-statement
  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");

    // Ch 9. C. 3 Change the way it is parsed for while
    try {
      loopDepth++;
      Stmt body = statement();
      return new Stmt.While(condition, body);
    } finally {
      loopDepth--;
    }
  }
//< Control Flow while-statement
//> Statements and State parse-expression-statement
// Ch 8 C.1, doesn't require ";" in REPL for statements
  private Stmt expressionStatement() {
    Expr expr = expression();

    if (allowExpression && isAtEnd()) {
      foundExpression = true;
    } else {
      consume(SEMICOLON, "Expect ';' after expression.");
    }
    return new Stmt.Expression(expr);
  }

// Chapter 10 Q.2: Parses body of an anonymous function expression
// Returns Expr.Function (no name token). Example: fun(x, y) { return x + y; }
  private Expr.Function functionBody(String kind) {
    consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }
        parameters.add(consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }
    consume(RIGHT_PAREN, "Expect ')' after parameters.");
    consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();
    return new Expr.Function(parameters, body);
  }

  // Chapter 10. Q.2: Handling Named vs. Anonymous Functions
  private boolean checkNext(TokenType tokenType) {
    if (isAtEnd()) return false;
    if (tokens.get(current + 1).type == EOF) return false;
    return tokens.get(current + 1).type == tokenType;
  }

// Chapter 12 Q.2: Parses a Function/Method; allows omitting parameter list for getters
// Chapter 10. Q.2: Helper method, which was edited with old one
  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

    List<Token> parameters = null;

    // Allow omitting the parameter list entirely for method getters.
    if (!kind.equals("method") || check(LEFT_PAREN)) {
      consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
      parameters = new ArrayList<>();
      if (!check(RIGHT_PAREN)) {
        do {
          if (parameters.size() >= 255) {
            error(peek(), "Can't have more than 255 parameters.");
          }
          parameters.add(consume(IDENTIFIER, "Expect parameter name."));
        } while (match(COMMA));
      }
      consume(RIGHT_PAREN, "Expect ')' after parameters.");
    }

    consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }

//< Statements and State parse-expression-statement
//> Statements and State block
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }
//< Statements and State block
//> Statements and State parse-assignment
  private Expr assignment() {
//> Control Flow or-in-assignment
    // Ch 6 Q.2: assignment delegates to conditional (ternary) before checking '='
    Expr expr = conditional();
//< Control Flow or-in-assignment

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
//> Classes assign-set
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get)expr;
        return new Expr.Set(get.object, get.name, value);
//< Classes assign-set
      }

      error(equals, "Invalid assignment target."); // [no-throw]
    }

    return expr;
  }
//< Statements and State parse-assignment

  // Ch 6 Q.2: Ternary conditional operator, right-associative
  // Grammar: conditional -> or ( "?" expression ":" conditional )?
  private Expr conditional() {
    Expr expr = or();

    if (match(QUESTION)) {
      Expr thenBranch = expression(); // then expression
      consume(COLON, "Expect ':' after then branch of ternary expression.");
      Expr elseBranch = conditional(); // else-expression, right-associative
      expr = new Expr.Conditional(expr, thenBranch, elseBranch);
    }

    return expr;
  }

//> Control Flow or
  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }
//< Control Flow or
//> Control Flow and
  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }
//< Control Flow and
//> equality
  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< equality
//> comparison
  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< comparison
//> term
  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< term
//> factor
  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< factor
//> unary
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

//> Functions unary-call
    return call();
//< Functions unary-call
  }
//< unary
//> Functions finish-call
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
//> check-max-arity
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
//< check-max-arity
        // change to "equality" instead of "expression" for binding
        arguments.add(equality());
      } while (match(COMMA));
    }

    Token paren = consume(RIGHT_PAREN,
                          "Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }
//< Functions finish-call
//> Functions call
  private Expr call() {
    Expr expr = primary();

    while (true) { // [while-true]
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
//> Classes parse-property
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER,
            "Expect property name after '.'.");
        expr = new Expr.Get(expr, name);
//< Classes parse-property
      } else {
        break;
      }
    }

    return expr;
  }
//< Functions call
//> primary
  private Expr primary() { // parsing primary expressions
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    // Chapter 10. Q.2: "fun" NOT followed by identifier is an anonymous function
    if (check(FUN) && !checkNext(IDENTIFIER)) {
      advance(); // consume "fun"
      return functionBody("function");
    }

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    // Parsing group expressions
    if (match(LEFT_PAREN)) {
      // Recursively parsing inner expression
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    // Error productions for leading binary operators
    if (match(BANG_EQUAL, EQUAL_EQUAL)) {
      error(previous(), "Missing left-hand operand.");
      // Parses right hand side even with error so parser can continue
      equality();
      return null;
    }

    if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      error(previous(), "Missing left-hand operand");
      // Consumes the right hand side
      comparison();
      return null;
    }

    if (match(PLUS)) {
      error(previous(), "Missing left-hand operand.");
      // Consume right hand side
      term();
      return null;
    }

    if (match(SLASH, STAR)) {
      error(previous(), "Missing left-hand operand");
      // Consume right hand side
      factor();
      return null;
    }
//> Inheritance parse-super

    if (match(SUPER)) {
      Token keyword = previous();
      consume(DOT, "Expect '.' after 'super'.");
      Token method = consume(IDENTIFIER,
          "Expect superclass method name.");
      return new Expr.Super(keyword, method);
    }
//< Inheritance parse-super
//> Classes parse-this

    if (match(THIS)) return new Expr.This(previous());
//< Classes parse-this
//> Statements and State parse-identifier

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
//< Statements and State parse-identifier

//> primary-error

    throw error(peek(), "Expect expression.");
//< primary-error
  }
//< primary
//> match
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }
//< match
//> consume
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }
//< consume
//> check
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }
//< check
//> advance
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }
//< advance
//> utils
  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }
//< utils
//> error
  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }
//< error
//> synchronize
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
        default:
          break;
      }

      advance();
    }
  }
//< synchronize
}