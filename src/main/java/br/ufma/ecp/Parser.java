package br.ufma.ecp;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;
import static br.ufma.ecp.token.TokenType.*;

public class Parser {

    private static class ParseError extends RuntimeException {}


    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();

    public Parser(byte[] input) {
        scan = new Scanner(input);
        nextToken();
    }

    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }

    public void parse() {
        parseClass();
    }

    void parseClass() {
        printNonTerminal("class");
        expectPeek(CLASS);
        expectPeek(IDENT);
        expectPeek(LBRACE);
        
        while (peekTokenIs(STATIC) || peekTokenIs(FIELD)) {
            System.out.println(peekToken);
            parseClassVarDec();
        }
    
        while (peekTokenIs(FUNCTION) || peekTokenIs(CONSTRUCTOR) || peekTokenIs(METHOD)) {
            parseSubroutineDec();
        }

        expectPeek(RBRACE);

        printNonTerminal("/class");
    }


    // funções auxiliares
    public String XMLOutput() {
        return xmlOutput.toString();
    }

    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }

    boolean peekTokenIs(TokenType type) {
        return peekToken.type == type;
    }

    boolean currentTokenIs(TokenType type) {
        return currentToken.type == type;
    }

    private void expectPeek(TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                expectPeek(type);
                return;
            }
        }

        throw error(peekToken, "Expected a statement");

    }

    private void expectPeek(TokenType type) {
        if (peekToken.type == type) {
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        } else {
            throw error(peekToken, "Expected " + type.name());
        }
    }

    private static void report(int line, String where,
            String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
    }

    private ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
        return new ParseError();
    }

    // term -> number | identifier | stringConstant | keywordConstant
    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
            case NUMBER:
                expectPeek(NUMBER);
                break;
            case STRING:
                expectPeek(STRING);
                break;
            case FALSE:
            case NULL:
            case TRUE:
            case THIS:
                expectPeek(FALSE, NULL, TRUE, THIS);
                break;
            case IDENT:
                expectPeek(IDENT);
                if (peekTokenIs(LPAREN) || peekTokenIs(DOT)) {
                    parseSubroutineCall();
                } else { // variavel comum ou array
                    if (peekTokenIs(LBRACKET)) { // array
                        expectPeek(LBRACKET);
                        parseExpression();
                        expectPeek(RBRACKET);
                    } 
                }
                break;
            case LPAREN:
                expectPeek(LPAREN);
                parseExpression();
                expectPeek(RPAREN);
                break;
            case MINUS:
            case NOT:
                expectPeek(MINUS, NOT);
                parseTerm();
                break;
            default:
                ;
        }
        printNonTerminal("/term");
    }

    static public boolean isOperator(String op) {
        return op != "" && "+-*/<>=~&|".contains(op);
    }

    // term (op term)*
    void parseExpression() {
        printNonTerminal("expression");
        parseTerm();
        while (isOperator(peekToken.lexeme)) {
            expectPeek(peekToken.type);
            parseTerm();
        }
        printNonTerminal("/expression");
    }

    // letStatement -> 'let' identifier( '[' expression ']' )? '=' expression ';’
    void parseLet() {
        printNonTerminal("letStatement");
        expectPeek(LET);
        expectPeek(IDENT);

        if (peekTokenIs(LBRACKET)) {
            expectPeek(LBRACKET);
            parseExpression();
            expectPeek(RBRACKET);
        }

        expectPeek(EQ);
        parseExpression();
        expectPeek(SEMICOLON);
        printNonTerminal("/letStatement");
    }

    // subroutineCall -> subroutineName '(' expressionList ')' | (className|varName)
    // '.' subroutineName '(' expressionList ')
    void parseSubroutineCall() {
        if (peekTokenIs(LPAREN)) {
            expectPeek(LPAREN);
            parseExpressionList();
            expectPeek(RPAREN);
        } else {
            expectPeek(DOT);
            expectPeek(IDENT);
            expectPeek(LPAREN);
            parseExpressionList();
            expectPeek(RPAREN);
        }
    }

    void parseExpressionList() {
        printNonTerminal("expressionList");

        if (!peekTokenIs(RPAREN)) // verifica se tem pelo menos uma expressao
        {
            parseExpression();
        }

        // procurando as demais
        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            parseExpression();
        }

        printNonTerminal("/expressionList");
    }
    // 'do' subroutineCall ';'
    public void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(DO);
        expectPeek(IDENT);
        parseSubroutineCall();
        expectPeek(SEMICOLON);
        printNonTerminal("/doStatement");
    }


        // classVarDec → ( 'static' | 'field' ) type varName ( ',' varName)* ';'
        void parseClassVarDec() {
            printNonTerminal("classVarDec");
            expectPeek(FIELD, STATIC);
            // 'int' | 'char' | 'boolean' | className
            expectPeek(INT, CHAR, BOOLEAN, IDENT);
            expectPeek(IDENT);
    
            while (peekTokenIs(COMMA)) {
                expectPeek(COMMA);
                expectPeek(IDENT);
            }
    
            expectPeek(SEMICOLON);
            printNonTerminal("/classVarDec");
        }


        void parseIf() {
            printNonTerminal("ifStatement");
            expectPeek(IF);
            expectPeek(LPAREN);
            parseExpression();
            expectPeek(RPAREN);
            expectPeek(LBRACE);
            parseStatements();
            expectPeek(RBRACE);
            printNonTerminal("/ifStatement");
        }

        void parseStatements() {
            printNonTerminal("statements");
            while (peekToken.type == WHILE ||
                    peekToken.type == IF ||
                    peekToken.type == LET ||
                    peekToken.type == DO ||
                    peekToken.type == RETURN) {
                parseStatement();
            }
    
            printNonTerminal("/statements");
        }


        void parseStatement() {
            switch (peekToken.type) {
                case LET:
                    parseLet();
                    break;
                case WHILE:
                    parseWhile();
                    break;
                case IF:
                    parseIf();
                    break;
                case RETURN:
                    parseReturn();
                    break;
                case DO:
                    parseDo();
                    break;
                default:
                    throw error(peekToken, "Expected a statement");
            }
        }
    
            // 'while' '(' expression ')' '{' statements '}'
    void parseWhile() {
        printNonTerminal("whileStatement");
        expectPeek(WHILE);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);
        printNonTerminal("/whileStatement");
    }

        // ReturnStatement -> 'return' expression? ';'
        void parseReturn() {
            printNonTerminal("returnStatement");
            expectPeek(RETURN);
            if (!peekTokenIs(SEMICOLON)) {
                parseExpression();
            }
            expectPeek(SEMICOLON);
    
            printNonTerminal("/returnStatement");
        }


        void parseSubroutineDec() {
            printNonTerminal("subroutineDec");
            expectPeek(CONSTRUCTOR, FUNCTION, METHOD);
            // 'int' | 'char' | 'boolean' | className
            expectPeek(VOID, INT, CHAR, BOOLEAN, IDENT);
            expectPeek(IDENT);
    
            expectPeek(LPAREN);
            parseParameterList();
            expectPeek(RPAREN);
            parseSubroutineBody();
    
            printNonTerminal("/subroutineDec");
        }

        void parseParameterList() {
            printNonTerminal("parameterList");
    
            if (!peekTokenIs(RPAREN)) // verifica se tem pelo menos uma expressao
            {
                expectPeek(INT, CHAR, BOOLEAN, IDENT);
                expectPeek(IDENT);
            }
    
            while (peekTokenIs(COMMA)) {
                expectPeek(COMMA);
                expectPeek(INT, CHAR, BOOLEAN, IDENT);
                expectPeek(IDENT);
            }
    
            printNonTerminal("/parameterList");
        }
    
        void parseSubroutineBody() {
    
            printNonTerminal("subroutineBody");
            expectPeek(LBRACE);
            while (peekTokenIs(VAR)) {
                parseVarDec();
            }
    
            parseStatements();
            expectPeek(RBRACE);
            printNonTerminal("/subroutineBody");
        }

        // 'var' type varName ( ',' varName)* ';'
        void parseVarDec() {
            printNonTerminal("varDec");
            expectPeek(VAR);
            // 'int' | 'char' | 'boolean' | className
            expectPeek(INT, CHAR, BOOLEAN, IDENT);
            expectPeek(IDENT);

            while (peekTokenIs(COMMA)) {
                expectPeek(COMMA);
                expectPeek(IDENT);
            }

            expectPeek(SEMICOLON);
            printNonTerminal("/varDec");
        }

}