package parser;

import ast.Expr;
import ast.Stmt;
import main.LinkLang;
import scanner.Token;
import scanner.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
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
            if (match(TokenType.FUNC)) return function();
            if (match(TokenType.VAR)) return varDecl();

            return statement();
        } catch (RuntimeException error) {
            sync();
            return null;
        }
    }

    private Stmt.Function function() {
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la función.");

        consume(TokenType.LEFT_PAREN, "Se esperaba '(' después del nombre de la función.");

        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                Token param = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de parámetro");
                parameters.add(param);
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de los parámetros.");

        consume(TokenType.LEFT_BRACE, "Se esperaba '{' antes del cuerpo de la función.");

        Stmt.Block body = block();

        return new Stmt.Function(name, parameters, body);
    }

    private Stmt.Block block() {

        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Se esperaba '}' después del contenido del bloque");


        return new Stmt.Block(statements);
    }

    private Stmt.VarDeclaration varDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la variable.");


        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Se esperaba ';' después de la declaración de la variable.");

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
            }

            LinkLang.error(equals, "Solo puedes realizar asignaciones a variables o atributos.");
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

        return primary();
    }

    private Expr primary() {
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.NULL)) return new Expr.Literal(null);
        if (match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal());

        if (match(TokenType.IDENTIFIER)) {
            Token name = previous();
            Expr.Variable var = new Expr.Variable(name);
            if (match(TokenType.LEFT_PAREN)) {
                return finishCall(var);
            }
            return var;
        }


        // Ejemplo: (1 + 2) * 3
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de la expresión.");
            return new Expr.Group(expr);
        }

        throw error(peek(), "Se esperaba una expresión.");
    }

    private Expr finishCall(Expr.Variable callee) {
        List<Expr> arguments = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de los argumentos.");

        return new Expr.Call(callee, paren, arguments);
    }


    private Stmt statement() {
        if (match(TokenType.FOR)) return forStmt();
        if (match(TokenType.IF)) return ifStmt();
        if (match(TokenType.WHILE)) return whileStmt();
        if (match(TokenType.LEFT_BRACE)) return block();
        if (match(TokenType.PRINT)) return printSmt();
        if (match(TokenType.INPUT)) return inputStmt();
        if (match(TokenType.RETURN)) return returnStmt();

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

    private Stmt inputStmt() {
        Token keyword = previous();
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la variable en la cual guardar la entrada");
        Expr prompt = null;

        if (!check(TokenType.SEMICOLON)) {
            prompt = expression();
        }

        consume(TokenType.SEMICOLON, "Se esperaba ';' después de la sentencia de input");

        return new Stmt.Input(keyword, name, prompt);
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
        LinkLang.error(token, msg);
        return new RuntimeException();
    }

    private void sync() {
        advance();

        while (!isAtEnd()) {
            switch (peek().type()) {
                case FUNC:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                case LEFT_BRACE:
                    return;
            }

            advance();
        }
    }
}
