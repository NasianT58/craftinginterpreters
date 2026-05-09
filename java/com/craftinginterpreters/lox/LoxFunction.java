//> Functions lox-function
package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {

  /* Ch. 10 Q.2 Replace Declaration Field
  private final String name;
  private final Expr.Function declaration;
  */

  // Named function declaration (Stmt.Function path); null for anonymous functions
  private final Stmt.Function declaration;

  // Ch 10 Q.2: Anonymous function declaration (Expr.Function path); null for named functions
  private final Expr.Function anonDeclaration;

//> closure-field
  private final Environment closure;
  
//< closure-field
/* Functions lox-function < Functions closure-constructor
  LoxFunction(Stmt.Function declaration) {
*/
/* Functions closure-constructor < Classes is-initializer-field
  LoxFunction(Stmt.Function declaration, Environment closure) {
*/
//> Classes is-initializer-field
  private final boolean isInitializer;

  LoxFunction(Stmt.Function declaration, Environment closure,
              boolean isInitializer) {
    this.isInitializer = isInitializer;
//< Classes is-initializer-field
//> closure-constructor
    this.closure = closure;
//< closure-constructor
    this.declaration = declaration;
    this.anonDeclaration = null;
  }

  // Chapter 10. Q.2: Constructor for anonymous function expressions
  // Ch 10 Q.2 and Ch 12 Q.2 (getters) both use LoxFunction; two constructors handle both
  LoxFunction(Expr.Function declaration, Environment closure) {
    this.anonDeclaration = declaration;
    this.declaration = null;
    this.closure = closure;
    this.isInitializer = false;
  }

  // Chapter 12 Q.2: Added isGetter helper function
  public boolean isGetter() {
    return declaration != null && declaration.params == null;
  }

  // Helper methods to get params/body from whichever declaration is active
  private List<Token> params() {
    return declaration != null ? declaration.params : anonDeclaration.params;
  }

  private List<Stmt> body() {
    return declaration != null ? declaration.body : anonDeclaration.body;
  }

//> Classes bind-instance
// Chapter 13 Q.2: Added "LoxFunction inner" parameter
  LoxFunction bind(LoxInstance instance, LoxFunction inner) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    // Chapter 13 Q.2: Added bind "inner" to the next method down
    environment.define("inner", inner);
/* Classes bind-instance < Classes lox-function-bind-with-initializer
    return new LoxFunction(declaration, environment);
*/
//> lox-function-bind-with-initializer
    if (declaration != null) {
      return new LoxFunction(declaration, environment, isInitializer);
    } else {
      return new LoxFunction(anonDeclaration, environment);
    }
//< lox-function-bind-with-initializer
  }
//< Classes bind-instance
//> function-to-string

  @Override
  public String toString() {
    if (declaration != null) return "<fn " + declaration.name.lexeme + ">";
    return "<fn>"; // Ch 10 Q.2: anonymous functions have no name
  }

//< function-to-string
//> function-arity
  @Override
  public int arity() {
    List<Token> p = params();
    // Chapter 12 Q.2: getter methods have params == null, arity is 0
    if (p == null) return 0;
    return p.size();
  }
//< function-arity
//> function-call
  @Override
  public Object call(Interpreter interpreter,
                     List<Object> arguments) {
/* Functions function-call < Functions call-closure
    Environment environment = new Environment(interpreter.globals);
*/
//> call-closure
    Environment environment = new Environment(closure);
//< call-closure

    // Chapter 12 Q.2: Getter Functions have params == null --> no args are bound
    List<Token> p = params();
    if (p != null && arguments != null) {
      for (int i = 0; i < p.size(); i++) {
        environment.define(p.get(i).lexeme, arguments.get(i));
      }
    }

/* Functions function-call < Functions catch-return
    interpreter.executeBlock(declaration.body, environment);
*/
//> catch-return
    try {
      interpreter.executeBlock(body(), environment);
    } catch (Return returnValue) {
//> Classes early-return-this
      if (isInitializer) return closure.getAt(0, "this");

//< Classes early-return-this
      return returnValue.value;
    }
//< catch-return
//> Classes return-this

    if (isInitializer) return closure.getAt(0, "this");
//< Classes return-this
    return null;
  }
//< function-call
}