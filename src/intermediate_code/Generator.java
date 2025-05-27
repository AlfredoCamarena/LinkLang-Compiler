package intermediate_code;

import ast.Expr;
import ast.Stmt;
import ast.Visitor;
import semantic_analysis.ScopeManager;
import semantic_analysis.Symbol;
import semantic_analysis.SymbolType;

import java.util.List;

public class Generator implements Visitor<String> {
    private final ScopeManager scopeManager;
    private final QuadrupleManager quadManager;

    public Generator(ScopeManager scopeManager) {
        this.quadManager = new QuadrupleManager();
        this.scopeManager = scopeManager;
    }

    public List<Quadruple> generate(List<Stmt> statements) {
        for (Stmt statement : statements) {
            statement.accept(this);
        }
        return quadManager.getQuadruples();
    }

    @Override
    public String visit(Expr.Assignment expr) {
        if (expr.target instanceof Expr.Subscript subscript) {
            // Caso: arr[1] = value
            String arrayTemp = subscript.var.accept(this);
            String indexTemp = subscript.index.accept(this);
            String valueTemp = expr.value.accept(this);
            quadManager.addQuadruple(OpCode.ARRAY_STORE, arrayTemp, indexTemp, valueTemp);
            return valueTemp;
        } else {
            String value = expr.value.accept(this);
            quadManager.addQuadruple(OpCode.ASSIGN, value, null, ((Expr.Variable) expr.target).name.lexeme());
            return value;
        }
    }

    @Override
    public String visit(Expr.Array expr) {
        if (expr.size != null) {
            // Caso [size:fill]
            String sizeTemp = expr.size.accept(this);
            String fillTemp = expr.fillValue != null ? expr.fillValue.accept(this) : "0";
            String arrayTemp = quadManager.newTemp();
            quadManager.addQuadruple(OpCode.NEW_ARRAY, sizeTemp, fillTemp, arrayTemp);
            return arrayTemp;
        } else {
            // Caso [1,2,3]
            String arrayTemp = quadManager.newTemp();
            assert expr.values != null;
            quadManager.addQuadruple(OpCode.NEW_ARRAY, String.valueOf(expr.values.size()), "null", arrayTemp);

            for (int i = 0; i < expr.values.size(); i++) {
                String elementTemp = expr.values.get(i).accept(this);
                quadManager.addQuadruple(OpCode.ARRAY_STORE, arrayTemp, String.valueOf(i), elementTemp);
            }
            return arrayTemp;
        }
    }

    @Override
    public String visit(Expr.Logical expr) {
        return handleBinaryOperation(expr.left, expr.right, expr.operator.lexeme());
    }

    @Override
    public String visit(Expr.Binary expr) {
        // Optimización para operaciones con constantes
        if (expr.left instanceof Expr.Literal && expr.right instanceof Expr.Literal) {
            double leftVal = (Double) ((Expr.Literal) expr.left).value;
            double rightVal = (Double) ((Expr.Literal) expr.right).value;
            double result = switch (expr.operator.type()) {
                case PLUS -> leftVal + rightVal;
                case MINUS -> leftVal - rightVal;
                case STAR -> leftVal * rightVal;
                case SLASH -> leftVal / rightVal;
                default -> throw new UnsupportedOperationException();
            };
            return String.valueOf(result);
        }
        return handleBinaryOperation(expr.left, expr.right, expr.operator.lexeme());
    }

    @Override
    public String visit(Expr.Call expr) {
        // Procesa los argumentos
        for (int i = 0; i < expr.arguments.size(); i++) {
            String argValue = expr.arguments.get(i).accept(this);
            quadManager.addQuadruple(OpCode.ARG, argValue, String.valueOf(i), null);
        }

        // Determina si la función que se llama es nativa
        Symbol symbol = scopeManager.lookup(expr.callee.name);
        boolean isNative = (symbol != null && symbol.type == SymbolType.NATIVE_FUNCTION);

        // Procesa la llamada
        String temp = quadManager.newTemp();
        OpCode opCode = isNative ? OpCode.NATIVE_CALL : OpCode.CALL;
        quadManager.addQuadruple(opCode, expr.callee.name.lexeme(), String.valueOf(expr.arguments.size()), temp);

        return temp;
    }

    @Override
    public String visit(Expr.Group expr) {
        return expr.expression.accept(this);
    }

    @Override
    public String visit(Expr.Literal expr) {
        if (expr.value == null) {
            return null;
        }

        String valueLex;
        if (expr.value instanceof Double number && number % 1 == 0) {
            valueLex = String.valueOf((int) number.doubleValue());
        } else {
            valueLex = expr.value.toString();
        }

        return valueLex;
    }

    @Override
    public String visit(Expr.Subscript expr) {
        String arrayTemp = expr.var.accept(this);
        String indexTemp = expr.index.accept(this);
        String elementTemp = quadManager.newTemp();
        quadManager.addQuadruple(OpCode.ARRAY_LOAD, arrayTemp, indexTemp, elementTemp);
        return elementTemp;
    }

