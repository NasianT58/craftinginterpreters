//> Functions lox-function
package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {

  /* Ch. 10 Q.2 Replace Declaration Field
  private final String name;
  private final Expr.Function declaration;
  */

  private final Stmt.Function declaration;

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
  }

  /* Chapter 10, Q. 2, Replace COnstructor
  LoxFunction(String name,
            Expr.Function declaration,
            Environment closure) {

  this.name = name;
  this.declaration = declaration;
  this.closure = closure;
} */

//> Classes bind-instance
  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
/* Classes bind-instance < Classes lox-function-bind-with-initializer
    return new LoxFunction(declaration, environment);
*/
//> lox-function-bind-with-initializer
    return new LoxFunction(declaration, environment,
                           isInitializer);
//< lox-function-bind-with-initializer
  }
//< Classes bind-instance
//> function-to-string



/*  Ch 10 Q.2 Update toString()
  @Override
  public String toString() {
    if (name == null) return "<fn>";
    return "<fn " + name + ">";
  }
*/

@Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }

//< function-to-string
//> function-arity
  @Override
  public int arity() {
    return declaration.params.size();
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
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme,
          arguments.get(i));
    }

/* Functions function-call < Functions catch-return
    interpreter.executeBlock(declaration.body, environment);
*/
//> catch-return
    try {
      interpreter.executeBlock(declaration.body, environment);
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
