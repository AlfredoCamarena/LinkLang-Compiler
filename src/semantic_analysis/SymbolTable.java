package semantic_analysis;

import main.GSD;
import scanner.Token;
import scanner.TokenType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public SymbolTable() {
        enterScope(); // Ámbito global
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }

    public void define(Token name, TokenType type, Object value) {
        Map<String, Symbol> currentScope = scopes.peek();

        assert currentScope != null;
        if (currentScope.containsKey(name.lexeme())) {
            GSD.error(name.line(), "La variable '" + name.lexeme() + "' ya se encuentra definida");
        }

        currentScope.put(name.lexeme(), new Symbol(name, type, value,
                scopes.size() == 1 ? ScopeType.GLOBAL : ScopeType.LOCAL));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Symbol Table:\n");
        int scopeLevel = scopes.size();
        for (Map<String, Symbol> scope : scopes) {
            sb.append("Scope Level ").append(scopeLevel).append(":\n");
            for (Map.Entry<String, Symbol> entry : scope.entrySet()) {
                sb.append("  ").append(entry.getValue().toString()).append("\n");
            }
            scopeLevel--;
        }
        return sb.toString();
    }

    public enum ScopeType {
        GLOBAL, LOCAL
    }

    public record Symbol(Token token, TokenType type, Object value, ScopeType scopeType) {
        @Override
        public String toString() {
            return token.lexeme() + ": " +
                    "Type=" + type +
                    ", Value=" + value +
                    ", Scope=" + scopeType;
        }
    }
}