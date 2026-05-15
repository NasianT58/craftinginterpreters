//> Strings object-c
#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"
//> Hash Tables object-include-table
#include "table.h"
//< Hash Tables object-include-table
#include "value.h"
#include "vm.h"
//> allocate-obj

#define ALLOCATE_OBJ(type, objectType) \
    (type*)allocateObject(sizeof(type), objectType)
//< allocate-obj
//> allocate-object

static Obj* allocateObject(size_t size, ObjType type) {
  Obj* object = (Obj*)reallocate(NULL, 0, size);
  object->type = type;
//> Garbage Collection init-is-marked
  object->isMarked = false;
//< Garbage Collection init-is-marked
//> add-to-list
  // Chapter 26 Question 3: Initialize refCount
  object->refCount = 0;
  object->next = vm.objects;
  vm.objects = object;
//< add-to-list
//> Garbage Collection debug-log-allocate

#ifdef DEBUG_LOG_GC
  printf("%p allocate %zu for %d\n", (void*)object, size, type);
#endif

//< Garbage Collection debug-log-allocate
  return object;
}
//< allocate-object
//> Methods and Initializers new-bound-method
ObjBoundMethod* newBoundMethod(Value receiver,
                               ObjClosure* method) {
  ObjBoundMethod* bound = ALLOCATE_OBJ(ObjBoundMethod,
                                       OBJ_BOUND_METHOD);
  bound->receiver = receiver;
  bound->method = method;
  return bound;
}
//< Methods and Initializers new-bound-method
//> Classes and Instances new-class
ObjClass* newClass(ObjString* name) {
  ObjClass* klass = ALLOCATE_OBJ(ObjClass, OBJ_CLASS);
  klass->name = name; // [klass]
  // Chapter 28 Question 1: zero initialize initializer
  klass->initializer = NIL_VAL;
  // Chapter 29 Question 3: Initialize new fields
  klass->superclass = NULL;
//> Methods and Initializers init-methods
  initTable(&klass->methods);
  // Chapter 29 Question 3: Initialize new fields
  initTable(&klass->ownMethods);
//< Methods and Initializers init-methods
  return klass;
}
//< Classes and Instances new-class
//> Closures new-closure
ObjClosure* newClosure(ObjFunction* function) {
//> allocate-upvalue-array
  ObjUpvalue** upvalues = ALLOCATE(ObjUpvalue*,
                                   function->upvalueCount);
  for (int i = 0; i < function->upvalueCount; i++) {
    upvalues[i] = NULL;
  }

//< allocate-upvalue-array
  ObjClosure* closure = ALLOCATE_OBJ(ObjClosure, OBJ_CLOSURE);
  closure->function = function;
//> init-upvalue-fields
  closure->upvalues = upvalues;
  closure->upvalueCount = function->upvalueCount;
  // Chapter 29 Question 3: Initialize owner
  closure->owner = NULL;
//< init-upvalue-fields
  return closure;
}
//< Closures new-closure
//> Calls and Functions new-function
ObjFunction* newFunction() {
  ObjFunction* function = ALLOCATE_OBJ(ObjFunction, OBJ_FUNCTION);
  function->arity = 0;
//> Closures init-upvalue-count
  function->upvalueCount = 0;
//< Closures init-upvalue-count
  function->name = NULL;
  initChunk(&function->chunk);
  return function;
}
//< Calls and Functions new-function
//> Classes and Instances new-instance
ObjInstance* newInstance(ObjClass* klass) {
  ObjInstance* instance = ALLOCATE_OBJ(ObjInstance, OBJ_INSTANCE);
  instance->klass = klass;
  initTable(&instance->fields);
  return instance;
}
//< Classes and Instances new-instance
//> Calls and Functions new-native
ObjNative* newNative(NativeFn function) {
  ObjNative* native = ALLOCATE_OBJ(ObjNative, OBJ_NATIVE);
  native->function = function;
  return native;
}
//< Calls and Functions new-native

/* Strings allocate-string < Hash Tables allocate-string
static ObjString* allocateString(char* chars, int length) {
*/
//> allocate-string
//> Hash Tables allocate-string
// Chapter 20 Question 1: update allocateString
static ObjString* allocateString(char* chars, int length,
                                 uint32_t hash) {
  ObjString* string = ALLOCATE_OBJ(ObjString, OBJ_STRING);
  string->length = length;
  string->chars = chars;
  string->hash = hash;

  push(OBJ_VAL(string));
  tableSet(&vm.strings, OBJ_VAL(string), NIL_VAL);
  pop();
  incRef((Obj*)string); // Chapter 26 Question 3: keep interned string alive

  return string;
}
//< allocate-string
//> Hash Tables hash-string
static uint32_t hashString(const char* key, int length) {
  uint32_t hash = 2166136261u;
  for (int i = 0; i < length; i++) {
    hash ^= (uint8_t)key[i];
    hash *= 16777619;
  }
  return hash;
}
//< Hash Tables hash-string
//> take-string
ObjString* takeString(char* chars, int length) {
/* Strings take-string < Hash Tables take-string-hash
  return allocateString(chars, length);
*/
//> Hash Tables take-string-hash
  uint32_t hash = hashString(chars, length);
//> take-string-intern
  ObjString* interned = tableFindString(&vm.strings, chars, length,
                                        hash);
  if (interned != NULL) {
    FREE_ARRAY(char, chars, length + 1);
    return interned;
  }

//< take-string-intern
  // Chapter 30 Question 2: use inline short strings
  if (length <= SHORT_STRING_MAX) {
    ObjString* shortString = (ObjString*)newShortString(chars, length, hash);
    FREE_ARRAY(char, chars, length + 1);
    return shortString;
  }

  return allocateString(chars, length, hash);
//< Hash Tables take-string-hash
}

