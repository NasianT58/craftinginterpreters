//> Resolving and Binding resolver
package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.craftinginterpreters.lox.Expr.Variable;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
//> scopes-field

  // Old Field
  // private final Stack<Map<String, Boolean>> scopes = new Stack<>();

//< scopes-field
//> function-type-field
  private FunctionType currentFunction = FunctionType.NONE;
//< function-type-field

  // Chapter 11. Q.4: Tracks next available index for each nested scope
  private final Stack<Integer> scopeNextIndex = new Stack<>();


  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }
//> function-type
  private enum FunctionType {
    NONE,
/* Resolving and Binding function-type < Classes function-type-method
    FUNCTION
*/
//> Classes function-type-method
    FUNCTION,
//> function-type-initializer
    INITIALIZER,
//< function-type-initializer
    METHOD
//< Classes function-type-method
  }
//< function-type
//> Classes class-type

// Chapter 11. Q.3 Stack can track "variables" instead of just booleans
  private final Stack<Map<String, Variable>> scopes = new Stack<>();

// Chapter 11. Q.3
  private static class Variable {
    final Token name;
    VariableState state;
    // Chapter 11 Q.4: Add Index Field
    final int index;

    // Chapter 11. Q.4 Added Index for parameter
    private Variable(Token name, VariableState state, int index) {
      this.name = name;
      this.state = state;
      // Chapter 11 Q.4: Add Index Field
      this.index = index;
    }
  }

  private enum VariableState {
    DECLARED,
    DEFINED,
    READ
  }

  private enum ClassType {
    NONE,
/* Classes class-type < Inheritance class-type-subclass
    CLASS
 */
//> Inheritance class-type-subclass
    CLASS,
    SUBCLASS
//< Inheritance class-type-subclass
  }

  private ClassType currentClass = ClassType.NONE;

//< Classes class-type
//> resolve-statements
  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }
//< resolve-statements
//> visit-block-stmt
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }
//< visit-block-stmt
//> Classes resolver-visit-class
  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
//> set-current-class
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;

//< set-current-class
    declare(stmt.name);
    define(stmt.name);
//> Inheritance resolve-superclass

//> inherit-self
    if (stmt.superclass != null &&
        stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
      Lox.error(stmt.superclass.name,
          "A class can't inherit from itself.");
    }

//< inherit-self
    if (stmt.superclass != null) {
//> set-current-subclass
      currentClass = ClassType.SUBCLASS;
//< set-current-subclass
      resolve(stmt.superclass);
    }
//< Inheritance resolve-superclass
//> Inheritance begin-super-scope

    if (stmt.superclass != null) {
      beginScope();

      //scopes.peek().put("super", true);

      // Chapter 11. Q.3 don't store just a boolean anymore, have to change to variable object
      // Chapter 11. Q.4 Added "-1" to match Index Argument, Does not Matter
      scopes.peek().put("this", new Variable(
        new Token(TokenType.THIS, "this", null, 0),
        VariableState.DEFINED, -1));

    }
//< Inheritance begin-super-scope
//> resolve-methods

//> resolver-begin-this-scope
    beginScope();
    // scopes.peek().put("this", true);

    // Chapter 11. Q.3 don't store just a boolean anymore, have to change to variable object
    // Chapter 11. Q.4 Added "-1" to match Index Argument, Does not Matter
    scopes.peek().put("this", new Variable(
      new Token(TokenType.THIS, "this", null, 0),
      VariableState.DEFINED, -1));
    // Chapter 13 Q.2: Allows resolver to recognize inner as a valid variable inside methods
    scopes.peek().put("inner", new Variable(
      new Token(TokenType.THIS, "this", null, 0),
      VariableState.DEFINED, -1));


//< resolver-begin-this-scope
    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
//> resolver-initializer-type
      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }
