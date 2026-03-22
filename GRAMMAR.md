## Syntactic Grammar

The syntactic grammar uses Extended Backus-Naur Form (EBNF) to define the structure of LinkLang programs.

```ebnf
program        ::= declaration* EOF ;

declaration    ::= funcDecl
                 | varDecl
                 | statement ;

funcDecl       ::= "func" IDENTIFIER "(" parameters? ")" block ;

parameters     ::= IDENTIFIER ( "," IDENTIFIER )* ;

arguments      ::= expression ( "," expression )* ;

block          ::= "{" declaration* "}" ;

varDecl        ::= "var" IDENTIFIER ( "=" expression )? ";" ;

statement      ::= forStmt
                 | ifStmt
                 | whileStmt
                 | block
                 | printStmt
                 | inputStmt
                 | returnStmt
                 | exprStmt ;

forStmt        ::= "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;

ifStmt         ::= "if" "(" expression ")" statement ( "else" statement )? ;

whileStmt      ::= "while" "(" expression ")" statement ;

printStmt      ::= "print" expression ";" ;

inputStmt      ::= "input" IDENTIFIER expression? ";" ;

returnStmt     ::= "return" expression? ";" ;

exprStmt       ::= expression ";" ;

expression     ::= assignment ;

assignment     ::= logic_or ( "=" assignment )? ;

logic_or       ::= logic_and ( "or" logic_and )* ;

logic_and      ::= equality ( "and" equality )* ;

equality       ::= comparison ( ( "!=" | "==" ) comparison )* ;

comparison     ::= addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;

addition       ::= multiplication ( ( "-" | "+" ) multiplication )* ;

multiplication ::= unary ( ( "/" | "*" ) unary )* ;

unary          ::= ( "!" | "-" ) unary
                 | subscript ;

subscript      ::= call ( "[" addition "]" )? ;

call           ::= primary ( "(" arguments? ")" )* ;

primary        ::= "true"
                 | "false"
                 | "null"
                 | NUMBER
                 | STRING
                 | IDENTIFIER
                 | arrayLiteral
                 | "(" expression ")" ;

arrayLiteral   ::= "[" "]"
                 | "[" expression ":" expression? "]"
                 | "[" expression ( "," expression )* "]" ;
```

## Lexical Grammar

Tokens like `IDENTIFIER`, `NUMBER`, and `STRING` are defined as follows:

```ebnf
IDENTIFIER     ::= [a-zA-Z_] [a-zA-Z_0-9]* ;
NUMBER         ::= [0-9]+ ( "." [0-9]+ )? ;
STRING         ::= '"' [^\r\n"]* '"' ;
```

## Code Example

Below is an example of a LinkLang program that demonstrates various syntactic elements defined in the grammar:

```lila
// Variable declarations
var language = "LinkLang";
var version = 1;

// Arrays creation
var numbers = [5:0]; // Array of size 5, filled with 0
var predefined = [1, 2, 3];

// Functions
func factorial(n) {
    if (n <= 1) {
        return 1;
    } else {
        return n * factorial(n - 1);
    }
}

// Interacting with the console
print "Calculating factorial...";
var result = factorial(5);
print result;

// Loops and array subscript modifications
for (var i = 0; i < 5; i = i + 1) {
    numbers[i] = i * 2;
}

var idx = 0;
while (idx < 5) {
    print numbers[idx];
    idx = idx + 1;
}

// Requesting user input
input command "Enter a command here: ";
print "Executed:";
print command;
```
