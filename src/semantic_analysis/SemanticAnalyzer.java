package semantic_analysis;

import ast.Expr;
import ast.Stmt;
import ast.Visitor;
import main.LinkLang;
import scanner.Token;
import scanner.TokenType;

import java.util.List;

public class SemanticAnalyzer implements Visitor<Void> {
    private final ScopeManager scopeManager = new ScopeManager();
    private boolean isInsideFunction = false;

    public ScopeManager analyze(List<Stmt> statements) {
        for (Stmt statement : statements) {
            statement.accept(this);
        }
        return scopeManager;
    }

    @Override
    public Void visit(Expr.Assignment expr) {
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
        checkValidBinaryOperands(expr);
        return null;
    }

    @Override
    public Void visit(Expr.Call expr) {
        expr.callee.accept(this);

        Token name = expr.callee.name;
        Symbol calleeSym = scopeManager.lookup(name);
        if (calleeSym != null && calleeSym.type() != SymbolType.FUNCTION) {
            LinkLang.error(name, "Solo puedes llamar a funciones, '" + name.lexeme() + "' no es una función.");
        }

        for (Expr argument : expr.arguments) {
            argument.accept(this);
        }
        checkArgumentsCount(expr.callee, expr.arguments);
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
        expr.right.accept(this);
        checkValidUnaryOperand(expr);
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

        scopeManager.enterScope();

        boolean prevContext = isInsideFunction;
        isInsideFunction = true;

        for (Token param : stmt.parameters) {
            scopeManager.define(param, SymbolType.PARAMETER, new Stmt.VarDeclaration(param, null));
        }

        stmt.body.accept(this);

        isInsideFunction = prevContext;

        scopeManager.exitScope();
        scopeManager.define(stmt.name, SymbolType.FUNCTION, stmt);
        return null;
    }

    @Override
    public Void visit(Stmt.Block stmt) {
        scopeManager.enterScope();
        for (Stmt statement : stmt.statements) {
            statement.accept(this);
        }
        scopeManager.exitScope();
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
        scopeManager.define(stmt.name, SymbolType.VARIABLE, stmt);
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
            LinkLang.error(stmt.keyword, "La sentencia 'return' solo pude usarse dentro de una función.");
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
    public Void visit(Stmt.Input stmt) {
        if (checkDoubleDecl(stmt.name, "variable")) {
            return null;
        }

        if (stmt.prompt != null) {
            stmt.prompt.accept(this);

            if (getType(stmt.prompt) != TokenType.STRING) {
                LinkLang.error(stmt.keyword, "Se esperaba un String para el mensaje del input");
            }
        }

        scopeManager.define(stmt.name, SymbolType.VARIABLE, stmt);
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
        Symbol existingSymbol = scopeManager.lookupLocal(name);
        if (existingSymbol != null) {
            if (existingSymbol.token().line() == -1) {
                LinkLang.error(name, "Existe una función nativa llamada '" + name.lexeme() + "', no puedes crear una función con ese nombre.");
            } else {
                LinkLang.error(name, "La " + entityType + " '" + name.lexeme() + "' ya fue declarada en la línea " + existingSymbol.token().line());
            }
            return true;
        }
        return false;
    }

    private void checkDeclared(Token name) {
        Symbol symbol = scopeManager.lookup(name);
        if (symbol == null) {
            LinkLang.error(name, "Asegurate de declarar '" + name.lexeme() + "' antes de usarla.");
        }
    }

    private void checkArgumentsCount(Expr.Variable callee, List<Expr> arguments) {
        Token name = callee.name;
        Symbol symbol = scopeManager.lookup(name);
        if (symbol != null && symbol.type() == SymbolType.FUNCTION) {
            Stmt.Function function = (Stmt.Function) symbol.statement();
            if (arguments.size() != function.parameters.size()) {
                argumentsError(name, function.parameters.size());
            }
        } else if (symbol != null && symbol.type() == SymbolType.NATIVE_FUNCTION) {
            int argumentsCount = switch (name.lexeme()) {
                case "_wifi_signal_strength" -> 1;
                case "_wifi_connect", "_hotspot_create" -> 2;
                default -> 0;
            };
            if (arguments.size() != argumentsCount) {
                argumentsError(name, argumentsCount);
            }
        }
    }

    private void argumentsError(Token name, int argumentsCount) {
        LinkLang.error(name, "Número incorrecto de argumentos para la función '" + name.lexeme() + "'. Se esperaban " + argumentsCount + " argumentos.");
    }

    private void checkValidUnaryOperand(Expr.Unary expr) {
        TokenType operandType = getType(expr.right);

        if (operandType == null) {
            return;
        }

        switch (expr.operator.type()) {
            case MINUS:
                if (operandType != TokenType.NUMBER) {
                    LinkLang.error(expr.operator, "El operador '" + expr.operator.lexeme() + "' solo trabaja con números");
                }
                break;

            case BANG:
                if (operandType != TokenType.TRUE && operandType != TokenType.FALSE) {
                    LinkLang.error(expr.operator, "El operador '" + expr.operator.lexeme() + "' solo trabaja con booleanos");
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
                LinkLang.error(expr.operator, "No puedes sumar una cadena con un número.");
                break;

            case STAR:
            case MINUS:
            case SLASH:
                if (leftType == TokenType.NUMBER && rightType == TokenType.NUMBER) {
                    return;
                }
                LinkLang.error(expr.operator, "El operador '" + expr.operator.lexeme() + "' solo trabaja con números.");
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
            if (isInsideFunction)
                return null;
            Symbol symbol = scopeManager.lookup(((Expr.Variable) expr).name);
            if (symbol != null) {
                if (symbol.type() == SymbolType.VARIABLE) {
                    Stmt.VarDeclaration variable = (Stmt.VarDeclaration) symbol.statement();
                    if (variable.initializer != null) {
                        return getType(variable.initializer);
                    } else {
                        LinkLang.error(symbol.token(), "La variable '" + symbol.token().lexeme() + "' no fue inicializada");
                        return null;
                    }
                }
            }
        }
        return null;
    }
}