//< resolver-initializer-type
      resolveFunction(method, declaration); // [local]
    }
    
    // Chapter 12. Q.1: Resolving class methods
    // "classMethods" may not get recognized if AST Generator is not compiled
    for (Stmt.Function method : stmt.classMethods) {
      beginScope();
      scopes.peek().put("this", new Variable(
        new Token(TokenType.THIS, "this", null, 0),
        VariableState.DEFINED, -1));
      resolveFunction(method, FunctionType.METHOD);
      endScope();
    }

//> resolver-end-this-scope
    endScope();

//< resolver-end-this-scope
//< resolve-methods
//> Inheritance end-super-scope
    if (stmt.superclass != null) endScope();

//< Inheritance end-super-scope
//> restore-current-class
    currentClass = enclosingClass;
//< restore-current-class
    return null;
  }
//< Classes resolver-visit-class
//> visit-expression-stmt
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }
//< visit-expression-stmt
//> visit-function-stmt
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);

/* Resolving and Binding visit-function-stmt < Resolving and Binding pass-function-type
    resolveFunction(stmt);
*/
//> pass-function-type
    resolveFunction(stmt, FunctionType.FUNCTION);
//< pass-function-type
    return null;
  }
//< visit-function-stmt
//> visit-if-stmt
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }
//< visit-if-stmt
//> visit-print-stmt
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }
//< visit-print-stmt
//> visit-return-stmt
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
//> return-from-top
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }

//< return-from-top
    if (stmt.value != null) {
//> Classes return-in-initializer
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword,
            "Can't return a value from an initializer.");
      }

//< Classes return-in-initializer
      resolve(stmt.value);
    }

    return null;
  }
//< visit-return-stmt
//> visit-var-stmt
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }
//< visit-var-stmt
//> visit-while-stmt
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }
//< visit-while-stmt
//> visit-assign-expr
  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    
    // Original Call
    // Chapter 11. Q.4: Return to Original Call for Correct Parameters
    resolveLocal(expr, expr.name);

    /* Chapter 11. Q.3 Pass in False
    resolveLocal(expr, expr.name, false);
    */
    return null;
  }
//< visit-assign-expr
//> visit-binary-expr
  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
//< visit-binary-expr
//> visit-call-expr
  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }
//< visit-call-expr
//> Classes resolver-visit-get
  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }
//< Classes resolver-visit-get
//> visit-grouping-expr
  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }
//< visit-grouping-expr
//> visit-literal-expr
  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }
//< visit-literal-expr
//> visit-logical-expr
  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
//< visit-logical-expr
//> Classes resolver-visit-set
  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }
//< Classes resolver-visit-set
//> Inheritance resolve-super-expr
  @Override
  public Void visitSuperExpr(Expr.Super expr) {
//> invalid-super
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Can't use 'super' outside of a class.");
    } else if (currentClass != ClassType.SUBCLASS) {
      Lox.error(expr.keyword,
          "Can't use 'super' in a class with no superclass.");
    }

//< invalid-super
    
    // Chapter 11. Q.4 Return to Original Call For Correct Parameters
    resolveLocal(expr, expr.keyword);

    /* Chapter 11. Q.3, Changed Data Structure
    resolveLocal(expr, expr.keyword, false);
    */

    return null;
  }

//< Inheritance resolve-super-expr
//> Classes resolver-visit-this
  @Override
  public Void visitThisExpr(Expr.This expr) {
//> this-outside-of-class
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Can't use 'this' outside of a class.");
      return null;
    }

//< this-outside-of-class

    resolveLocal(expr, expr.keyword);

    /*  Chapter 11. Q.3
    resolveLocal(expr, expr.keyword, true);
    */
    return null;
  }

//< Classes resolver-visit-this
//> visit-unary-expr
  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }
//< visit-unary-expr
//> visit-variable-expr
/*
  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() &&
        scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name,
          "Can't read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    //resolveLocal(expr, expr.name, true);
    return null;
  }
*/
//< visit-variable-expr
//> resolve-stmt

// Chapter 11 Q.3
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() &&
        scopes.peek().containsKey(expr.name.lexeme) &&
        scopes.peek().get(expr.name.lexeme).state == VariableState.DECLARED) {
      Lox.error(expr.name,
          "Can't read local variable in its own initializer.");
    }

    // Chapter 11. Q.4 Change Call to Match Parameters
    resolveLocal(expr, expr.name);

    /* Chapter 11. Q.3 Pass in True
    resolveLocal(expr, expr.name, true);
    */

    return null;
  }
  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

