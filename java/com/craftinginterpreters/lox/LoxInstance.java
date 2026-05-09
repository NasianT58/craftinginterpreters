//> Classes lox-instance
package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  private LoxClass klass;
//> lox-instance-fields
  private final Map<String, Object> fields = new HashMap<>();
//< lox-instance-fields

  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

//> lox-instance-get-property
  Object get(Token name) {
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }

//> lox-instance-get-method

    // Chapter 13 Q.2: findMethod now takes LoxInstance, String
    // Chapter 13 Q.2: bind now takes LoxInstance, LoxFunction inner
    LoxFunction method = klass.findMethod(this, name.lexeme);

/* Old single-arg calls (base book):
    LoxFunction method = klass.findMethod(name.lexeme);
    if (method != null) return method.bind(this);
*/

    if (method != null) return method;  // findMethod already called bind internally

//< lox-instance-get-method
    throw new RuntimeError(name, // [hidden]
        "Undefined property '" + name.lexeme + "'.");
  }
//< lox-instance-get-property
//> lox-instance-set-property
  void set(Token name, Object value) {
    fields.put(name.lexeme, value);
  }
//< lox-instance-set-property
  @Override
  public String toString() {
    return klass.name + " instance";
  }
}