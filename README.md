# LinkLang Compiler

A compiler for a custom language developed for the Languages and Automata course. It takes `.lila` source files and produces x86 NASM assembly.

## Requirements

- Java 17+ (JDK)
- Apache Maven 3.6+

> **To assemble and run the generated `.asm` file** you'll also need:
> - NASM
> - GCC (MinGW on Windows)

---

## Build

```bash
mvn package
```

This produces `target/linklang.jar`.

---

## Run

### With the built-in example

```bash
java -jar target/linklang.jar
```

### With your own `.lila` file

```bash
java -jar target/linklang.jar path/to/your_program.lila
```

The compiler will write the generated assembly to `output/<filename>.asm`.

---

## Assemble & Link the Output

After running the compiler use nasm and gcc as in this example

```bash
nasm -f win32 output/example.asm -o output/example.obj
gcc -m32 output/example.obj -o output/example.exe
output/example.exe
```

---

## Language Grammar

The formal grammar representation for LinkLang is detailed in the [GRAMMAR.md](GRAMMAR.md) file.

---

## Project Structure

```
src/
└── main/
    ├── java/
    │   ├── main/             # Entry point (LinkLang.java)
    │   ├── scanner/          # Lexical analysis
    │   ├── parser/           # Syntax analysis
    │   ├── ast/              # AST nodes and visitor
    │   ├── semantic_analysis/# Semantic analysis and scope management
    │   ├── intermediate_code/# Quadruple IR generation
    │   └── assembly_code/    # x86 NASM code generation
    └── resources/
        └── examples/
            └── example.lila   # Built-in demo program
```
