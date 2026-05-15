//> A Virtual Machine vm-c
//> Types of Values include-stdarg
#include <stdarg.h>
//< Types of Values include-stdarg
//> vm-include-stdio
#include <stdio.h>
//> Strings vm-include-string
#include <string.h>
//< Strings vm-include-string
//> Calls and Functions vm-include-time
#include <time.h>
//< Calls and Functions vm-include-time

//< vm-include-stdio
#include "common.h"
//> Scanning on Demand vm-include-compiler
#include "compiler.h"
//< Scanning on Demand vm-include-compiler
//> vm-include-debug
#include "debug.h"
//< vm-include-debug
//> Strings vm-include-object-memory
#include "object.h"
#include "memory.h"
//< Strings vm-include-object-memory
#include "vm.h"

VM vm; // [one]

// Chapter 30 Question 2: get chars from either string type
static const char* getStringChars(ObjString* str) {
  if (str->obj.type == OBJ_SHORT_STRING) {
    return ((ObjShortString*)str)->chars;
  }
  return str->chars;
}

// Chapter 30 Question 2: get length from either string type
static int getStringLength(ObjString* str) {
  if (str->obj.type == OBJ_SHORT_STRING) {
    return (int)strlen(((ObjShortString*)str)->chars);
  }
  return str->length;
}

//> Calls and Functions clock-native
// Chapter 24 Question 2: Update clockNative
static bool clockNative(int argCount, Value* args) {
  // return NUMBER_VAL((double)clock() / CLOCKS_PER_SEC);
  args[-1] = NUMBER_VAL((double)clock() / CLOCKS_PER_SEC);
  return true;
}

// Chapter 24 Question 4: Add typeNative function
static bool typeNative(int argCount, Value* args) {
  if (argCount != 1) {
    args[-1] = OBJ_VAL(copyString("type() expects 1 argument.", 26));
    return false;
  }

  Value value = args[0];

  if (IS_NUMBER(value)) {
    args[-1] = OBJ_VAL(copyString("number", 6));
  } else if (IS_BOOL(value)) {
    args[-1] = OBJ_VAL(copyString("bool", 4));
  } else if (IS_NIL(value)) {
    args[-1] = OBJ_VAL(copyString("nil", 3));
  } else if (IS_STRING(value)) {
    args[-1] = OBJ_VAL(copyString("string", 6));
  } else if (IS_FUNCTION(value) || IS_CLOSURE(value)) {
    args[-1] = OBJ_VAL(copyString("function", 8));
  } else if (IS_NATIVE(value)) {
    args[-1] = OBJ_VAL(copyString("native", 6));
  } else {
    args[-1] = OBJ_VAL(copyString("unknown", 7));
  }
  return true;
}

//< Calls and Functions clock-native
//> reset-stack
static void resetStack() {
  vm.stackTop = vm.stack;
//> Calls and Functions reset-frame-count
  vm.frameCount = 0;
//< Calls and Functions reset-frame-count
//> Closures init-open-upvalues
  vm.openUpvalues = NULL;
//< Closures init-open-upvalues
}
//< reset-stack
//> Types of Values runtime-error


// Chapter 15 Question 3: Replace Old resetStack
/*
static void resetStack() {
  vm.stackCount = 0;
}
*/

// Chapter 25 Question 1: add getFrameFunction() helper
static inline ObjFunction* getFrameFunction(CallFrame* frame) {
  if (frame->function->type == OBJ_FUNCTION) {
    return (ObjFunction*)frame->function;
  } else {
    return ((ObjClosure*)frame->function)->function;
  }
}

static void runtimeError(const char* format, ...) {
  va_list args;
  va_start(args, format);
  vfprintf(stderr, format, args);
  va_end(args);
  fputs("\n", stderr);

/* Types of Values runtime-error < Calls and Functions runtime-error-temp
  size_t instruction = vm.ip - vm.chunk->code - 1;
  int line = vm.chunk->lines[instruction];
*/
/* Calls and Functions runtime-error-temp < Calls and Functions runtime-error-stack
  CallFrame* frame = &vm.frames[vm.frameCount - 1];
  size_t instruction = frame->ip - frame->function->chunk.code - 1;
  int line = frame->function->chunk.lines[instruction];
*/
/* Types of Values runtime-error < Calls and Functions runtime-error-stack
  fprintf(stderr, "[line %d] in script\n", line);
*/
//> Calls and Functions runtime-error-stack
  for (int i = vm.frameCount - 1; i >= 0; i--) {
    CallFrame* frame = &vm.frames[i];
/* Calls and Functions runtime-error-stack < Closures runtime-error-function
    ObjFunction* function = frame->function;
*/
//> Closures runtime-error-function
// Chapter 25 Question 1: Modify runtimeError()
    // ObjFunction* function = frame->closure->function;
    ObjFunction* function = getFrameFunction(frame);
//< Closures runtime-error-function
    size_t instruction = frame->ip - function->chunk.code - 1;
    fprintf(stderr, "[line %d] in ", // [minus]
        function->chunk.lines[instruction].line);;
    if (function->name == NULL) {
      fprintf(stderr, "script\n");
    } else {
      fprintf(stderr, "%s()\n", getStringChars(function->name));
    }
  }

//< Calls and Functions runtime-error-stack
  resetStack();
}
//< Types of Values runtime-error
//> Calls and Functions define-native
static void defineNative(const char* name, NativeFn function) {
  push(OBJ_VAL(copyString(name, (int)strlen(name))));
  push(OBJ_VAL(newNative(function)));
  tableSet(&vm.globals, OBJ_VAL(AS_STRING(vm.stackTop[-2])), vm.stackTop[-1]);
  pop();
  pop();
}
//< Calls and Functions define-native

