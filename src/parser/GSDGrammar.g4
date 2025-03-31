## Tenía pensado usar ANTLR para el parser, pero al final no lo hice ##
## Por lo que no sé si el formato está correcto para usar en ese programa ##

grammar GSDGrammar;

program:
    declaration+
    ;

declaration:
    funcDecl
    | varDecl
    | statement
    ;

funcDecl:
    'func' function
    ;

function:
    identifier '(' parameters? ')' block
    ;

block:
    '{' declaration* '}'
    ;

varDecl:
    'var' identifier ( '=' expression )? ';'
    ;

statement:
    forStmt
    | ifStmt
    | whileStmt
    | block
    | printStmt
    | inputStmt
    | returnStmt
    | exprStmt
    ;

forStmt:
    'for' '(' ( varDecl | exprStmt | ';' )
                      expression? ';'
                      expression? ')' statement
    ;

ifStmt:
    'if' '(' expression ')' statement
           ( 'else' statement )?
    ;

whileStmt:
    'while' '(' expression ')' statement
    ;

printStmt:
    'print' expression ';'
    ;

inputStmt:
    'input' identifier (String)? ';'
    ;

returnStmt:
    'return' expression? ';'
    ;

exprStmt:
    expression ';'
    ;

expression:
    assignment
    ;

assignment:
    identifier '=' assignment
               | logic_or
    ;

logic_or:
    logic_and ( 'or' logic_and )*
    ;

logic_and:
    equality ( 'and' equality )*
    ;

equality:
    comparison ( ( '!=' | '==' ) comparison )*
    ;

comparison:
    term ( ( '>' | '>=' | '<' | '<=' ) term )*
    ;

term:
    factor ( ( '-' | '+' ) factor )*
    ;

factor:
    unary ( ( '/' | '*' ) unary )*
    ;

unary:
    ( '!' | '-' ) unary | call
    ;

call:
    primary ( '(' arguments? ')')*
    ;

primary:
    'true'
    | 'false'
    | 'null'
    | number
    | String
    ;

parameters:
    identifier ( ',' identifier )*
    ;
arguments:
    expression ( ',' expression )*
    ;

identifier:
    Alpha ( Alpha | Digit )*
    ;

number:
    Digit+ ( '.' Digit+ )?
    ;

String:
    '"' ~['"\r\n]* '"'
    ;

Alpha:
    [a-zA-Z_]
    ;

Digit:
    [0-9]
    ;
