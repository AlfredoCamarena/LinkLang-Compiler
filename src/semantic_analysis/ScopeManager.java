package semantic_analysis;

import ast.Expr;
import ast.Stmt;
import scanner.Token;
import scanner.TokenType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class ScopeManager {
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public ScopeManager() {
        enterScope(); // Ámbito global
        defineNativeFunctions();
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }

    public void defineNativeFunctions() {
        defineNativeFunction("_wifi_connect");
        defineNativeFunction("_wifi_disconnect");
        defineNativeFunction("_wifi_scan");
        defineNativeFunction("_wifi_signal_strength");
        defineNativeFunction("_wifi_saved_networks");
        defineNativeFunction("_hotspot_create");
        defineNativeFunction("_hotspot_stop");
        defineNativeFunction("_hotspot_status");
    }

    public void defineNativeFunction(String funcName) {
        define(new Token(TokenType.IDENTIFIER, funcName, null, -1), SymbolType.NATIVE_FUNCTION, null, null);
    }

    public void define(Token name, SymbolType type, Expr value, Stmt statement) {
        Map<String, Symbol> currentScope = scopes.peek();

        assert currentScope != null;
        currentScope.put(name.lexeme(), new Symbol(name, type, value, statement,
                scopes.size() == 1 ? ScopeType.GLOBAL : ScopeType.LOCAL));
    }

    public void assignValue(Token name, Expr value) {
        Symbol symbol = lookup(name);
        symbol.setValue(value);
    }

    public Symbol lookup(Token name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol symbol = scope.get(name.lexeme());
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }

    public Symbol lookupLocal(Token name) {
        assert scopes.peek() != null;
        return scopes.peek().get(name.lexeme());
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
}