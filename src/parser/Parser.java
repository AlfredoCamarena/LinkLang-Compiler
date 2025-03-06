package parser;

import ast.Expr;
import ast.Stmt;
import main.GSD;
import scanner.Token;
import scanner.TokenType;
import semantic_analysis.SymbolTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private final SymbolTable symbolTable;
    private int current = 0;

    public Parser(List<Token> tokens, SymbolTable symbolTable) {
        this.tokens = tokens;
        this.symbolTable = symbolTable;
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();

        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.CLASS)) return classDecl();
            if (match(TokenType.FUNC)) return function("función");
            if (match(TokenType.VAR)) return varDecl();

            return statement();
        } catch (RuntimeException error) {
            sync();
            return null;
        }
    }

    private Stmt.Class classDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la clase.");

        symbolTable.enterScope();

        Expr.Variable superclass = null;
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la superclase.");
            superclass = new Expr.Variable(previous());
        }

        consume(TokenType.LEFT_BRACE, "Se esperaba '{' antes del cuerpo de la clase.");

        List<Stmt.VarDeclaration> variables = new ArrayList<>();
        List<Stmt.Function> methods = new ArrayList<>();


        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.VAR)) {
                variables.add(varDecl());
            } else if (match(TokenType.FUNC)) {
                methods.add(function("método"));
            } else {
                throw error(peek(), "Se esperaba la declaración de un método o variable.");
            }
        }

        consume(TokenType.RIGHT_BRACE, "Se esperaba '}' después del cuerpo de la clase.");

        symbolTable.exitScope();
        symbolTable.define(name, TokenType.CLASS, null);

        return new Stmt.Class(name, superclass, variables, methods);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de " + kind + ".");

        symbolTable.enterScope();

        consume(TokenType.LEFT_PAREN, "Se esperaba '(' después del nombre de  " + kind + ".");

        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                Token param = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de parámetro");
                parameters.add(param);
                symbolTable.define(param, TokenType.VAR, null);
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de los parámetros.");

        consume(TokenType.LEFT_BRACE, "Se esperaba '{' antes del cuerpo de  " + kind + ".");

        Stmt.Block body = block();

        symbolTable.exitScope();
        symbolTable.define(name, TokenType.FUNC, null);

        return new Stmt.Function(name, parameters, body);
    }

    private Stmt.Block block() {
        symbolTable.enterScope();

        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Se esperaba '}' después del contenido del bloque");

        symbolTable.exitScope();

        return new Stmt.Block(statements);
    }

    private Stmt.VarDeclaration varDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la variable.");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Se esperaba ';' después de la declaración de la variable.");

        symbolTable.define(name, TokenType.VAR, initializer);

        return new Stmt.VarDeclaration(name, initializer);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assignment(name, value);
            } else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Solo puedes realizar asignaciones a variables o atributos.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = addition();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr addition() {
        Expr expr = multiplication();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr multiplication() {
        Expr expr = unary();

        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre del atributo después de '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de los argumentos.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.NULL)) return new Expr.Literal(null);
        if (match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal());

        if (match(TokenType.SUPER)) {
            Token keyword = previous();
            consume(TokenType.DOT, "Se esperaba '.' después de 'super'.");
            Token method = consume(TokenType.IDENTIFIER, "Se esperaba el nombre del método de la superclase");
            return new Expr.Super(keyword, method);
        }

        if (match(TokenType.THIS)) {
            Token keyword = previous();
            return new Expr.This(keyword);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }


        // Ejemplo: (1 + 2) * 3
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de la expresión.");
            return new Expr.Group(expr);
        }

        throw error(peek(), "Se esperaba una expresión.");
    }


    private Stmt statement() {
        if (match(TokenType.FOR)) return forStmt();
        if (match(TokenType.IF)) return ifStmt();
        if (match(TokenType.WHILE)) return whileStmt();
        if (match(TokenType.LEFT_BRACE)) return block();
        if (match(TokenType.PRINT)) return printSmt();
        if (match(TokenType.RETURN)) return returnStmt();
        if (match(TokenType.CONNECT)) return connectStmt();
        if (match(TokenType.DISCONNECT)) return disconnectStmt();

        return exprStmt();
    }

    private Stmt forStmt() {
        consume(TokenType.LEFT_PAREN, "Se esperaba '(' después de 'for'.");

        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDecl();
        } else {
            initializer = exprStmt();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Se esperaba ';' después de la condición del ciclo.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después del incremento");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) {
            condition = new Expr.Literal(true);
        }

        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStmt() {
        consume(TokenType.LEFT_PAREN, "Se esperaba '(' después de 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de la condición.");
        Stmt thenBranch = statement();

        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStmt() {
        consume(TokenType.LEFT_PAREN, "Se esperaba '(' después de 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de la condición.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt printSmt() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Se esperaba ';' después del valor");
        return new Stmt.Print(value);
    }

    private Stmt returnStmt() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Se esperaba ';' después del retorno.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt connectStmt() {
        Expr ssid = expression();
        Expr password = null;
        if (!check(TokenType.SEMICOLON)) {
            password = expression();
        }
        consume(TokenType.SEMICOLON, "Se esperaba ';'.");
        return new Stmt.Connect(ssid, password);
    }

    private Stmt disconnectStmt() {
        consume(TokenType.SEMICOLON, "Se esperaba ';'.");
        return new Stmt.Disconnect();
    }

    private Stmt exprStmt() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Se esperaba ';' después de la expresión.");
        return new Stmt.Expression(expr);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;

        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private Token consume(TokenType type, String msg) {
        if (check(type))
            return advance();
        throw error(peek(), msg);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private RuntimeException error(Token token, String msg) {
        GSD.error(token, msg);
        return new RuntimeException();
    }

    private void sync() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON)
                return;

            switch (peek().type()) {
                case CLASS:
                case FUNC:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                case LEFT_BRACE:
                case CONNECT:
                case DISCONNECT:
                    return;
            }

            advance();
        }
    }
}
