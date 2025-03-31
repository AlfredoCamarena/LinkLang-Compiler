package semantic_analysis;

import ast.Expr;
import ast.Stmt;
import scanner.Token;

public final class Symbol {
    public final Token token;
    public final SymbolType type;
    public final Stmt statement;
    public final ScopeManager.ScopeType scopeType;
    public Expr value;

    public Symbol(Token token, SymbolType type, Expr value, Stmt statement, ScopeManager.ScopeType scopeType) {
        this.token = token;
        this.type = type;
        this.value = value;
        this.statement = statement;
        this.scopeType = scopeType;
    }

    @Override
    public String toString() {
        return token.lexeme() + ": " +
                "Type=" + type +
                ", Value=" + value +
                ", Sentencia=" + statement +
                ", Scope=" + scopeType;
    }

    public void setValue(Expr value) {
        this.value = value;
    }

}

