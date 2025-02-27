## Tenía pensado usar ANTLR para el parser, pero al final no lo hice ##
## Por lo que no sé si el formato está correcto para usar en ese programa ##

grammar GSDGrammar;

program:
    declaration+
    ;

declaration:
    classDecl
    | funcDecl
    | varDecl
    | statement
    ;

classDecl:
    'class' identifier ( '<' identifier )? '{' ( varDecl | function )* '}'
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
    | returnStmt
    | connectStmt
    | disconnectStmt
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

returnStmt:
    'return' expression? ';'
    ;

connectStmt:
    'connect' String (String)? ';'
    ;
disconnect:
    'disconnect' ';'
    ;

exprStmt:
    expression ';'
    ;

expression:
    assignment
    ;

assignment:
    ( call '.' )? identifier '=' assignment
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
    primary ( '(' arguments? ')' | '.' identifier )*
    ;

primary:
    'true'
    | 'false'
    | 'null'
    | 'this'
    | number
    | String
    | 'super'
    | 'this'
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
