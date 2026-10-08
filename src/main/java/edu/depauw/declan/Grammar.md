# Context-free Grammar for DeCLan

This is the grammar for the DePauw Compilers Language (DeCLan).
It is based on the Oberon programming language, with no modules, records, pointers, case statements, sets, function procedures, or procedure variables. Arrays of arrays are allowed, but an array cannot be assigned or passed as a whole.

## Lexical Rules

```
letter -> [A-Za-z]
digit -> [0-9]
hexDigit -> digit | [A-F]

ident -> letter (letter | digit)*  // except for the reserved words, listed below

integer -> digit digit* | digit hexDigit* H

real -> digit digit* . digit* scaleFactor?
scaleFactor -> E (+ | -)? digit digit*

number -> integer | real

// The type of an integer literal is the minimal type to which the number belongs:
// INTEGER if it is at most 32767, otherwise LONGINT (at most 2147483647). There is no
// separate LONGINT literal syntax.
```

Whitespace between tokens may be zero or more spaces, tabs, newlines, carriage returns, or comments.
A comment is any sequence of characters, other than `*)`, surrounded by a leading `(*` and a trailing `*)`.
The body of a comment may contain another comment, which must be properly closed (and which itself may
contain further nested comments).

### Reserved Words

```
ARRAY, BEGIN, BOOLEAN, BY, CONST, DIV, DO, ELSE, ELSIF, END, FALSE, FOR, IF, INTEGER, LONGINT, MOD, OF, OR, PROCEDURE, REAL, REPEAT, THEN, TO, TRUE, TYPE, UNTIL, VAR, WHILE
```

## Syntax Rules

```
Program -> DeclSequence ProcedureDeclSequence BEGIN StatementSequence END .

DeclSequence -> Constants Types Variables

Constants -> CONST ConstDecl ; ConstDeclSequence
Constants ->

ConstDeclSequence -> ConstDecl ; ConstDeclSequence
ConstDeclSequence ->

ConstDecl -> ident = ConstExpr

ConstExpr -> Expression  // may only use idents defined in previous ConstDecls

Types -> TYPE TypeDecl ; TypeDeclSequence
Types ->

TypeDeclSequence -> TypeDecl ; TypeDeclSequence
TypeDeclSequence ->

TypeDecl -> ident = Type

Variables -> VAR VariableDecl ; VariableDeclSequence
Variables ->

VariableDeclSequence -> VariableDecl ; VariableDeclSequence
VariableDeclSequence ->

VariableDecl -> IdentList : Type

IdentList -> ident IdentListRest

IdentListRest -> , ident IdentListRest
IdentListRest ->

BasicType -> BOOLEAN | INTEGER | LONGINT | REAL

Type -> BasicType
Type -> ident  // a type declared with TYPE
Type -> ARRAY ConstExpr OF Type  // the length must be a positive INTEGER constant

ProcedureDeclSequence -> ProcedureDecl ; ProcedureDeclSequence
ProcedureDeclSequence ->

ProcedureDecl -> ProcedureHead ; ProcedureBody ident  // same ident from ProcedureHead

ProcedureHead -> PROCEDURE ident FormalParameters

ProcedureBody -> DeclSequence BEGIN StatementSequence END

FormalParameters -> ( FPSection FPSectionSequence )
FormalParameters -> ( )

FPSectionSequence -> ; FPSection FPSectionSequence
FPSectionSequence ->

FPSection -> VAR IdentList : BasicType
FPSection -> IdentList : BasicType

StatementSequence -> Statement StatementSequenceRest

StatementSequenceRest -> ; Statement StatementSequenceRest
StatementSequenceRest ->

Statement -> Assignment | ProcedureCall | IfStatement | WhileStatement | RepeatStatement | ForStatement
Statement ->

Assignment -> Designator := Expression

Designator -> ident Selectors

Selectors -> [ Expression ] Selectors
Selectors ->

ProcedureCall -> ident ActualParameters

ActualParameters -> ( ExpList )
ActualParameters -> ( )

ExpList -> Expression ExpListRest

ExpListRest -> , Expression
ExpListRest ->

IfStatement -> IF Expression THEN StatementSequence ElsifThenSequence ELSE StatementSequence END
IfStatement -> IF Expression THEN StatementSequence ElsifThenSequence END

ElsifThenSequence -> ELSIF Expression THEN StatementSequence ElsifThenSequence
ElsifThenSequence ->

WhileStatement -> WHILE Expression DO StatementSequence ElsifDoSequence END

ElsifDoSequence -> ELSIF Expression DO StatementSequence ElsifDoSequence
ElsifDoSequence ->

RepeatStatement -> REPEAT StatementSequence UNTIL Expression

ForStatement -> FOR ident := Expression TO Expression BY ConstExpr DO StatementSequence END
ForStatement -> FOR ident := Expression TO Expression DO StatementSequence END

Expression -> SimpleExpr
Expression -> SimpleExpr Relation SimpleExpr

Relation -> = | # | < | <= | > | >=

SimpleExpr -> + Term SimpleExprRest
SimpleExpr -> - Term SimpleExprRest
SimpleExpr -> Term SimpleExprRest

SimpleExprRest -> AddOperator Term SimpleExprRest
SimpleExprRest ->

AddOperator -> + | - | OR

Term -> Factor TermRest

TermRest -> MulOperator Factor TermRest
TermRest ->

MulOperator -> * | / | DIV | MOD | &

Factor -> number | TRUE | FALSE | Designator
Factor -> ( Expression )
Factor -> ~ Factor
```

