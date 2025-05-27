package semantic_analysis;

import ast.Expr;
import ast.Stmt;
import ast.Visitor;
import main.LinkLang;
import scanner.Token;

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
        // 1. Analizar el valor de la asignación
        expr.value.accept(this);

        // 2. Verificar el target (variable o array)
        if (expr.target instanceof Expr.Variable) {
            // Caso: x = 5
            Token varName = ((Expr.Variable) expr.target).name;
            if (checkDeclared(varName)) {
                scopeManager.assignValue(varName, expr.value);
            }
        } else if (expr.target instanceof Expr.Subscript subscript) {
            // Caso: arr[1] = 5
            if (checkDeclared(subscript.var.name)) {
                subscript.index.accept(this);
                // No se necesita assignValue para arrays, ya que la modificación se maneja en ejecución
            }
        }
        return null;
    }

    @Override
    public Void visit(Expr.Array expr) {
        SemanticType elementType = null;

        // Caso 1: Array con tamaño explícito [size:default]
        if (expr.size != null) {
            expr.size.accept(this);

            SemanticType sizeType = getType(expr.size);
            if (sizeType != SemanticType.NUMBER) {
                LinkLang.error(expr.name, "El tamaño del array debe ser un número");
            }

            if (expr.fillValue != null) {
                expr.fillValue.accept(this);
            }
        }

        // Validar homogeneidad en arrays literales [1,2,3]
        if (expr.values != null) {
            for (Expr value : expr.values) {
                if (value != null) {
                    value.accept(this);

                    SemanticType currentType = getType(value);
                    if (elementType == null) {
                        elementType = currentType; // Primer elemento define el tipo esperado
                    } else if (elementType != currentType) {
                        LinkLang.error(expr.name, "Todos los elementos del array deben ser del mismo tipo.");
                    }
                }
            }
        }
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
        if (calleeSym != null && calleeSym.type != SymbolType.FUNCTION && calleeSym.type != SymbolType.NATIVE_FUNCTION) {
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
    public Void visit(Expr.Subscript expr) {
        expr.var.accept(this);
        expr.index.accept(this);

        Token arrayToken = expr.var.name;
        Symbol arraySymbol = scopeManager.lookup(arrayToken);
        if (arraySymbol == null) {
            return null;
        }

        if (arraySymbol.type != SymbolType.ARRAY) {
            LinkLang.error(arrayToken, "La variable '" + arrayToken.lexeme() + "' no es un arreglo.");
            return null;
        }

        if (getType(expr.index) != SemanticType.NUMBER) {
            LinkLang.error(arrayToken, "El índice del arreglo debe ser un número.");
            return null;
        }

        // Validar el rango del índice (solo para literales numéricos)
        if (expr.index instanceof Expr.Literal) {
            Expr.Array array = (Expr.Array) arraySymbol.value;
            int arraySize = array.values != null ? array.values.size() :
                    array.size instanceof Expr.Literal ?
                            ((Double) ((Expr.Literal) array.size).value).intValue() : -1;

            if (arraySize >= 0) {
                try {
                    double indexValue = (Double) ((Expr.Literal) expr.index).value;
                    if (indexValue < 0 || indexValue >= arraySize) {
                        LinkLang.error(arrayToken,
                                "Índice fuera de rango. El índice máximo es " + (arraySize - 1) + ".");
                    }
                } catch (ClassCastException e) {
                    LinkLang.error(arrayToken, "El índice debe ser un número entero.");
                }
            }
        }

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
        checkInit(expr.name);
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
            scopeManager.define(param, SymbolType.PARAMETER, null, new Stmt.VarDeclaration(param, null));
        }

        stmt.body.accept(this);

        isInsideFunction = prevContext;

        scopeManager.exitScope();
        scopeManager.define(stmt.name, SymbolType.FUNCTION, null, stmt);
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

        SymbolType type = stmt.initializer instanceof Expr.Variable ? SymbolType.VARIABLE : SymbolType.ARRAY;
        scopeManager.define(stmt.name, type, stmt.initializer, stmt);
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

            if (getType(stmt.prompt) != SemanticType.STRING) {
                LinkLang.error(stmt.keyword, "Se esperaba un String para el mensaje del input");
            }
        }

        scopeManager.define(stmt.name, SymbolType.VARIABLE, null, stmt);
        return null;
    }

    private Boolean checkDoubleDecl(Token name, String entityType) {
        Symbol existingSymbol = scopeManager.lookupLocal(name);
        if (existingSymbol == null) return false;

        if (existingSymbol.token.line() == -1) {
            LinkLang.error(name, "Existe una función nativa llamada '" + name.lexeme() + "', no puedes crear una función con ese nombre.");
        } else {
            LinkLang.error(name, "La " + entityType + " '" + name.lexeme() + "' ya fue declarada en la línea " + existingSymbol.token.line());
        }
        return true;
    }

    private Boolean checkDeclared(Token name) {
        Symbol symbol = scopeManager.lookup(name);
        if (symbol == null) {
            LinkLang.error(name, "Asegurate de declarar '" + name.lexeme() + "' antes de usarla.");
            return false;
        }
        return true;
    }

    private void checkInit(Token name) {
        Symbol symbol = scopeManager.lookup(name);
        if (symbol != null && symbol.value == null && symbol.type != SymbolType.FUNCTION && symbol.type != SymbolType.NATIVE_FUNCTION) {
            LinkLang.error(name, "La variable '" + symbol.token.lexeme() + "' no fue inicializada");
        }
    }

    private void checkArgumentsCount(Expr.Variable callee, List<Expr> arguments) {
        Token name = callee.name;
        Symbol symbol = scopeManager.lookup(name);
        if (symbol == null) return;

        switch (symbol.type) {
            case FUNCTION -> {
                Stmt.Function function = (Stmt.Function) symbol.statement;
                if (arguments.size() != function.parameters.size()) {
                    argumentsError(name, function.parameters.size());
                }
            }
            case NATIVE_FUNCTION -> {
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
    }

    private void argumentsError(Token name, int argumentsCount) {
        LinkLang.error(name, "Número incorrecto de argumentos para la función '" + name.lexeme() + "'. Se esperaban " + argumentsCount + " argumentos.");
    }

    private void checkValidUnaryOperand(Expr.Unary expr) {
        SemanticType operandType = getType(expr.right);

        if (operandType == null) {
            return;
        }

        switch (expr.operator.type()) {
            case MINUS:
                if (operandType != SemanticType.NUMBER) {
                    LinkLang.error(expr.operator, "El operador '" + expr.operator.lexeme() + "' solo trabaja con números");
                }
                break;

            case BANG:
                if (operandType != SemanticType.TRUE && operandType != SemanticType.FALSE) {
                    LinkLang.error(expr.operator, "El operador '" + expr.operator.lexeme() + "' solo trabaja con booleanos");
                }
                break;
        }
    }

    private void checkValidBinaryOperands(Expr.Binary expr) {
        SemanticType leftType = getType(expr.left);
        SemanticType rightType = getType(expr.right);

        if (leftType == null || rightType == null) {
            return;
        }

        // Bloquear operaciones inválidas con arrays temporalmente
        if (leftType == SemanticType.ARRAY || rightType == SemanticType.ARRAY) {
            LinkLang.error(expr.operator, "Operación no soportada para arrays.");
        }

        switch (expr.operator.type()) { // TODO mejorar y agregar arreglos
            case PLUS:
                if (leftType == rightType) {
                    return;
                }
                LinkLang.error(expr.operator, "No puedes sumar una cadena con un número.");
                break;

            case STAR:
            case MINUS:
            case SLASH:
                if (leftType == SemanticType.NUMBER && rightType == SemanticType.NUMBER) {
                    return;
                }
                LinkLang.error(expr.operator, "El operador '" + expr.operator.lexeme() + "' solo trabaja con números.");
                break;
        }
    }

    private SemanticType getType(Expr expr) {
        if (expr instanceof Expr.Literal literal) {
            Object value = literal.value;
            if (value instanceof Double) return SemanticType.NUMBER;
            if (value instanceof Boolean) return (boolean) value ? SemanticType.TRUE : SemanticType.FALSE;
            if (value instanceof String) return SemanticType.STRING;
        }

        if (expr instanceof Expr.Call) {
            return SemanticType.CALL;
        }

        if (expr instanceof Expr.Array) {
            return SemanticType.ARRAY;
        }

        if (expr instanceof Expr.Variable variable) {
            Symbol symbol = scopeManager.lookup(variable.name);
            if (symbol == null || symbol.type == SymbolType.PARAMETER) return null;

            Expr value = symbol.value;
            if (value != null) {
                return getType(value);
            }
        }
        return null;
    }

    public enum SemanticType {
        NUMBER,
        TRUE,
        FALSE,
        STRING,
        ARRAY,
        CALL,
    }
}