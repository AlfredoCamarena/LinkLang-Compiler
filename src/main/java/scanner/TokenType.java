package scanner;

public enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET,
    RIGHT_BRACKET, COMMA, MINUS, PLUS, COLON, SEMICOLON, SLASH, STAR,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals.
    IDENTIFIER, STRING, NUMBER,

    // Keywords.
    AND, ELSE, FALSE, FUNC, FOR, IF, NULL, OR,
    RETURN, TRUE, VAR, WHILE, PRINT, INPUT,

    EOF
}
