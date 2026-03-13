//> Classes lox-class
package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

/* Classes lox-class < Classes lox-class-callable
class LoxClass {
*/
//> lox-class-callable

// Chapter 12. Q.11 Changing Declaration of Lox
class LoxClass extends LoxInstance implements LoxCallable {
//< lox-class-callable
  final String name;
//> Inheritance lox-class-superclass-field
/*
  final LoxClass superclass;
*/
//< Inheritance lox-class-superclass-field
/* Classes lox-class < Classes lox-class-methods

  LoxClass(String name) {
    this.name = name;
  }
*/
//> lox-class-methods
  private final Map<String, LoxFunction> methods;

/* Classes lox-class-methods < Inheritance lox-class-constructor
  LoxClass(String name, Map<String, LoxFunction> methods) {
*/
//> Inheritance lox-class-constructor

/* Old Constructor
  LoxClass(String name, LoxClass superclass,
           Map<String, LoxFunction> methods) {
    this.superclass = superclass;
//< Inheritance lox-class-constructor
    this.name = name;
    this.methods = methods;
  } */

  // Chapter 12 Q.1: New Constructor
  // Classes themselves are instances
  LoxClass(LoxClass metaclass, String name,
        Map<String, LoxFunction> methods) {
    super(metaclass);
    this.name = name;
    this.methods = methods;
  }

//< lox-class-methods
//> lox-class-find-method
/*
  LoxFunction findMethod(String name) {
    if (methods.containsKey(name)) {
      return methods.get(name);
    }
*/

// Chapter 13 Q.2: Finds Method with Given Name amd sets up "inner" reference to next method
// down the inheritance chain
  LoxFunction findMethod(LoxInstance instance, String name) {
    LoxFunction method = null;
    LoxFunction inner = null;
    LoxClass klass = this;

    while (klass != null) {
      if (klass.methods.containsKey(name)) {
       inner = method;
        method = klass.methods.get(name);
      }

      klass = klass.superclass;
    }

    if (method != null) {
      return method.bind(instance, inner);
    }

    return null;
  }

/*
//> Inheritance find-method-recurse-superclass
    if (superclass != null) {
      return superclass.findMethod(name);
    }
*/

//< Inheritance find-method-recurse-superclass
    return null;
  }
//< lox-class-find-method

  @Override
  public String toString() {
    return name;
  }
//> lox-class-call-arity
  @Override
  public Object call(Interpreter interpreter,
                     List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this);
//> lox-class-call-initializer
    LoxFunction initializer = findMethod("init");
    if (initializer != null) {
      // initializer.bind(instance).call(interpreter, arguments);
      // Chapter 13 Q.2 Calls the Initializer
      initializer.call(interpreter, arguments);
    }

//< lox-class-call-initializer
    return instance;
  }

  @Override
  public int arity() {
/* Classes lox-class-call-arity < Classes lox-initializer-arity
    return 0;
*/
//> lox-initializer-arity
    LoxFunction initializer = findMethod("init");
    if (initializer == null) return 0;
    return initializer.arity();
//< lox-initializer-arity
  }
//< lox-class-call-arity
}
