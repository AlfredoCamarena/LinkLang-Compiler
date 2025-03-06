package semantic_analysis;

import scanner.Token;
import scanner.TokenType;

public record Symbol(Token token, TokenType type, Object value, SymbolTable.ScopeType scopeType) {
    @Override
    public String toString() {
        return token.lexeme() + ": " +
                "Type=" + type +
                ", Value=" + value +
                ", Scope=" + scopeType;
    }
}

