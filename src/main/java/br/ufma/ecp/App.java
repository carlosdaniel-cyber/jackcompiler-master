package br.ufma.ecp;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class App {

    public static void main(String[] args) {

        /*
         * Teste 01
         */
        // String input = "45 + if - \"ola\" laranja 876";
        // Scanner scan = new Scanner (input.getBytes());
        // for (Token tk = scan.nextToken(); tk.type != EOF; tk = scan.nextToken()) {
        // System.out.println(tk);
        // }

        /*
         * Teste 02
         */
        // String input = "45 variavel + while < , if";
        // Scanner scan = new Scanner (input.getBytes());
        // for (Token tk = scan.nextToken(); tk.type != TokenType.EOF; tk =
        // scan.nextToken()) {
        // System.out.println(tk);
        // }

        /*
         * Teste 03
         */
        // String input = " //45 variavel + while < , if";
        // Scanner scan = new Scanner (input.getBytes());
        // for (Token tk = scan.nextToken(); tk.type != TokenType.EOF; tk =
        // scan.nextToken()) {
        // System.out.println(tk);
        // }

        /*
         * Teste 04
         */
        String input = """
                // é um comentario 10
                45 \"hello\" variavel + while < , if
                /*
                comentario em bloco
                */
                42 ola

                """;
        Scanner scan = new Scanner(input.getBytes());
        for (Token tk = scan.nextToken(); tk.type != TokenType.EOF; tk = scan.nextToken()) {
            System.out.println(tk);
        }
        // Parser p = new Parser (fromFile().getBytes());
        // p.parse();

        /*
         * String input = "489-85+69";
         * Scanner scan = new Scanner (input.getBytes());
         * System.out.println(scan.nextToken());
         * System.out.println(scan.nextToken());
         * System.out.println(scan.nextToken());
         * System.out.println(scan.nextToken());
         * System.out.println(scan.nextToken());
         * Token tk = new Token(NUMBER, "42");
         * System.out.println(tk);
         */
    }
}