// Chapter 27 Question 1: Add native function hasFieldNative()
static bool hasFieldNative(int argCount, Value* args) {
  if (argCount != 2 || !IS_INSTANCE(args[0]) || !IS_STRING(args[1])) {
    args[-1] = NIL_VAL;
    return true;
  }
  ObjInstance* instance = AS_INSTANCE(args[0]);
  Value dummy;
  // Chapter 27 Question 1: key is now Value, not ObjString*
  args[-1] = BOOL_VAL(tableGet(&instance->fields, args[1], &dummy));
  return true;
}

// Chapter 27 Question 2: Add native functions getFieldNative(), setFieldNative()
static bool getFieldNative(int argCount, Value* args) {
  if (argCount != 2 || !IS_INSTANCE(args[0]) || !IS_STRING(args[1])) {
    args[-1] = NIL_VAL;
    return true;
  }
  ObjInstance* instance = AS_INSTANCE(args[0]);
  Value value;
  // Chapter 27 Question 2: key is now Value not ObjString* from previous challenge problem
  tableGet(&instance->fields, args[1], &value);
  args[-1] = value;
  return true;
}

static bool setFieldNative(int argCount, Value* args) {
  if (argCount != 3 || !IS_INSTANCE(args[0]) || !IS_STRING(args[1])) {
    args[-1] = NIL_VAL;
    return true;
  }
  ObjInstance* instance = AS_INSTANCE(args[0]);
  // Chapter 27 Question 2: key is now Value not ObjString* from previous challenge problem
  tableSet(&instance->fields, args[1], args[2]);
  args[-1] = args[2];
  return true;
}

// Chapter 27 Question 3: add native function deleteFieldNative()
static bool deleteFieldNative(int argCount, Value* args) {
  if (argCount != 2 || !IS_INSTANCE(args[0]) || !IS_STRING(args[1])) {
    args[-1] = NIL_VAL;
    return true;
  }
  ObjInstance* instance = AS_INSTANCE(args[0]);
  // Chapter 27 Question 3: key is now Value not ObjString* from previous challenge problem
  tableDelete(&instance->fields, args[1]);
  args[-1] = NIL_VAL;
  return true;
}

void initVM() {
//> call-reset-stack
  resetStack();
//< call-reset-stack
//> Strings init-objects-root
  vm.objects = NULL;
//< Strings init-objects-root
//> Garbage Collection init-gc-fields
  vm.bytesAllocated = 0;
  vm.nextGC = 1024 * 1024;
//< Garbage Collection init-gc-fields
//> Garbage Collection init-gray-stack

  vm.grayCount = 0;
  vm.grayCapacity = 0;
  vm.grayStack = NULL;
//< Garbage Collection init-gray-stack
//> Global Variables init-globals

  initTable(&vm.globals);
//< Global Variables init-globals
//> Hash Tables init-strings
  initTable(&vm.strings);
//< Hash Tables init-strings
//> Methods and Initializers init-init-string

//> null-init-string
  vm.initString = NULL;
//< null-init-string
  vm.initString = copyString("init", 4);
//< Methods and Initializers init-init-string
//> Calls and Functions define-native-clock

  defineNative("clock", clockNative);
  // Chapter 24 Question 4: register typeNative
  defineNative("type", typeNative);
//< Calls and Functions define-native-clock
// Chapter 27 Quetion 1: Define native function
  defineNative("hasField", hasFieldNative);
// Chapter 27 Question 2: Define other Natives
  defineNative("getField", getFieldNative);
  defineNative("setField", setFieldNative);
// Chapter 27 Question 3: Define deleteField
  defineNative("deleteField", deleteFieldNative);
}

// Chapter 15 Challenge 3: Replace exisiting stack initialization
/*
void initVM() {
  vm.stack = NULL;
  vm.stackCapacity = 0;
  resetStack();
} */

// Chapter 15 Challenge 3: Changes to freeVM() (From TB)
/*
  void freeVM() {
    free(vm.stack);
  }
*/

void freeVM() {
//> Global Variables free-globals
  freeTable(&vm.globals);
//< Global Variables free-globals
//> Hash Tables free-strings
  freeTable(&vm.strings);
//< Hash Tables free-strings
//> Methods and Initializers clear-init-string
  vm.initString = NULL;
//< Methods and Initializers clear-init-string
//> Strings call-free-objects
  freeObjects();
//< Strings call-free-objects
}

//> push
void push(Value value) {
  // Chapter 26 Question 3: Pushes trigger a reference count increase
  if (IS_OBJ(value)) incRef(AS_OBJ(value));
  *vm.stackTop = value;
  vm.stackTop++;
}
//< push

