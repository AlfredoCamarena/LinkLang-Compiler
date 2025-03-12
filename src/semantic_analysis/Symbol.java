package semantic_analysis;

import ast.Stmt;
import scanner.Token;
import scanner.TokenType;

public record Symbol(Token token, TokenType type, Stmt statement, ScopeManager.ScopeType scopeType) {
    @Override
    public String toString() {
        return token.lexeme() + ": " +
                "Type=" + type +
                ", Sentencia=" + statement +
                ", Scope=" + scopeType;
    }
}

