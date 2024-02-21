package br.ufma.ecp;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;
import static br.ufma.ecp.token.TokenType.*;

import br.ufma.ecp.VMWriter.Command;
import br.ufma.ecp.VMWriter.Segment;

public class Parser {

    private static class ParseError extends RuntimeException {}

    private VMWriter vmWriter = new VMWriter();

    private int ifLabelNum = 0 ;
    private int whileLabelNum = 0;


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
        TokenType op;
        switch (peekToken.type) {
            case NUMBER:
                expectPeek(TokenType.NUMBER);
                vmWriter.writePush(Segment.CONST, Integer.parseInt(currentToken.lexeme));
                break;
            case STRING:
                expectPeek(TokenType.STRING);
                var strValue = currentToken.lexeme;
                vmWriter.writePush(Segment.CONST, strValue.length());
                vmWriter.writeCall("String.new", 1);
                for (int i = 0; i < strValue.length(); i++) {
                    vmWriter.writePush(Segment.CONST, strValue.charAt(i));
                    vmWriter.writeCall("String.appendChar", 2);
                }
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(FALSE, NULL, TRUE);
                vmWriter.writePush(Segment.CONST, 0);
                if (currentToken.type == TRUE)
                    vmWriter.writeArithmetic(Command.NOT);
                break;
            case THIS:
                expectPeek(THIS);
                vmWriter.writePush(Segment.POINTER, 0);
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
                expectPeek(MINUS);
                op = currentToken.type;
                parseTerm();
                vmWriter.writeArithmetic(Command.NEG);
                break;
            case NOT:
                expectPeek(NOT);
                op = currentToken.type;
                parseTerm();
                vmWriter.writeArithmetic(Command.NOT);    
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
            var ope = peekToken.type;
            expectPeek(peekToken.type);
            parseTerm();
            compileOperators(ope);
        }
        printNonTerminal("/expression");
    }

    public void compileOperators(TokenType type) {

            if (type == ASTERISK) {
                vmWriter.writeCall("Math.multiply", 2);
            } else if (type == SLASH) {
                vmWriter.writeCall("Math.divide", 2);
            } else {
                vmWriter.writeArithmetic(typeOperator(type));
            }
        }

        private Command typeOperator(TokenType type) {
            if (type == PLUS)
                return Command.ADD;
            if (type == MINUS)
                return Command.SUB;
            if (type == LT)
                return Command.LT;
            if (type == GT)
                return Command.GT;
            if (type == EQ)
                return Command.EQ;
            if (type == AND)
                return Command.AND;
            if (type == OR)
                return Command.OR;
            return null;
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

            var labelTrue = "IF_TRUE" + ifLabelNum;
            var labelFalse = "IF_FALSE" + ifLabelNum;
            var labelEnd = "IF_END" + ifLabelNum;
    
            ifLabelNum++;

            expectPeek(IF);
            expectPeek(LPAREN);
            parseExpression();
            expectPeek(RPAREN);

            vmWriter.writeIf(labelTrue);
            vmWriter.writeGoto(labelFalse);
            vmWriter.writeLabel(labelTrue);

            expectPeek(LBRACE);
            parseStatements();
            expectPeek(RBRACE);

            if (peekTokenIs(ELSE)){
                vmWriter.writeGoto(labelEnd);
            }

            vmWriter.writeLabel(labelFalse);

            if (peekTokenIs(ELSE)){
                expectPeek(ELSE);
                expectPeek(LBRACE);
                parseStatements();
                expectPeek(RBRACE);
                vmWriter.writeLabel(labelEnd);
            }

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

            var labelTrue = "WHILE_EXP" + whileLabelNum;
            var labelFalse = "WHILE_END" + whileLabelNum;
            whileLabelNum++;

            vmWriter.writeLabel(labelTrue);

            expectPeek(WHILE);
            expectPeek(LPAREN);
            parseExpression();

            vmWriter.writeArithmetic(Command.NOT);
            vmWriter.writeIf(labelFalse);

            expectPeek(RPAREN);
            expectPeek(LBRACE);
            parseStatements();

            vmWriter.writeGoto(labelTrue); // Go back to labelTrue and check condition
            vmWriter.writeLabel(labelFalse); // Breaks out of while loop because ~(condition) is true

            expectPeek(RBRACE);
            printNonTerminal("/whileStatement");
        }

        // ReturnStatement -> 'return' expression? ';'
        void parseReturn() {
            printNonTerminal("returnStatement");
            expectPeek(RETURN);
            if (!peekTokenIs(SEMICOLON)) {
                parseExpression();
            } else {
                vmWriter.writePush(Segment.CONST, 0);
            }
            expectPeek(SEMICOLON);
            vmWriter.writeReturn();
    
            printNonTerminal("/returnStatement");
        }

        void parseSubroutineDec() {
            printNonTerminal("subroutineDec");

            ifLabelNum = 0;
            whileLabelNum = 0;

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

        public String VMOutput() {
            return vmWriter.vmOutput();
        }

}