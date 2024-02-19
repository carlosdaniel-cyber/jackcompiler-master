

# Projeto de Compiladores 

Este é o repositório do projeto da disciplina de Compiladores, realizado como parte do curso de Engenharia da Computação (UFMA). Este projeto é uma colaboração entre [João Felipe](https://github.com/Joaofelipe14) e [carlosCajado](https://github.com/carlosCajado), sendo tutoriado pelo [Prof.Sergio Costa](https://profsergiocosta.notion.site/Construindo-Compiladores-com-Nand2Tetris-193b291e3e02443984aeecc796682cfc)

## Descrição

Este projeto vamos implementar um compilador em duas etapas. A análise léxica poderá ser implementada manualmente ou com auxílio de expressões regulares. Utilizaremos um analisador sintático preditivo implementado manualmente, sem a geração de uma Árvore Sintática Abstrata (AST). Em vez disso, iremos gerar diretamente uma representação intermediária stack-based.

Ao final do processo, essa linguagem intermediária será traduzida para um código Assembly de uma arquitetura de computador simplificada, conforme especificado no contexto do curso Nand2Tetris https://www.nand2tetris.org/.

## Estrutura do Projeto

- `src/`: Este diretório contém os arquivos-fonte do compilador.
- `tests/`: Aqui estão os casos de teste para verificar a corretude da implementação.

## Pré-Requisitos

Certifique-se de ter instalado os seguintes requisitos antes de executar o compilador:

- [Nand2Tetris Software Suite](https://www.nand2tetris.org/software): Ferramentas necessárias para o desenvolvimento do projeto.
- [Java Development Kit (JDK)](https://www.oracle.com/java/technologies/javase-jdk15-downloads.html): O compilador será implementado em Java.

## Como Utilizar

1. Clone este repositório:

   ```bash
   git clone https://github.com/Joaofelipe14/jackcompiler.git
   ```

2. Navegue até o diretório do projeto:

   ```bash
   cd projeto-compiladores
   ```

3. Execute o compilador:

   ```bash
   java -jar compilador.jar arquivoDeEntrada.jack
   ```