// Chapter 15 Challenge 3: Replace Old push():
/*
void push(Value value) {
  if (vm.stackCapacity < vm.stackCount + 1) {
    int oldCapacity = vm.stackCapacity;
    vm.stackCapacity = GROW_CAPACITY(oldCapacity);
    vm.stack = GROW_ARRAY(Value, vm.stack,
                          oldCapacity, vm.stackCapacity);
  }

  vm.stack[vm.stackCount] = value;
  vm.stackCount++;
}
*/


//> pop
Value pop() {
  // Chapter 26 Question 3: Pops trigger a reference count decrease
  vm.stackTop--;
  if (IS_OBJ(*vm.stackTop)) decRef(AS_OBJ(*vm.stackTop));
  return *vm.stackTop;
}

// Chapter 15 Challenge 3: Replace old pop()
/*
  Value pop() {
  vm.stackCount--;
  return vm.stack[vm.stackCount];
}
*/
//< pop

//> Types of Values peek
static Value peek(int distance) {
  return vm.stackTop[-1 - distance];
}
//< Types of Values peek
/* Calls and Functions call < Closures call-signature
static bool call(ObjFunction* function, int argCount) {
*/
//> Calls and Functions call
//> Closures call-signature

// Chapter 25 Question 1: Replace call() function with call(), callClosure() and callFunction()
static bool call(Obj* callee, ObjFunction* function, int argCount) {
  if (argCount != function->arity) {
    runtimeError("Expected %d arguments but got %d.",
        function->arity, argCount);
    return false;
  }

  if (vm.frameCount == FRAMES_MAX) {
    runtimeError("Stack overflow.");
    return false;
  }

  CallFrame* frame = &vm.frames[vm.frameCount++];
  frame->function = (Obj*)callee;
  frame->ip = function->chunk.code;
  frame->slots = vm.stackTop - argCount - 1;
  return true;
}

static bool callClosure(ObjClosure* closure, int argCount) {
  return call((Obj*)closure, closure->function, argCount);
}

static bool callFunction(ObjFunction* function, int argCount) {
  return call((Obj*)function, function, argCount);
}

