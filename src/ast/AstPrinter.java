package ast;

import scanner.Token;

public class AstPrinter implements Visitor<String> {

    private int indentLevel = 0;

    @Override
    public String visit(Expr.Assignment expr) {
        return parenthesize("= " + expr.name.lexeme(), expr.value);
    }

    @Override
    public String visit(Expr.Array expr) {
        return ""; //TODO
    }

    @Override
    public String visit(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme(), expr.left, expr.right);
    }

    @Override
    public String visit(Expr.Call expr) {
        return parenthesize("call", expr.callee) + " " + parenthesize("args", expr.arguments.toArray(new Expr[0]));
    }

    @Override
    public String visit(Expr.Group expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visit(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visit(Expr.Subscript expr) {
        return ""; //TODO
    }

    @Override
    public String visit(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme(), expr.left, expr.right);
    }

    @Override
    public String visit(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme(), expr.right);
    }

    @Override
    public String visit(Expr.Variable expr) {
        return expr.name.lexeme();
    }

    @Override
    public String visit(Stmt.Block stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(block\n");
        indentLevel++;
        for (Stmt statement : stmt.statements) {
            builder.append(indent()).append(statement.accept(this)).append("\n");
        }
        indentLevel--;
        builder.append(indent()).append(")");
        return builder.toString();
    }

    @Override
    public String visit(Stmt.Expression stmt) {
        return parenthesize("expr", stmt.expression);
    }

    @Override
    public String visit(Stmt.Function stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(func ").append(stmt.name.lexeme()).append(" (");
        for (Token param : stmt.parameters) {
            builder.append(param.lexeme()).append(" ");
        }
        builder.append(")\n");
        indentLevel++;
        builder.append(indent()).append(stmt.body.accept(this)).append("\n");
        indentLevel--;
        builder.append(indent()).append(")");
        return builder.toString();
    }

    @Override
    public String visit(Stmt.If stmt) {
        if (stmt.elseBranch == null) {
            return parenthesize("if", stmt.condition, stmt.thenBranch);
        }
        return parenthesize("if-else", stmt.condition, stmt.thenBranch, stmt.elseBranch);
    }

    @Override
    public String visit(Stmt.Print stmt) {
        return parenthesize("print", stmt.expression);
    }

    @Override
    public String visit(Stmt.Input stmt) {
        return ""; // TODO
    }

    @Override
    public String visit(Stmt.Return stmt) {
        if (stmt.value == null) return "(return)";
        return parenthesize("return", stmt.value);
    }

    @Override
    public String visit(Stmt.VarDeclaration stmt) {
        if (stmt.initializer == null) {
            return "(var " + stmt.name.lexeme() + ")";
        }
        return parenthesize("var " + stmt.name.lexeme(), stmt.initializer);
    }

    @Override
    public String visit(Stmt.While stmt) {
        return parenthesize("while", stmt.condition, stmt.body);
    }

    private String parenthesize(String name, Node... nodes) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Node node : nodes) {
            builder.append(" ");
            builder.append(node.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    private String indent() {
        return "  ".repeat(indentLevel);
    }
}