    @Override
    public String visit(Expr.Unary expr) {
        String right = expr.right.accept(this);
        String temp = quadManager.newTemp();
        OpCode opcode = getOpCode(expr.operator.lexeme());
        quadManager.addQuadruple(opcode, right, null, temp);
        return temp;
    }

    @Override
    public String visit(Expr.Variable expr) {
        return expr.name.lexeme();
    }

    @Override
    public String visit(Stmt.Function stmt) {
        // Procesa los parametros
        for (int i = 0; i < stmt.parameters.size(); i++) {
            String argValue = stmt.parameters.get(i).lexeme();
            quadManager.addQuadruple(OpCode.PARAM, argValue, String.valueOf(i), null);
        }

        // Comienzo de la función
        quadManager.addQuadruple(OpCode.FUNC, stmt.name.lexeme(), String.valueOf(stmt.parameters.size()), null);

        // Procesa el cuerpo de la función
        stmt.body.accept(this);

        // Agrega un return por defecto si no se encuentra
        if (quadManager.getQuadruples().getLast().op() != OpCode.RETURN) {
            quadManager.addQuadruple(OpCode.RETURN, null, null, null);
        }

        // Fin de la función
        quadManager.addQuadruple(OpCode.END_FUNC, stmt.name.lexeme(), null, null);
        return null;
    }

    @Override
    public String visit(Stmt.Block stmt) {
        for (Stmt s : stmt.statements) {
            s.accept(this);
        }
        return null;
    }

    @Override
    public String visit(Stmt.VarDeclaration stmt) {
        if (stmt.initializer != null) {
            String value = stmt.initializer.accept(this);
            quadManager.addQuadruple(OpCode.ASSIGN, value, null, stmt.name.lexeme());
        }
        return null;
    }

    @Override
    public String visit(Stmt.Expression stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override
    public String visit(Stmt.If stmt) { // TODO: Tal vez pueda agregar optimización
        String elseLabel = quadManager.newLabel();
        String endLabel = quadManager.newLabel();

        String condition = stmt.condition.accept(this);

        // Salto condicional al ELSE si la condición es falsa
        quadManager.addQuadruple(OpCode.IF_FALSE, condition, null, elseLabel);

        stmt.thenBranch.accept(this);

        // Salto al final (para evitar ejecutar el ELSE)
        quadManager.addQuadruple(OpCode.GOTO, null, null, endLabel);


        quadManager.addQuadruple(OpCode.LABEL, elseLabel, null, null);

        if (stmt.elseBranch != null) {
            stmt.elseBranch.accept(this);
        }

        quadManager.addQuadruple(OpCode.LABEL, endLabel, null, null);

        return null;
    }

    @Override
    public String visit(Stmt.While stmt) {
        String startLabel = quadManager.newLabel();
        String endLabel = quadManager.newLabel();

        // Marca el inicio del bucle
        quadManager.addQuadruple(OpCode.LABEL, startLabel, null, null);

        String condition = stmt.condition.accept(this);

        // Optimización: Bucle infinito
        if (condition.equals("true")) {
            stmt.body.accept(this);
            quadManager.addQuadruple(OpCode.GOTO, null, null, startLabel);
            return null;
        }

        // Salto condicional al final si es falso
        quadManager.addQuadruple(OpCode.IF_FALSE, condition, null, endLabel);

        stmt.body.accept(this);

        // Salto incondicional al inicio
        quadManager.addQuadruple(OpCode.GOTO, null, null, startLabel);

        // Marca el final del bucle
        quadManager.addQuadruple(OpCode.LABEL, endLabel, null, null);

        return null;
    }

    @Override
    public String visit(Stmt.Return stmt) {
        String value = stmt.value != null ? stmt.value.accept(this) : null;
        quadManager.addQuadruple(OpCode.RETURN, value, null, null);
        return null;
    }

    @Override
    public String visit(Stmt.Print stmt) {
        String value = stmt.expression.accept(this);
        quadManager.addQuadruple(OpCode.PRINT, value, null, null);
        return null;
    }

    @Override
    public String visit(Stmt.Input stmt) {
        String prompt = stmt.prompt != null ? stmt.prompt.accept(this) : "null";
        quadManager.addQuadruple(OpCode.INPUT, prompt, null, stmt.name.lexeme());
        return null;
    }

    private String handleBinaryOperation(Expr left, Expr right, String operator) {
        String leftVal = left.accept(this);
        String rightVal = right.accept(this);
        String temp = quadManager.newTemp();
        quadManager.addQuadruple(getOpCode(operator), leftVal, rightVal, temp);
        return temp;
    }

    private OpCode getOpCode(String operatorLex) {
        return switch (operatorLex) {
            case "+" -> OpCode.ADD;
            case "-" -> OpCode.SUB;
            case "*" -> OpCode.MULTIPLY;
            case "/" -> OpCode.DIVIDE;
            case "<" -> OpCode.LESS;
            case "<=" -> OpCode.LESS_EQUAL;
            case ">" -> OpCode.GREATER;
            case ">=" -> OpCode.GREATER_EQUAL;
            case "and" -> OpCode.AND;
            case "or" -> OpCode.OR;
            case "!" -> OpCode.NOT;
            default -> null;
        };
    }
}