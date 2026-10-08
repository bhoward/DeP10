package edu.depauw.declan;

public enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, // '('
    RIGHT_PAREN, // ')'
    LEFT_BRACKET, // '['
    RIGHT_BRACKET, // ']'
    SEMICOLON, // ';'
    COMMA, // ','
    DOT, // '.'
    MINUS, // '-'
    PLUS, // '+'
    SLASH, // '/'
    STAR, // '*'
    AND, // '&'
    NOT, // '~' (tilde)
    EQUAL, // '='
    NOT_EQUAL, // '#'

    // One or two character tokens.
    COLON, // ':'
    ASSIGN, // ':='
    GREATER, // '>'
    GREATER_EQUAL, // '>='
    LESS, // '<'
    LESS_EQUAL, // '<='

    // Literals.
    IDENTIFIER, NUMBER,

    // Keywords.
    ARRAY, BEGIN, BOOLEAN, BY, CONST, DIV, DO, ELSE, ELSIF, END, FALSE, FOR, IF, //
    INTEGER, LONGINT, MOD, OF, OR, PROCEDURE, REAL, REPEAT, THEN, TO, TRUE, TYPE, UNTIL, VAR, WHILE,

    // Other.
    ERROR, EOF
}
