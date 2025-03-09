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
    public Void visit(Expr.Get expr) {
        expr.object.accept(this);
        return null;
    }

    @Override
    public Void visit(Expr.Set expr) {
        expr.object.accept(this);
        expr.value.accept(this);
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
    public Void visit(Expr.Super expr) {
        return null;
    }

    @Override
    public Void visit(Expr.This expr) {
        return null;
    }

    @Override
    public Void visit(Expr.Unary expr) {
        expr.right.accept(this);
        return null;
    }

    @Override
    public Void visit(Expr.Variable expr) {
        checkDeclared(expr.name);
        return null;
    }

    @Override
    public Void visit(Stmt.Class stmt) {
        checkDoubleDecl(stmt.name, "clase");
        symbolTable.enterScope();

        if (stmt.superclass != null) {
            stmt.superclass.accept(this);
        }

        for (Stmt.VarDeclaration varDecl : stmt.variables) {
            varDecl.accept(this);
        }

        for (Stmt.Function method : stmt.methods) {
            method.accept(this);
        }

        symbolTable.exitScope();
        symbolTable.define(stmt.name, TokenType.CLASS, stmt);
        return null;
    }

    @Override
    public Void visit(Stmt.Function stmt) {
        checkDoubleDecl(stmt.name, "función");
        symbolTable.enterScope();

        for (Token param : stmt.parameters) {
            symbolTable.define(param, TokenType.VAR, new Stmt.VarDeclaration(param, null));
        }

        stmt.body.accept(this);

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
        checkDoubleDecl(stmt.name, "variable");
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

    private void checkDoubleDecl(Token name, String entityType) {
        Symbol existingSymbol = symbolTable.lookup(name);
        if (existingSymbol != null) {
            GSD.error(name, "La " + entityType + " '" + name.lexeme() + "' ya fue declarada en la línea " + existingSymbol.token().line());
        }
    }

    private void checkDeclared(Token name) {
        Symbol symbol = symbolTable.lookup(name);
        if (symbol == null) {
            GSD.error(name, "Asegurate de declarar '" + name.lexeme() + "'.");
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

}