//< resolve-stmt
//> resolve-expr
  private void resolve(Expr expr) {
    expr.accept(this);
  }
//< resolve-expr
//> resolve-function
/* Resolving and Binding resolve-function < Resolving and Binding set-current-function
  private void resolveFunction(Stmt.Function function) {
*/
//> set-current-function
/* Old Method
  private void resolveFunction(
      Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

//< set-current-function
    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
//> restore-current-function
    currentFunction = enclosingFunction;
//< restore-current-function
  }
//< resolve-function
*/

// Chapter 12 Q.2: Declares and Defines a function's scope by parameters
// If function is a getter, it has no parameters
  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    if (function.params != null) {
      for (Token param : function.params) {
        declare(param);
       define(param);
      }
    }
    resolve(function.body);
   endScope();
    currentFunction = enclosingFunction;
  }

//> begin-scope

/* Original Code
  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }
*/

/*  Chapter 11. Q.3 Replace beginScope to have Variables
private void beginScope() {
    scopes.push(new HashMap<String, Variable>());
  }
*/

  // Chapter 11. Q.4: Initialize Index for New Scope Addition
private void beginScope() {
    scopes.push(new HashMap<String, Variable>());
    scopeNextIndex.push(0);
  }

//< begin-scope

//> end-scope

/*
  private void endScope() {
    scopes.pop();
  }
*/

// Ch. 11 Q.3 Replace old endScope()
 private void endScope() {
  // Retrieve Current Scope
    Map<String, Variable> scope = scopes.pop();
  // Iterate through variables in local scope
    for (Map.Entry<String, Variable> entry : scope.entrySet()) {
      if (entry.getValue().state == VariableState.DEFINED) {
        // Static Error if declared/defined but never used
        Lox.error(entry.getValue().name, "Local variable is not used.");
      }
    }
  }


//< end-scope
//> declare

/* Original Code
  private void declare(Token name) {
    if (scopes.isEmpty()) return;

    Map<String, Boolean> scope = scopes.peek();
//> duplicate-variable
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name,
          "Already a variable with this name in this scope.");
    }

//< duplicate-variable
    scope.put(name.lexeme, false);
  }
*/

/* Chapter 11. Q.3: 
private void declare(Token name) {
    if (scopes.isEmpty()) return;

    Map<String, Variable> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name,
          "Already variable with this name in this scope.");
    }

    scope.put(name.lexeme, new Variable(name, VariableState.DECLARED));
  }
*/
  // Chapter 11. Q.4: Added and Edited Method
  private void declare(Token name) {
    if (scopes.isEmpty()) return;

    // Assign and increment the unique index for this scope
    int index = scopeNextIndex.peek();
    scopeNextIndex.set(scopeNextIndex.size() - 1, index + 1);

    // Store the index inside the Variable metadata
    scopes.peek().put(name.lexeme, new Variable(name, VariableState.DECLARED, index));
}

//< declare



//> define
/*
  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme, true);
  }
*/

// Ch 11. Q.3
private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().get(name.lexeme).state = VariableState.DEFINED;
  }
//< define


//> resolve-local

/* Original Code
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }
*/
  /* Chapter 11. Q.3 changed to mark USED when variable is accessed
    private void resolveLocal(Expr expr, Token name, boolean isRead) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);

        // Mark it used.
        if (isRead) {
          scopes.get(i).get(name.lexeme).state = VariableState.READ;
        }
        return;
      }
    }
  }
  */
  
  // Chapter 11. Q.4
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
        if (scopes.get(i).containsKey(name.lexeme)) {
            Variable variable = scopes.get(i).get(name.lexeme);

            // Pass both depth and unique index to the interpreter
            interpreter.resolve(expr, scopes.size() - 1 - i, variable.index);
            return;
        }
    }
}

//< resolve-local
}
