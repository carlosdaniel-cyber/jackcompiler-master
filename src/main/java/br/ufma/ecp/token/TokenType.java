package br.ufma.ecp.token;

import java.util.List;
import java.util.Map;

public enum TokenType {
    PLUS, MINUS,

    // Literals.
    NUMBER,
    STRING,

    IDENT,
    EOF,

    // symbols
    LPAREN, RPAREN,
    LBRACE, RBRACE,
    LBRACKET, RBRACKET,

    COMMA, SEMICOLON, DOT, ASTERISK, SLASH,
    AND, OR, NOT,
    LT, GT, EQ,
    // keywords
    WHILE, CLASS, CONSTRUCTOR, FUNCTION,
    METHOD, FIELD, STATIC, VAR, INT,
    CHAR, BOOLEAN, VOID, TRUE, FALSE,
    NULL, THIS, LET, DO, IF, ELSE, RETURN,
    ILLEGAL;

    static public boolean isSymbol (String c) {
        String symbols = "{}()[].,;+-*/&|<>=~";
        return symbols.indexOf(c) > -1;

    }

    // static public boolean isKeyword(TokenType type) {
    //     List<TokenType> keywords = List.of(

    //             WHILE, CLASS, CONSTRUCTOR, FUNCTION,
    //             METHOD, FIELD, STATIC, VAR, INT,
    //             CHAR, BOOLEAN, VOID, TRUE, FALSE,
    //             NULL, THIS, LET, DO, IF, ELSE, RETURN);
    //     return keywords.contains(type);
    // }

}
