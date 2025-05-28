package main;

import assembly_code.AssemblyGenerator;
import ast.AstPrinter;
import ast.Stmt;
import intermediate_code.Generator;
import intermediate_code.Quadruple;
import parser.Parser;
import scanner.Scanner;
import scanner.Token;
import scanner.TokenType;
import semantic_analysis.ScopeManager;
import semantic_analysis.SemanticAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class LinkLang {
    private static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1)
            System.out.println("Uso: java ruta/LinkLang.java [ruta/codigo.lila] ");
        else if (args.length == 1)
            runFile(args[0]);
        else
//            runPrompt();
            runFile(".\\src\\main\\example.gsd");
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null)
                break;
            run(line);
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) return;

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        ScopeManager scopeManager = semanticAnalyzer.analyze(statements);

        if (hadError) return;

        Generator quadrupleGenerator = new Generator(scopeManager);
        List<Quadruple> quadruples = quadrupleGenerator.generate(statements);

        //printTokens(tokens);
        //printAst(statements);
        //System.out.println(scopeManager);
        printQuadruples(quadruples);

        String asmFileName = "C:\\Users\\Acen\\Downloads\\temp\\test";
        AssemblyGenerator assemblyGenerator = new AssemblyGenerator(quadruples);
        try {
            assemblyGenerator.generateAssembly(asmFileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printQuadruples(List<Quadruple> quadruples) {
        for (int i = 0; i < quadruples.size(); i++) {
            System.out.println(i + ": " + quadruples.get(i));
        }
    }

    private static void printTokens(List<Token> tokens) {
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    private static void printAst(List<Stmt> statements) {
        AstPrinter printer = new AstPrinter();

        for (Stmt statement : statements) {
            System.out.println(statement.accept(printer));
        }
    }

    public static void error(int line, String msg) {
        report(line, "", msg);
    }

    public static void error(Token token, String msg) {
        if (token.type() == TokenType.EOF) {
            report(token.line(), "al final.", msg);
        } else {
            report(token.line(), "en '" + token.lexeme() + "'", msg);
        }
    }

    private static void report(int line, String where, String msg) {
        System.err.println("[linea " + line + "] Error " + where + ": " + msg);
        hadError = true;
    }
}