/* Chapter 19 Question 1: Replace "takeString()" with "makeString()"
ObjString* makeString(int length) {
  ObjString* string = (ObjString*)allocateObject(
      sizeof(ObjString) + length + 1, OBJ_STRING);
  string->length = length;
  return string;
}

*/
//< take-string

// Chapter 30 Question 2: allocate short string object
ObjShortString* newShortString(const char* chars, int length, uint32_t hash) {
  ObjShortString* string = (ObjShortString*)allocateObject(
      sizeof(ObjShortString), OBJ_SHORT_STRING);
  memcpy(string->chars, chars, length);
  string->chars[length] = '\0';
  string->hash = hash;

  // Chapter 30 Question 2: intern short strings
  push(OBJ_VAL(string));
  tableSet(&vm.strings, OBJ_VAL(string), NIL_VAL);
  pop();
  incRef((Obj*)string);

  return string;
}

ObjString* copyString(const char* chars, int length) {
  uint32_t hash = hashString(chars, length);

  // Chapter 30 Question 2: check interned strings first
  ObjString* interned = tableFindString(&vm.strings, chars, length, hash);
  if (interned != NULL) return interned;

  // Chapter 30 Question 2: use inline short strings
  if (length <= SHORT_STRING_MAX) {
    return (ObjString*)newShortString(chars, length, hash);
  }

  char* heapChars = ALLOCATE(char, length + 1);
  memcpy(heapChars, chars, length);
  heapChars[length] = '\0';
  return allocateString(heapChars, length, hash);
}

/* Chapter 19 Question 1: Replace copyString() with this version:
  ObjString* copyString(const char* chars, int length) {
    ObjString* string = makeString(length);

    memcpy(string->chars, chars, length);
    string->chars[length] = '\0';

    return string;
  }
*/

/* Chapter 19 Question 2: Replace takeString() AND copyString() with:
  ObjString* makeString(bool ownsChars, char* chars, int length) {
    ObjString* string = ALLOCATE_OBJ(ObjString, OBJ_STRING);
    string->ownsChars = ownsChars;
    string->length = length;
    string->chars = chars;
    return string;
  }
*/

//> Closures new-upvalue
ObjUpvalue* newUpvalue(Value* slot) {
  ObjUpvalue* upvalue = ALLOCATE_OBJ(ObjUpvalue, OBJ_UPVALUE);
//> init-closed
  upvalue->closed = NIL_VAL;
//< init-closed
  upvalue->location = slot;
//> init-next
  upvalue->next = NULL;
//< init-next
  return upvalue;
}
//< Closures new-upvalue

// Chapter 30 Question 2 get object string chars
static const char* objectStringChars(ObjString* string) {
  if (string->obj.type == OBJ_SHORT_STRING) {
    return ((ObjShortString*)string)->chars;
  }

  return string->chars;
}

//> Calls and Functions print-function-helper
static void printFunction(ObjFunction* function) {
//> print-script
  if (function->name == NULL) {
    printf("<script>");
    return;
  }
//< print-script
  printf("<fn %s>", objectStringChars(function->name));
}
//< Calls and Functions print-function-helper
//> print-object
void printObject(Value value) {
  switch (OBJ_TYPE(value)) {
//> Methods and Initializers print-bound-method
    case OBJ_BOUND_METHOD:
      printFunction(AS_BOUND_METHOD(value)->method->function);
      break;
//< Methods and Initializers print-bound-method
//> Classes and Instances print-class
    case OBJ_CLASS:
      printf("%s", objectStringChars(AS_CLASS(value)->name));
      break;
//< Classes and Instances print-class
//> Closures print-closure
    case OBJ_CLOSURE:
      printFunction(AS_CLOSURE(value)->function);
      break;
//< Closures print-closure
//> Calls and Functions print-function
    case OBJ_FUNCTION:
      printFunction(AS_FUNCTION(value));
      break;
//< Calls and Functions print-function
//> Classes and Instances print-instance
    case OBJ_INSTANCE:
      printf("%s instance",
             objectStringChars(AS_INSTANCE(value)->klass->name));
      break;
//< Classes and Instances print-instance
//> Calls and Functions print-native
    case OBJ_NATIVE:
      printf("<native fn>");
      break;
//< Calls and Functions print-native
    case OBJ_STRING:
      printf("%s", AS_CSTRING(value));
      break;
      /* Chapter 19 Question 2: Update case OBJ_STRING: body to:
        // Was: printf("%s", AS_CSTRING(value));
        // Now: use length-bounded print since chars may not be null-terminated
        printf("%.*s", AS_STRING(value)->length, AS_CSTRING(value));
        break;
      */
    // Chapter 30 Question 2: print short string
    case OBJ_SHORT_STRING:
      printf("%s", AS_SHORT_STRING(value)->chars);
      break;
//> Closures print-upvalue
    case OBJ_UPVALUE:
      printf("upvalue");
      break;
//< Closures print-upvalue
  }
}
//< print-object