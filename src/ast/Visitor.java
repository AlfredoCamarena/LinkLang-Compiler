package ast;

public interface Visitor<R> {
    R visit(Expr.Assignment expr);

    R visit(Expr.Logical expr);

    R visit(Expr.Binary expr);

    R visit(Expr.Call expr);

    R visit(Expr.Group expr);

    R visit(Expr.Literal expr);

    R visit(Expr.Unary expr);

    R visit(Expr.Variable expr);

    // statements

    R visit(Stmt.Function stmt);

    R visit(Stmt.Block stmt);

    R visit(Stmt.VarDeclaration stmt);

    R visit(Stmt.Expression stmt);

    R visit(Stmt.If stmt);

    R visit(Stmt.While stmt);

    R visit(Stmt.Return stmt);

    R visit(Stmt.Print stmt);

    R visit(Stmt.Input stmt);

    R visit(Stmt.Connect stmt);

    R visit(Stmt.Disconnect stmt);
}
