package semantic_analysis;

import ast.Expr;
import ast.Stmt;
import ast.Visitor;
import main.GSD;
import scanner.Token;
import scanner.TokenType;

import java.util.List;

public class SemanticAnalyzer implements Visitor<Void> {
    private final SymbolTable symbolTable = new SymbolTable();
    private boolean isInsideFunction = false;

    public SymbolTable analyze(List<Stmt> statements) {
        for (Stmt statement : statements) {
            statement.accept(this);
        }
        return symbolTable;
    }

    @Override
    public Void visit(Expr.Assignment expr) {
        checkDeclared(expr.name);
        expr.value.accept(this);
        return null;
    }

    @Override
    public Void visit(Expr.Logical expr) {
        expr.left.accept(this);
        expr.right.accept(this);
        return null;
    }

    @Override
    public Void visit(Expr.Binary expr) {
        checkValidBinaryOperands(expr);
        expr.left.accept(this);
        expr.right.accept(this);
        return null;
    }

    @Override
    public Void visit(Expr.Call expr) {
        expr.callee.accept(this);
        checkFuncArguments(expr.callee, expr.arguments);
        for (Expr argument : expr.arguments) {
            argument.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Expr.Group expr) {
        expr.expression.accept(this);
        return null;
    }

    @Override
    public Void visit(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visit(Expr.Unary expr) {
        checkValidUnaryOperand(expr);
        expr.right.accept(this);
        return null;
    }

    @Override
    public Void visit(Expr.Variable expr) {
        checkDeclared(expr.name);
        return null;
    }

    @Override
    public Void visit(Stmt.Function stmt) {
        if (checkDoubleDecl(stmt.name, "función")) {
            return null;
        }

        symbolTable.enterScope();

        boolean prevContext = isInsideFunction;
        isInsideFunction = true;

        for (Token param : stmt.parameters) {
            symbolTable.define(param, TokenType.VAR, new Stmt.VarDeclaration(param, null));
        }

        stmt.body.accept(this);

        isInsideFunction = prevContext;

        symbolTable.exitScope();
        symbolTable.define(stmt.name, TokenType.FUNC, stmt);
        return null;
    }

    @Override
    public Void visit(Stmt.Block stmt) {
        symbolTable.enterScope();
        for (Stmt statement : stmt.statements) {
            statement.accept(this);
        }
        symbolTable.exitScope();
        return null;
    }

    @Override
    public Void visit(Stmt.VarDeclaration stmt) {
        if (checkDoubleDecl(stmt.name, "variable")) {
            return null;
        }

        if (stmt.initializer != null) {
            stmt.initializer.accept(this);
        }
        symbolTable.define(stmt.name, TokenType.VAR, stmt);
        return null;
    }

    @Override
    public Void visit(Stmt.Expression stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visit(Stmt.If stmt) {
        stmt.condition.accept(this);
        stmt.thenBranch.accept(this);
        if (stmt.elseBranch != null) {
            stmt.elseBranch.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Stmt.While stmt) {
        stmt.condition.accept(this);
        stmt.body.accept(this);
        return null;
    }

    @Override
    public Void visit(Stmt.Return stmt) {
        if (!isInsideFunction) {
            GSD.error(stmt.keyword, "La sentencia 'Return' solo pude usarse dentro de una función.");
        }

        if (stmt.value != null) {
            stmt.value.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Stmt.Print stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visit(Stmt.Connect stmt) {
        stmt.ssid.accept(this);
        if (stmt.password != null) {
            stmt.password.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Stmt.Disconnect stmt) {
        return null;
    }

    private Boolean checkDoubleDecl(Token name, String entityType) {
        Symbol existingSymbol = symbolTable.lookup(name);
        if (existingSymbol != null) {
            GSD.error(name, "La " + entityType + " '" + name.lexeme() + "' ya fue declarada en la línea " + existingSymbol.token().line());
            return true;
        }
        return false;
    }

    private void checkDeclared(Token name) {
        Symbol symbol = symbolTable.lookup(name);
        if (symbol == null) {
            GSD.error(name, "Asegurate de declarar '" + name.lexeme() + "' antes de usarla.");
        }
    }

    private void checkFuncArguments(Expr callee, List<Expr> arguments) {
        Token name = ((Expr.Variable) callee).name;
        Symbol symbol = symbolTable.lookup(name);
        if (symbol != null && symbol.type() == TokenType.FUNC) {
            Stmt.Function function = (Stmt.Function) symbol.statement();
            if (arguments.size() != function.parameters.size()) {
                GSD.error(name, "Número incorrecto de argumentos para la función '" + name.lexeme() + "'. Se esperaban " + function.parameters.size() + " argumentos.");
            }
        }
    }

    private void checkValidUnaryOperand(Expr.Unary expr) {
        TokenType operandType = getType(expr.right);

        if (operandType == null) {
            return;
        }

        switch (expr.operator.type()) {
            case MINUS:
                if (operandType != TokenType.NUMBER) {
                    GSD.error(expr.operator, "El operador '" + expr.operator.lexeme() + "' solo trabaja con números");
                }
                break;

            case BANG:
                if (operandType != TokenType.TRUE && operandType != TokenType.FALSE) {
                    GSD.error(expr.operator, "El operador '" + expr.operator.lexeme() + "' solo trabaja con booleanos");
                }
                break;
        }
    }

    private void checkValidBinaryOperands(Expr.Binary expr) {
        TokenType leftType = getType(expr.left);
        TokenType rightType = getType(expr.right);

        if (leftType == null || rightType == null) {
            return;
        }

        switch (expr.operator.type()) {
            case PLUS:
                if ((leftType == TokenType.STRING && rightType == TokenType.STRING) ||
                        (leftType == TokenType.NUMBER && rightType == TokenType.NUMBER)) {
                    return;
                }
                GSD.error(expr.operator, "No puedes sumar una cadena con un número.");
                break;

            case STAR:
            case MINUS:
            case SLASH:
                if (leftType == TokenType.NUMBER && rightType == TokenType.NUMBER) {
                    return;
                }
                GSD.error(expr.operator, "El operador '" + expr.operator.lexeme() + "' solo trabaja con números.");
                break;
        }
    }

    private TokenType getType(Expr expr) {
        if (expr instanceof Expr.Literal) {
            Object value = ((Expr.Literal) expr).value;
            if (value instanceof Double) return TokenType.NUMBER;
            if (value instanceof Boolean) return (boolean) value ? TokenType.TRUE : TokenType.FALSE;
            if (value instanceof String) return TokenType.STRING;
        } else if (expr instanceof Expr.Variable) {
            Symbol symbol = symbolTable.lookup(((Expr.Variable) expr).name);
            if (symbol != null) {
                if (symbol.type() == TokenType.VAR) {
                    Stmt.VarDeclaration variable = (Stmt.VarDeclaration) symbol.statement();
                    if (variable.initializer != null) {
                        return getType(variable.initializer);
                    } else {
                        GSD.error(symbol.token(), "La variable '" + symbol.token().lexeme() + "' no fue inicializada");
                        return null;
                    }
                }
            }
        }
        return null;
    }
}