## Types and LONGINT

DeCLan has four types: `BOOLEAN`, `INTEGER`, `LONGINT` and `REAL`.

| Type | Size | Range |
| --- | --- | --- |
| `INTEGER` | 16 bits | -32768 to 32767 |
| `LONGINT` | 32 bits | -2147483648 to 2147483647 |

Both integer types use two's complement and wrap around silently on overflow, as `int` does in Java.

**Literals.** There is no separate syntax for `LONGINT` literals. The type of an integer literal is the minimal type to which the number belongs: `32767` is an `INTEGER`, and `32768` is a `LONGINT`. A literal larger than 2147483647 is an error. Constant expressions follow the same rule, so after `CONST big = 200 * 200;` the constant `big` (40000) is a `LONGINT`.

**Widening.** An `INTEGER` is converted to `LONGINT` automatically in assignments, in value parameters, and when it is mixed with a `LONGINT` in an expression. There is no automatic conversion the other way.

**Operations.** `LONGINT` supports `+`, `-` (binary and unary), `*`, `DIV`, `MOD`, and all six comparisons (`=`, `#`, `<`, `<=`, `>`, `>=`). `DIV` and `MOD` truncate toward zero, like their `INTEGER` versions. A `LONGINT` can be a global, a local, a value parameter or a `VAR` parameter.

**Watch out: widen before you multiply.** An expression whose operands are both `INTEGER` is computed in 16 bits, even if it is assigned to a `LONGINT`:

```
VAR i, j : INTEGER; a : LONGINT;
BEGIN
  i := 300; j := 300;
  a := i * j;     (* 16-bit multiply, wraps: a = 24464 *)
  a := i;
  a := a * j      (* 32-bit multiply: a = 90000 *)
END.
```

A `LONGINT` variable can be the counter of a `FOR` loop. Its start and stop values may be `INTEGER` or `LONGINT`, and the `BY` step may be an `INTEGER` or `LONGINT` constant. An `INTEGER` counter still needs `INTEGER` bounds and step.

**Not supported yet.** Mixing `LONGINT` with `REAL`, including assigning a `LONGINT` to a `REAL`.

### Standard procedures for input and output

| Call | Does |
| --- | --- |
| `ReadInt(n)` | reads a decimal `INTEGER` into the variable `n` |
| `ReadLong(n)` | reads a signed decimal number into the `LONGINT` variable `n`; skips blanks and line ends first, and wraps silently like other `LONGINT` arithmetic |
| `WriteInt(n)` | a space, then the `INTEGER` `n` |
| `WriteLong(n)` | a space, then the signed decimal value of the `LONGINT` `n` (an `INTEGER` argument is widened) |
| `WriteLn()` | a newline |

### Example

Factorials and a Fibonacci number that do not fit in 16 bits (also in `demo/longint-demo.dcl`):

```
VAR f, a, b, t : LONGINT; i : INTEGER;

BEGIN
  f := 1;
  FOR i := 1 TO 12 DO
    f := f * i;
    WriteLong(i);
    WriteLong(f);
    WriteLn()
  END;
  a := 0;
  b := 1;
  FOR i := 1 TO 45 DO
    t := a + b;
    a := b;
    b := t
  END;
  WriteLong(a);      (* Fibonacci(45) = 1134903170 *)
  WriteLn()
END.
```

The compiler implements `LONGINT` arithmetic with the library routines `DADD`, `DSUB`, `DMul` and `DDiv`, and printing with `DPRINTD`. A program that does not use `LONGINT` compiles to exactly the same code as before, and each library is included only when the program needs it.

## Arrays and TYPE

A `TYPE` section, between `CONST` and `VAR`, gives names to types:

```
CONST n = 10;
TYPE Vector = ARRAY n OF INTEGER;
     Table = ARRAY 3 OF Vector;
VAR v : Vector; t : Table; x : ARRAY 5 OF LONGINT;
```

An `ARRAY` type has a length, which is a constant expression with a positive `INTEGER` value, and an element type. The elements are numbered from 0 to length - 1, so `v[0]` is the first element of `v` and `v[n - 1]` is the last. Write `t[i][j]` for element `j` of element `i` of `t`.

* An index must be an `INTEGER`. If it is out of range when the program runs, the program prints `Array index out of range` and stops.
* The element type can be any type, including another array. A local array is allocated in the procedure's stack frame, so a very large array should be global.
* An array element can be used like a variable of the element type: assigned to, used in an expression, and passed to a `VAR` parameter, as in `ReadLong(x[i])`.
* An array as a whole cannot be assigned, compared, or passed to a procedure, and a procedure parameter cannot have an array type. Use a global array, or copy the elements one by one.

```
VAR a : ARRAY 5 OF INTEGER; i, sum : INTEGER;

BEGIN
  FOR i := 0 TO 4 DO a[i] := i * i END;
  sum := 0;
  FOR i := 0 TO 4 DO sum := sum + a[i] END;
  WriteLong(sum);    (* 30 *)
  WriteLn()
END.
```