//< Calls and Functions call
//> Calls and Functions call-value
static bool callValue(Value callee, int argCount) {
  if (IS_OBJ(callee)) {
    switch (OBJ_TYPE(callee)) {
//> Methods and Initializers call-bound-method
      case OBJ_BOUND_METHOD: {
        ObjBoundMethod* bound = AS_BOUND_METHOD(callee);
//> store-receiver
        vm.stackTop[-argCount - 1] = bound->receiver;
//< store-receiver
        // Chapter 25 Question 1: Change OBJ_BOUND_METHOD call signature
        return callClosure(bound->method, argCount);
      }
//< Methods and Initializers call-bound-method
//> Classes and Instances call-class
case OBJ_CLASS: {
        ObjClass* klass = AS_CLASS(callee);
        vm.stackTop[-argCount - 1] = OBJ_VAL(newInstance(klass));
//> Methods and Initializers call-init
        // Chapter 28 Question 1: Replace looking up init via tableGet
        if (!IS_NIL(klass->initializer)) {
          // Chapter 25 Question 1:
          // Chapter 28 Question 1: handle both closure and plain function
          if (IS_CLOSURE(klass->initializer)) {
            return callClosure(AS_CLOSURE(klass->initializer), argCount);
          } else {
            return callFunction(AS_FUNCTION(klass->initializer), argCount);
          }
//> no-init-arity-error
        } else if (argCount != 0) {
          runtimeError("Expected 0 arguments but got %d.",
                       argCount);
          return false;
//< no-init-arity-error
        }
//< Methods and Initializers call-init
        return true;
      }
//< Classes and Instances call-class
//> Closures call-value-closure
// Chapter 25 Question 1: Fix callValue()
      case OBJ_CLOSURE:
        return callClosure(AS_CLOSURE(callee), argCount);
      case OBJ_FUNCTION:
        return callFunction(AS_FUNCTION(callee), argCount);
//< Closures call-value-closure
/* Calls and Functions call-value < Closures call-value-closure
      case OBJ_FUNCTION: // [switch]
        return call(AS_FUNCTION(callee), argCount);
*/
//> call-native
// Chapter 24 Question 2: Update OBJ_NATIVE case
      case OBJ_NATIVE: {
        // NativeFn native = AS_NATIVE(callee);
        // Value result = native(argCount, vm.stackTop - argCount);
        // v.stackTop -= argCount + 1;
        // push(result);
        // return true;
        NativeFn native = AS_NATIVE(callee);
        if (native(argCount, vm.stackTop - argCount)) {
          vm.stackTop -= argCount;
          return true;
        } else {
          runtimeError("%s", getStringChars(AS_STRING(vm.stackTop[-argCount - 1])));
          return false;
        }
      }
//< call-native
      default:
        break; // Non-callable object type.
    }
  }
  runtimeError("Can only call functions and classes.");
  return false;
}
//< Calls and Functions call-value
//> Methods and Initializers invoke-from-class
static bool invokeFromClass(ObjClass* klass, ObjString* name,
                            int argCount) {
  Value method;
  if (!tableGet(&klass->methods, OBJ_VAL(name), &method)) {
    runtimeError("Undefined property '%s'.", getStringChars(name));
    return false;
  }
  // Chapter 25 Question 1: Change invokeFromClass call signature and handle both closure and plain function
  if (IS_CLOSURE(method)) {
    return callClosure(AS_CLOSURE(method), argCount);
  } else {
    return callFunction(AS_FUNCTION(method), argCount);
  }
}
//< Methods and Initializers invoke-from-class
//> Methods and Initializers invoke
static bool invoke(ObjString* name, int argCount) {
  Value receiver = peek(argCount);
//> invoke-check-type

  if (!IS_INSTANCE(receiver)) {
    runtimeError("Only instances have methods.");
    return false;
  }

//< invoke-check-type
  ObjInstance* instance = AS_INSTANCE(receiver);
//> invoke-field

  Value value;
  if (tableGet(&instance->fields, OBJ_VAL(name), &value)) {
    vm.stackTop[-argCount - 1] = value;
    return callValue(value, argCount);
  }

//< invoke-field
  return invokeFromClass(instance->klass, name, argCount);
}
//< Methods and Initializers invoke
//> Methods and Initializers bind-method
static bool bindMethod(ObjClass* klass, ObjString* name) {
  Value method;
  if (!tableGet(&klass->methods, OBJ_VAL(name), &method)) {
    runtimeError("Undefined property '%s'.", getStringChars(name));
    return false;
  }

  // Chapter 29 Question 3: handle both closure and plain function
  ObjClosure* closure;
  if (IS_CLOSURE(method)) {
    closure = AS_CLOSURE(method);
  } else {
    closure = newClosure(AS_FUNCTION(method));
  }

  ObjBoundMethod* bound = newBoundMethod(peek(0), closure);
  pop();
  push(OBJ_VAL(bound));
  return true;
}
//< Methods and Initializers bind-method
//> Closures capture-upvalue
static ObjUpvalue* captureUpvalue(Value* local) {
//> look-for-existing-upvalue
  ObjUpvalue* prevUpvalue = NULL;
  ObjUpvalue* upvalue = vm.openUpvalues;
  while (upvalue != NULL && upvalue->location > local) {
    prevUpvalue = upvalue;
    upvalue = upvalue->next;
  }

  if (upvalue != NULL && upvalue->location == local) {
    return upvalue;
  }

//< look-for-existing-upvalue
  ObjUpvalue* createdUpvalue = newUpvalue(local);
//> insert-upvalue-in-list

  // Chapter 26 Question 3: Upvalue is referenced immediately, so refCount is updated
  incRef((Obj*)createdUpvalue);

  // Chapter 26 Question 3: Increment the reference count of the local if it is an object
  if (IS_OBJ(*local)) incRef(AS_OBJ(*local));

  createdUpvalue->next = upvalue;

  if (prevUpvalue == NULL) {
    vm.openUpvalues = createdUpvalue;
  } else {
    prevUpvalue->next = createdUpvalue;
  }

//< insert-upvalue-in-list
  return createdUpvalue;
}
//< Closures capture-upvalue
//> Closures close-upvalues
static void closeUpvalues(Value* last) {
  while (vm.openUpvalues != NULL &&
         vm.openUpvalues->location >= last) {
    ObjUpvalue* upvalue = vm.openUpvalues;
    upvalue->closed = *upvalue->location;
    upvalue->location = &upvalue->closed;
    vm.openUpvalues = upvalue->next;
  }
}
//< Closures close-upvalues
//> Methods and Initializers define-method
static void defineMethod(ObjString* name) {
  Value method = peek(0);
  ObjClass* klass = AS_CLASS(peek(1));

  // Chapter 29 Question 3: only tag if it's actually a closure
  if (IS_CLOSURE(method)) {
    ObjClosure* closure = AS_CLOSURE(method);
    closure->owner = klass;
    tableSet(&klass->ownMethods, OBJ_VAL(name), method);
    Value existing;
    if (!tableGet(&klass->methods, OBJ_VAL(name), &existing)) {
      tableSet(&klass->methods, OBJ_VAL(name), method);
    }
  } else {
    // Chapter 29 Question 3: plain function (no upvalues), store normally
    tableSet(&klass->methods, OBJ_VAL(name), method);
  }

  // Chapter 28 Question 1: cache initializer directly on class
  if (name == vm.initString) klass->initializer = method;
  pop();
}
//< Methods and Initializers define-method
//> Types of Values is-falsey
static bool isFalsey(Value value) {
  return IS_NIL(value) || (IS_BOOL(value) && !AS_BOOL(value));
}
//< Types of Values is-falsey

//> Strings concatenate
static void concatenate() {
  ObjString* b = AS_STRING(peek(0));
  ObjString* a = AS_STRING(peek(1));

  // Chapter 30 Question 2: handle both string types
  const char* aChars = getStringChars(a);
  const char* bChars = getStringChars(b);
  int aLen = getStringLength(a);
  int bLen = getStringLength(b);

  int length = aLen + bLen;
  char* chars = ALLOCATE(char, length + 1);
  memcpy(chars, aChars, aLen);
  memcpy(chars + aLen, bChars, bLen);
  chars[length] = '\0';

  ObjString* result = takeString(chars, length);
  pop();
  pop();
  push(OBJ_VAL(result));
}
//< Strings concatenate


