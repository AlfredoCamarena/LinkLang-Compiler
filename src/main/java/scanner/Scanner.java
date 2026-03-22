package scanner;

import main.LinkLang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", TokenType.AND);
        keywords.put("else", TokenType.ELSE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("func", TokenType.FUNC);
        keywords.put("for", TokenType.FOR);
        keywords.put("if", TokenType.IF);
        keywords.put("null", TokenType.NULL);
        keywords.put("or", TokenType.OR);
        keywords.put("return", TokenType.RETURN);
        keywords.put("true", TokenType.TRUE);
        keywords.put("var", TokenType.VAR);
        keywords.put("while", TokenType.WHILE);
        keywords.put("print", TokenType.PRINT);
        keywords.put("input", TokenType.INPUT);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ' ', '\r', '\t' -> {
            } // Ignorar espacios en blanco
            case '\n' -> line++;
            case '(' -> addToken(TokenType.LEFT_PAREN);
            case ')' -> addToken(TokenType.RIGHT_PAREN);
            case '{' -> addToken(TokenType.LEFT_BRACE);
            case '}' -> addToken(TokenType.RIGHT_BRACE);
            case '[' -> addToken(TokenType.LEFT_BRACKET);
            case ']' -> addToken(TokenType.RIGHT_BRACKET);
            case ',' -> addToken(TokenType.COMMA);
            case '-' -> addToken(TokenType.MINUS);
            case '+' -> addToken(TokenType.PLUS);
            case ':' -> addToken(TokenType.COLON);
            case ';' -> addToken(TokenType.SEMICOLON);
            case '*' -> addToken(TokenType.STAR);
            case '!' -> addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '=' -> addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
            case '>' -> addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '<' -> addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '/' -> slash();
            case '"' -> string();
            default -> {
                if (isDigit(c))
                    number();
                else if (isAlpha(c))
                    identifier();
                else
                    LinkLang.error(line, "Carácter desconocido '" + c + "'.");
            }
        }
    }

    private void slash() {
        if (match('/'))
            while (peek() != '\n' && !isAtEnd()) advance(); // Ignorar comentarios
        else {
            addToken(TokenType.SLASH);
        }
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            LinkLang.error(line, "Cadena sin comillas finales");
            return;
        }

        // Consume las comillas finales
        advance();

        // Obtener texto de la cadena sin las comillas
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Si hay parte fraccional
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume el punto y sigue buscando números
            do {
                advance();
            } while (isDigit(peek()));
        }

        double number = Double.parseDouble(source.substring(start, current));
        addToken(TokenType.NUMBER, number);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        // Revisa si es una palabra reservada
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        addToken(type == null ? TokenType.IDENTIFIER : type);
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean match(char expected) {
        if (isAtEnd())
            return false;

        if (source.charAt(current) != expected)
            return false;

        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length())
            return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, literal, line));
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }
}
