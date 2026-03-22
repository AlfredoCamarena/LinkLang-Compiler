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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class LinkLang {
    private static boolean hadError = false;

    private static final String BUNDLED_EXAMPLE_RESOURCE = "/examples/example.gsd";
    private static final String DEFAULT_OUTPUT_DIR = "output";
    private static final String DEFAULT_OUTPUT_NAME = "example";

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Uso: java -jar linklang.jar [ruta/codigo.gsd]");
            return;
        }

        if (args.length == 1) {
            runFile(Paths.get(args[0]), deriveOutputName(args[0]));
        } else {
            runBundledExample();
        }
    }

    private static void runFile(Path inputPath, String outputName) throws IOException {
        String source = Files.readString(inputPath, StandardCharsets.UTF_8);
        run(source, outputName);
    }

    private static void runBundledExample() throws IOException {
        try (InputStream stream = LinkLang.class.getResourceAsStream(BUNDLED_EXAMPLE_RESOURCE)) {
            if (stream == null) {
                System.err.println("Error: no se encontró el ejemplo integrado en el JAR.");
                return;
            }
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            run(source, DEFAULT_OUTPUT_NAME);
        }
    }

    private static void run(String source, String outputName) throws IOException {
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

        printQuadruples(quadruples);

        Path outputDir = Paths.get(DEFAULT_OUTPUT_DIR);
        Files.createDirectories(outputDir);
        String asmFilePath = outputDir.resolve(outputName).toString();

        AssemblyGenerator assemblyGenerator = new AssemblyGenerator(quadruples);
        assemblyGenerator.generateAssembly(asmFilePath);
    }

    private static String deriveOutputName(String inputPath) {
        String filename = Paths.get(inputPath).getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    private static void printQuadruples(List<Quadruple> quadruples) {
        for (int i = 0; i < quadruples.size(); i++) {
            System.out.println(i + ": " + quadruples.get(i));
        }
    }

    @SuppressWarnings("unused")
    private static void printTokens(List<Token> tokens) {
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    @SuppressWarnings("unused")
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