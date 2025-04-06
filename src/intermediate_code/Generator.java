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
        String value = expr.value.accept(this);
        quadManager.addQuadruple(OpCode.ASSIGN, value, null, expr.name.lexeme());
        return value;
    }

    @Override
    public String visit(Expr.Logical expr) {
        return handleBinaryOperation(expr.left, expr.right, expr.operator.lexeme());
    }

    @Override
    public String visit(Expr.Binary expr) {
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
        return expr.value == null ? "null" : expr.value.toString();
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
    public String visit(Stmt.If stmt) {
        String condition = stmt.condition.accept(this);

        // Agrega un salto condicional (con un target pendiente)
        int ifJumpIndex = quadManager.getQuadruples().size();
        quadManager.addQuadruple(OpCode.IF_FALSE, condition, null, "?");

        // Procesa la rama then
        stmt.thenBranch.accept(this);

        // Agrega un salto incondicional (con target pendiente)
        int elseJumpIndex = quadManager.getQuadruples().size();
        quadManager.addQuadruple(OpCode.GOTO, null, null, "?");

        updateJumpTarget(ifJumpIndex, elseJumpIndex + 1);

        // Procesa la rama else
        if (stmt.elseBranch != null) {
            stmt.elseBranch.accept(this);
        }

        updateJumpTarget(elseJumpIndex, quadManager.getQuadruples().size());

        return null;
    }

    @Override
    public String visit(Stmt.While stmt) {
        int loopStart = quadManager.getQuadruples().size();
        String condition = stmt.condition.accept(this);

        // Agrega salto condicional (con target pendiente)
        int whileJumpIndex = quadManager.getQuadruples().size();
        quadManager.addQuadruple(OpCode.IF_FALSE, condition, null, "?");

        // Procesa el cuerpo del ciclo
        stmt.body.accept(this);

        // Agrega salto a la revisión de la condición
        quadManager.addQuadruple(OpCode.GOTO, null, null, String.valueOf(loopStart));

        updateJumpTarget(whileJumpIndex, quadManager.getQuadruples().size());

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

    private void updateJumpTarget(int quadIndex, int target) {
        Quadruple quad = quadManager.getQuadruples().get(quadIndex);
        quadManager.getQuadruples().set(quadIndex,
                new Quadruple(quad.op(), quad.arg1(), quad.arg2(), String.valueOf(target)));
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
            default -> null;
        };
    }
}