/* Chapter 19 Question 1: Replace Original concatenate() with:
  // Allocate the ObjString first using the new MakeString
  // Write directly into its trailing chars buffer with the two memcpy calls after
  static void concatenate() {
    ObjString* b = AS_STRING(pop());
    ObjString* a = AS_STRING(pop());

    int length = a->length + b->length;
    ObjString* result = makeString(length);
    memcpy(result->chars, a->chars, a->length);
    memcpy(result->chars + a->length, b->chars, b->length);
   result->chars[length] = '\0';

    push(OBJ_VAL(result));
  }
*/

/* Chapter 19 Question 2: Replace other concatenate() with:
  static void concatenate() {
    ObjString* b = AS_STRING(pop());
    ObjString* a = AS_STRING(pop());

    int length = a->length + b->length;
    char* chars = ALLOCATE(char, length + 1);
    memcpy(chars, a->chars, a->length);
    memcpy(chars + a->length, b->chars, b->length);
    chars[length] = '\0';

    ObjString* result = makeString(true, chars, length);
    push(OBJ_VAL(result));
  }
*/

//> run
static InterpretResult run() {
//> Calls and Functions run
  CallFrame* frame = &vm.frames[vm.frameCount - 1];
  // Chapter 24 Question 1:
  register uint8_t* ip = frame->ip;

/* A Virtual Machine run < Calls and Functions run
#define READ_BYTE() (*vm.ip++)
*/
// #define READ_BYTE() (*frame->ip++)
// Chapter 24 Question 1: Replace parameter with (*ip++)
#define READ_BYTE() (*ip++)
/* A Virtual Machine read-constant < Calls and Functions run
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
*/

// Chapter 22 Question 4: Add macro
#define READ_SLOT_LONG() \
  (ip += 2, (uint16_t)((ip[-2] << 8) | ip[-1]))

/* Jumping Back and Forth read-short < Calls and Functions run
#define READ_SHORT() \
    (vm.ip += 2, (uint16_t)((vm.ip[-2] << 8) | vm.ip[-1]))
*/

// Chapter 24 Question 1: Replace frame-> ip with ip
#define READ_SHORT() \
    (ip += 2, (uint16_t)((ip[-2] << 8) | ip[-1]))

/* Calls and Functions run < Closures read-constant
#define READ_CONSTANT() \
    (frame->function->chunk.constants.values[READ_BYTE()])
*/
//> Closures read-constant
// Chapter 25 Question 1: Fix READ_CONSTANT Macro
#define READ_CONSTANT() \
    (getFrameFunction(frame)->chunk.constants.values[READ_BYTE()])
//< Closures read-constant

//< Calls and Functions run
//> Global Variables read-string
#define READ_STRING() AS_STRING(READ_CONSTANT())
//< Global Variables read-string
/* A Virtual Machine binary-op < Types of Values binary-op
#define BINARY_OP(op) \
    do { \
      double b = pop(); \
      double a = pop(); \
      push(a op b); \
    } while (false)
*/
//> Types of Values binary-op
// Chapter 24 Question 1: Add frame->ip = ip;
#define BINARY_OP(valueType, op) \
    do { \
      if (!IS_NUMBER(peek(0)) || !IS_NUMBER(peek(1))) { \
        frame->ip = ip; \
        runtimeError("Operands must be numbers."); \
        return INTERPRET_RUNTIME_ERROR; \
      } \
      double b = AS_NUMBER(pop()); \
      double a = AS_NUMBER(pop()); \
      push(valueType(a op b)); \
    } while (false)
//< Types of Values binary-op

/* Chapter 15 Challenge 4: Replace TB BINARY_OP with:
#define BINARY_OP(op) \
  do { \
    vm.stackTop[-2] = vm.stackTop[-2] op vm.stackTop[-1]; \
    vm.stackTop--; \
  } while (false)
*/

  for (;;) {
//> trace-execution
#ifdef DEBUG_TRACE_EXECUTION
//> trace-stack
    printf("          ");
    for (Value* slot = vm.stack; slot < vm.stackTop; slot++) {
      printf("[ ");
      printValue(*slot);
      printf(" ]");
    }
    printf("\n");
//< trace-stack
/* A Virtual Machine trace-execution < Calls and Functions trace-execution
    disassembleInstruction(vm.chunk,
                           (int)(vm.ip - vm.chunk->code));
*/
/* Calls and Functions trace-execution < Closures disassemble-instruction
    disassembleInstruction(&frame->function->chunk,
        (int)(frame->ip - frame->function->chunk.code));
*/
// Chapter 25 Question 1: Modify disassembleInstruction
//> Closures disassemble-instruction
    disassembleInstruction(&getFrameFunction(frame)->chunk,
      // Chapter 24 Question 1: Change to IP
        (int)(ip - getFrameFunction(frame)->chunk.code));
//< Closures disassemble-instruction
#endif

//< trace-execution
    uint8_t instruction;
    switch (instruction = READ_BYTE()) {
//> op-constant
      case OP_CONSTANT: {
        Value constant = READ_CONSTANT();
/* A Virtual Machine op-constant < A Virtual Machine push-constant
        printValue(constant);
        printf("\n");
*/
        push(constant);
        break;
      }
      case OP_NIL: push(NIL_VAL); break;
      case OP_TRUE: push(BOOL_VAL(true)); break;
      case OP_FALSE: push(BOOL_VAL(false)); break;
      case OP_POP: pop(); break;
      case OP_DUP:
        push(peek(0));
        break;
      case OP_GET_LOCAL: {
        uint8_t slot = READ_BYTE();
        push(frame->slots[slot]);
        break;
      }
      // Chapter 22 Question 4: add OP_GET_LOCAL_LONG case
      case OP_GET_LOCAL_LONG: {
        uint16_t slot = READ_SLOT_LONG();
        push(frame->slots[slot]);
        break;
      }
      case OP_SET_LOCAL: {
        uint8_t slot = READ_BYTE();
        frame->slots[slot] = peek(0);
        break;
      }
      // Chapter 22 Question 4: add OP_SET_LOCAL_LONG case
      case OP_SET_LOCAL_LONG: {
        uint16_t slot = READ_SLOT_LONG();
        frame->slots[slot] = peek(0);
        break;
      }
      case OP_GET_GLOBAL: {
        ObjString* name = READ_STRING();
        Value value;
        if (!tableGet(&vm.globals, OBJ_VAL(name), &value)) {
          // Chapter 24 Question 1:
          frame->ip = ip;
          // Chapter 30 Question 2: use string helper
          runtimeError("Undefined variable '%s'.", getStringChars(name));
          return INTERPRET_RUNTIME_ERROR;
        }
        push(value);
        break;
      }
      case OP_DEFINE_GLOBAL: {
        ObjString* name = READ_STRING();
        tableSet(&vm.globals, OBJ_VAL(name), peek(0));
        pop();
        break;
      }
      case OP_SET_GLOBAL: {
        ObjString* name = READ_STRING();
        if (tableSet(&vm.globals, OBJ_VAL(name), peek(0))) {
          tableDelete(&vm.globals, OBJ_VAL(name)); // [delete]
          // Chapter 24 Question 1:
          frame->ip = ip;
          // Chapter 30 Question 2: use string helper
          runtimeError("Undefined variable '%s'.", getStringChars(name));
          return INTERPRET_RUNTIME_ERROR;
        }
        break;
      }
      case OP_GET_UPVALUE: {
        uint8_t slot = READ_BYTE();
        // Chapter 25 Question 1: Fix OP_GET_UPVALUE
        push(*((ObjClosure*)frame->function)->upvalues[slot]->location);
        break;
      }
      case OP_SET_UPVALUE: {
        uint8_t slot = READ_BYTE();
        // Chapter 25 Question 1: Fix OP_SET_UPVALUE
        *((ObjClosure*)frame->function)->upvalues[slot]->location = peek(0);
        break;
      }
      case OP_GET_PROPERTY: {
        if (!IS_INSTANCE(peek(0))) {
          runtimeError("Only instances have properties.");
          return INTERPRET_RUNTIME_ERROR;
        }
        ObjInstance* instance = AS_INSTANCE(peek(0));
        ObjString* name = READ_STRING();
        
        Value value;
        if (tableGet(&instance->fields, OBJ_VAL(name), &value)) {
          pop(); // Instance.
          push(value);
          break;
        }
        if (!bindMethod(instance->klass, name)) {
          return INTERPRET_RUNTIME_ERROR;
        }
        break;
      }
      case OP_SET_PROPERTY: {
        if (!IS_INSTANCE(peek(1))) {
          runtimeError("Only instances have fields.");
          return INTERPRET_RUNTIME_ERROR;
        }
        ObjInstance* instance = AS_INSTANCE(peek(1));
        tableSet(&instance->fields, OBJ_VAL(READ_STRING()), peek(0));
        Value value = pop();
        pop();
        push(value);
        break;
      }
      case OP_GET_SUPER: {
        ObjString* name = READ_STRING();
        ObjClass* superclass = AS_CLASS(pop());
        
        if (!bindMethod(superclass, name)) {
          return INTERPRET_RUNTIME_ERROR;
        }
        break;
      }
      case OP_EQUAL: {
        Value b = pop();
        Value a = pop();
        push(BOOL_VAL(valuesEqual(a, b)));
        break;
      }

      case OP_GREATER:  BINARY_OP(BOOL_VAL, >); break;
      case OP_LESS:     BINARY_OP(BOOL_VAL, <); break;
      case OP_ADD: {
        if (IS_STRING(peek(0)) && IS_STRING(peek(1))) {
          concatenate();
        } else if (IS_NUMBER(peek(0)) && IS_NUMBER(peek(1))) {
          double b = AS_NUMBER(pop());
          double a = AS_NUMBER(pop());
          push(NUMBER_VAL(a + b));
        } else {
          // Chapter 24 Question 1:
          frame->ip = ip;
          runtimeError(
              "Operands must be two numbers or two strings.");
          return INTERPRET_RUNTIME_ERROR;
        }
        break;
      }
//< Strings add-strings
//> Types of Values op-arithmetic
      case OP_SUBTRACT: BINARY_OP(NUMBER_VAL, -); break;
      case OP_MULTIPLY: BINARY_OP(NUMBER_VAL, *); break;
      case OP_DIVIDE:   BINARY_OP(NUMBER_VAL, /); break;
//< Types of Values op-arithmetic
//> Types of Values op-not
      case OP_NOT:
        push(BOOL_VAL(isFalsey(pop())));
        break;
//< Types of Values op-not
//> Types of Values op-negate
      case OP_NEGATE:
        if (!IS_NUMBER(peek(0))) {
          // Chapter 24 Question 1:
          frame->ip = ip;
          runtimeError("Operand must be a number.");
          return INTERPRET_RUNTIME_ERROR;
        }
        push(NUMBER_VAL(-AS_NUMBER(pop())));
        break;
//< Types of Values op-negate
//> Global Variables interpret-print
      case OP_PRINT: {
        printValue(pop());
        printf("\n");
        break;
      }
//< Global Variables interpret-print
//> Jumping Back and Forth op-jump
      case OP_JUMP: {
        uint16_t offset = READ_SHORT();
/* Jumping Back and Forth op-jump < Calls and Functions jump
        vm.ip += offset;
*/
//> Calls and Functions jump
// Chapter 24 Question 1: Change to IP
        ip += offset;
//< Calls and Functions jump
        break;
      }
//< Jumping Back and Forth op-jump
//> Jumping Back and Forth op-jump-if-false
      case OP_JUMP_IF_FALSE: {
        uint16_t offset = READ_SHORT();
/* Jumping Back and Forth op-jump-if-false < Calls and Functions jump-if-false
        if (isFalsey(peek(0))) vm.ip += offset;
*/
//> Calls and Functions jump-if-false
// Chapter 24 Question 1: Change to IP
        if (isFalsey(peek(0))) ip += offset;
//< Jumping Back and Forth jump-if-false
        break;
      }
//< Jumping Back and Forth op-jump-if-false
//> Jumping Back and Forth op-loop
      case OP_LOOP: {
        uint16_t offset = READ_SHORT();
/* Jumping Back and Forth op-loop < Calls and Functions loop
        vm.ip -= offset;
*/
//> Calls and Functions loop
// Chapter 24 Question 1: Change to IP
        ip -= offset;
//< Calls and Functions loop
        break;
      }
//< Jumping Back and Forth op-loop
//> Calls and Functions interpret-call
      case OP_CALL: {
        int argCount = READ_BYTE();
        // Chapter 24 Question 1: save before call
        frame->ip = ip;
        if (!callValue(peek(argCount), argCount)) {
          return INTERPRET_RUNTIME_ERROR;
        }
//> update-frame-after-call
        frame = &vm.frames[vm.frameCount - 1];
        // Chapter 24 Question 1: Add Load Callee's IP
        ip = frame->ip;
//< update-frame-after-call
        break;
      }
//< Calls and Functions interpret-call
//> Methods and Initializers interpret-invoke
      case OP_INVOKE: {
        ObjString* method = READ_STRING();
        int argCount = READ_BYTE();
        if (!invoke(method, argCount)) {
          return INTERPRET_RUNTIME_ERROR;
        }
        frame = &vm.frames[vm.frameCount - 1];
        break;
      }
//< Methods and Initializers interpret-invoke
//> Superclasses interpret-super-invoke
      case OP_SUPER_INVOKE: {
        ObjString* method = READ_STRING();
        int argCount = READ_BYTE();
        ObjClass* superclass = AS_CLASS(pop());
        if (!invokeFromClass(superclass, method, argCount)) {
          return INTERPRET_RUNTIME_ERROR;
        }
        frame = &vm.frames[vm.frameCount - 1];
        break;
      }
//< Superclasses interpret-super-invoke
//> Closures interpret-closure
      case OP_CLOSURE: {
        ObjFunction* function = AS_FUNCTION(READ_CONSTANT());
        ObjClosure* closure = newClosure(function);
        push(OBJ_VAL(closure));
//> interpret-capture-upvalues
        for (int i = 0; i < closure->upvalueCount; i++) {
          uint8_t isLocal = READ_BYTE();
          uint8_t index = READ_BYTE();
          if (isLocal) {
            closure->upvalues[i] =
                captureUpvalue(frame->slots + index);
          } else {
            // Chapter 25 Question 1: Fix OP_CLOSURE
            closure->upvalues[i] = ((ObjClosure*)frame->function)->upvalues[index];
          }
        }
//< interpret-capture-upvalues
        break;
      }
//< Closures interpret-closure
//> Closures interpret-close-upvalue
      case OP_CLOSE_UPVALUE:
        closeUpvalues(vm.stackTop - 1);
        pop();
        break;
//< Closures interpret-close-upvalue
      case OP_RETURN: {
/* A Virtual Machine print-return < Global Variables op-return
        printValue(pop());
        printf("\n");
*/
/* Global Variables op-return < Calls and Functions interpret-return
        // Exit interpreter.
*/
/* A Virtual Machine run < Calls and Functions interpret-return
        return INTERPRET_OK;
*/
//> Calls and Functions interpret-return
        Value result = pop();
//> Closures return-close-upvalues
        closeUpvalues(frame->slots);
//< Closures return-close-upvalues
        vm.frameCount--;
        if (vm.frameCount == 0) {
          pop();
          return INTERPRET_OK;
        }

        vm.stackTop = frame->slots;
        push(result);
        frame = &vm.frames[vm.frameCount - 1];
        // Chapter 24 Question 1:
        ip = frame->ip;
        break;
//< Calls and Functions interpret-return
      }
//> Classes and Instances interpret-class
      case OP_CLASS:
        push(OBJ_VAL(newClass(READ_STRING())));
        break;
//< Classes and Instances interpret-class
//> Superclasses interpret-inherit
      case OP_INHERIT: {
        Value superclass = peek(1);
//> inherit-non-class
        if (!IS_CLASS(superclass)) {
          runtimeError("Superclass must be a class.");
          return INTERPRET_RUNTIME_ERROR;
        }

//< inherit-non-class
        ObjClass* subclass = AS_CLASS(peek(0));
        tableAddAll(&AS_CLASS(superclass)->methods,
                    &subclass->methods);
        // Chapter 29 Question 3: Set superclass
        subclass->superclass = AS_CLASS(superclass);
        pop(); // Subclass.
        break;
      }
//< Superclasses interpret-inherit
//> Methods and Initializers interpret-method
      case OP_METHOD:
        defineMethod(READ_STRING());
        break;
//< Methods and Initializers interpret-method
      // Chapter 29 Question 3: Add OP_INNER case:
      case OP_INNER: {
        ObjString* name = READ_STRING();
        int argCount = READ_BYTE();
        ObjClass* owner = ((ObjClosure*)frame->function)->owner;
        ObjInstance* instance = AS_INSTANCE(peek(argCount));

        // Walk up from receiver's class, stop just below the owner.
        ObjClass* path[64];
        int pathLen = 0;
        for (ObjClass* k = instance->klass;
             k != NULL && k != owner && pathLen < 64;
             k = k->superclass) {
          path[pathLen++] = k;
        }

        // Walk back down: nearest subclass first.
        Value method;
        bool found = false;
        for (int i = pathLen - 1; i >= 0; i--) {
          if (tableGet(&path[i]->ownMethods, OBJ_VAL(name), &method)) { found = true; break; }
        }

        if (!found) {           // no match: inner is a no-op
          vm.stackTop -= argCount;
          *(vm.stackTop - 1) = NIL_VAL;
          break;
        }
        frame->ip = ip;
        if (!callClosure(AS_CLOSURE(method), argCount)) return INTERPRET_RUNTIME_ERROR;
        frame = &vm.frames[vm.frameCount - 1];
        ip = frame->ip;
        break;
      }
    }
  }
#undef READ_BYTE
//> Jumping Back and Forth undef-read-short
#undef READ_SHORT
//< Jumping Back and Forth undef-read-short
//> undef-read-constant
#undef READ_CONSTANT
//< undef-read-constant
//> Global Variables undef-read-string
#undef READ_STRING
//< Global Variables undef-read-string
//> undef-binary-op
#undef BINARY_OP
//< undef-binary-op
}
//< run
//> omit
void hack(bool b) {
  // Hack to avoid unused function error. run() is not used in the
  // scanning chapter.
  run();
  if (b) hack(false);
}
//< omit
//> interpret
/* A Virtual Machine interpret < Scanning on Demand vm-interpret-c
InterpretResult interpret(Chunk* chunk) {
  vm.chunk = chunk;
  vm.ip = vm.chunk->code;
  return run();
*/
//> Scanning on Demand vm-interpret-c
InterpretResult interpret(const char* source) {
/* Scanning on Demand vm-interpret-c < Compiling Expressions interpret-chunk
  compile(source);
  return INTERPRET_OK;
*/
/* Compiling Expressions interpret-chunk < Calls and Functions interpret-stub
  Chunk chunk;
  initChunk(&chunk);

  if (!compile(source, &chunk)) {
    freeChunk(&chunk);
    return INTERPRET_COMPILE_ERROR;
  }

  vm.chunk = &chunk;
  vm.ip = vm.chunk->code;
*/
//> Calls and Functions interpret-stub
  ObjFunction* function = compile(source);
  if (function == NULL) return INTERPRET_COMPILE_ERROR;

  push(OBJ_VAL(function));
//< Calls and Functions interpret-stub
/* Calls and Functions interpret-stub < Calls and Functions interpret
  CallFrame* frame = &vm.frames[vm.frameCount++];
  frame->function = function;
  frame->ip = function->chunk.code;
  frame->slots = vm.stack;
*/
/* Calls and Functions interpret < Closures interpret
  call(function, 0);
*/
//> Closures interpret
  ObjClosure* closure = newClosure(function);
  pop();
  push(OBJ_VAL(closure));
  // Chapter 25 Question 1: Edit interpret() call signature
  callClosure(closure, 0);
//< Closures interpret
//< Scanning on Demand vm-interpret-c
//> Compiling Expressions interpret-chunk

/* Compiling Expressions interpret-chunk < Calls and Functions end-interpret
  InterpretResult result = run();

  freeChunk(&chunk);
  return result;
*/
//> Calls and Functions end-interpret
  return run();
//< Calls and Functions end-interpret
//< Compiling Expressions interpret-chunk
}
//< interpret