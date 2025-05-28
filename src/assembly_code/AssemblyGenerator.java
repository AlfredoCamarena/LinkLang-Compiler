package assembly_code;

import intermediate_code.OpCode;
import intermediate_code.Quadruple;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AssemblyGenerator {
    private final List<Quadruple> quadruples;
    private final StringBuilder asmCode;
    private final Map<String, Integer> variables;
    private final Map<String, Integer> temporals;
    private final Set<String> functions;
    private final Stack<Integer> callStack;
    private int stackOffset;
    private int tempCounter;
    private boolean inFunction;
    private String currentFunction;

    public AssemblyGenerator(List<Quadruple> quadruples) {
        this.quadruples = quadruples;
        this.asmCode = new StringBuilder();
        this.variables = new HashMap<>();
        this.temporals = new HashMap<>();
        this.functions = new HashSet<>();
        this.callStack = new Stack<>();
        this.stackOffset = 0;
        this.tempCounter = 0;
        this.inFunction = false;
        this.currentFunction = "";
    }

    public void generateAssembly(String filename) throws IOException {
        // Análisis previo para encontrar funciones y variables
        analyzeQuadruples();

        // Generar el código de ensamblador
        generateHeader();
        generateDataSection();
        generateTextSection();
        generateMainFunction();
        generateUserFunctions();
        generateNativeFunctions(); // Agregar funciones nativas
        generateFooter();

        // Escribir a archivo
        try (FileWriter writer = new FileWriter(filename + ".asm")) {
            writer.write(asmCode.toString());
        }

        System.out.println("Código ensamblador generado: " + filename + ".asm");
        System.out.println("\nPara compilar y ejecutar:");
        System.out.println("1. nasm -f win32 " + filename + ".asm -o " + filename + ".obj");
        System.out.println("2. gcc -m32 " + filename + ".obj -o " + filename + ".exe");
        System.out.println("3. ./" + filename + ".exe");
    }

    private void analyzeQuadruples() {
        for (Quadruple quad : quadruples) {
            switch (quad.op()) {
                case ARG -> {
                    if (!inFunction && !isTemporary(quad.arg1()) && !variables.containsKey(quad.arg1())) {
                        variables.put(quad.arg1(), stackOffset);
                        stackOffset += 4;
                    }
                }
                case FUNC -> {
                    functions.add(quad.arg1());
                    inFunction = true;
                    currentFunction = quad.arg1();
                }
                case END_FUNC -> {
                    inFunction = false;
                    currentFunction = "";
                }
                case ASSIGN -> {
                    if (!inFunction && !isTemporary(quad.result()) && !variables.containsKey(quad.result())) {
                        variables.put(quad.result(), stackOffset);
                        stackOffset += 4;
                    }
                }
                case INPUT -> {
                    if (!variables.containsKey(quad.result())) {
                        variables.put(quad.result(), stackOffset);
                        stackOffset += 4;
                    }
                }
            }
        }
    }

    private void generateHeader() {
        asmCode.append("section .data\n");
        asmCode.append("    newline db 10, 0\n");
        asmCode.append("    input_buffer times 32 db 0\n");
        asmCode.append("    format_int db '%d', 0\n");
        asmCode.append("    format_str db '%s', 0\n");
        asmCode.append("    format_input db '%31s', 0\n");
        asmCode.append("    temp_str times 32 db 0\n");

        // Mensajes para la función wifi_connect
        asmCode.append("    wifi_connecting db 'Conectando a WiFi...', 10, 0\n");
        asmCode.append("    wifi_success db 'WiFi conectado exitosamente', 10, 0\n");
        asmCode.append("    wifi_error db 'Error al conectar WiFi', 10, 0\n");
        asmCode.append("    wifi_prompt_ssid db 'Ingrese SSID: ', 0\n");
        asmCode.append("    wifi_prompt_pass db 'Ingrese password: ', 0\n");
        asmCode.append("    ssid_buffer times 64 db 0\n");
        asmCode.append("    pass_buffer times 64 db 0\n");

        // Plantilla y buffer para netsh
        asmCode.append("    netsh_template db 'netsh wlan connect name=\"%s\"', 0\n");
        asmCode.append("    netsh_command times 128 db 0\n\n");
    }


    private void generateDataSection() {
        asmCode.append("section .bss\n");
        // Reservar espacio para variables globales
        for (Map.Entry<String, Integer> var : variables.entrySet()) {
            asmCode.append("    ").append(var.getKey()).append(" resd 1\n");
        }
        asmCode.append("\n");
    }

    private void generateTextSection() {
        asmCode.append("section .text\n");
        asmCode.append("extern printf\n");
        asmCode.append("extern sprintf\n");
        asmCode.append("extern system\n");
        asmCode.append("extern scanf\n");
        asmCode.append("extern atoi\n");
        asmCode.append("extern exit\n");
        asmCode.append("extern time\n");
        asmCode.append("extern srand\n");
        asmCode.append("extern rand\n");
        asmCode.append("global main\n\n");
    }

    private void generateMainFunction() {
        asmCode.append("main:\n");
        asmCode.append("    push ebp\n");
        asmCode.append("    mov ebp, esp\n");
        asmCode.append("    sub esp, 64    ; Espacio para variables locales y temporales\n\n");

        // Generar código para cuádruplos que no están en funciones
        boolean skipUntilEndFunc = false;
        for (Quadruple quad : quadruples) {
            if (quad.op() == OpCode.FUNC) {
                skipUntilEndFunc = true;
                continue;
            }
            if (quad.op() == OpCode.END_FUNC) {
                skipUntilEndFunc = false;
                continue;
            }
            if (!skipUntilEndFunc) {
                generateQuadruple(quad);
            }
        }

        asmCode.append("\n    ; Salir del programa\n");
        asmCode.append("    push 0\n");
        asmCode.append("    call exit\n");
    }

    private void generateUserFunctions() {
        boolean inFunc = false;
        String funcName = "";

        for (Quadruple quad : quadruples) {
            if (quad.op() == OpCode.FUNC) {
                inFunc = true;
                funcName = quad.arg1();
                asmCode.append("\n").append(funcName).append(":\n");
                asmCode.append("    push ebp\n");
                asmCode.append("    mov ebp, esp\n");
                asmCode.append("    sub esp, 32    ; Espacio para variables locales\n");
                continue;
            }

            if (quad.op() == OpCode.END_FUNC) {
                if (inFunc) {
                    asmCode.append("    mov esp, ebp\n");
                    asmCode.append("    pop ebp\n");
                    asmCode.append("    ret\n");
                }
                inFunc = false;
                continue;
            }

            if (inFunc) {
                generateQuadruple(quad);
            }
        }
    }

    private void generateNativeFunctions() {
        asmCode.append("\n; ===== FUNCIÓN NATIVA: _wifi_connect =====\n");
        asmCode.append("_wifi_connect:\n");
        asmCode.append("    push ebp\n");
        asmCode.append("    mov ebp, esp\n");
        asmCode.append("    sub esp, 16    ; Espacio para variables locales\n");
        asmCode.append("\n");

        // Mostrar mensaje de conexión
        asmCode.append("    push wifi_connecting\n");
        asmCode.append("    push format_str\n");
        asmCode.append("    call printf\n");
        asmCode.append("    add esp, 8\n");
        asmCode.append("\n");

        // Solicitar SSID
        asmCode.append("    push wifi_prompt_ssid\n");
        asmCode.append("    push format_str\n");
        asmCode.append("    call printf\n");
        asmCode.append("    add esp, 8\n");

        asmCode.append("    push ssid_buffer\n");
        asmCode.append("    push format_input\n");
        asmCode.append("    call scanf\n");
        asmCode.append("    add esp, 8\n");
        asmCode.append("\n");

        // Construir comando netsh
        asmCode.append("    push ssid_buffer\n");
        asmCode.append("    push netsh_template\n");
        asmCode.append("    push netsh_command\n");
        asmCode.append("    call sprintf\n");
        asmCode.append("    add esp, 12\n");
        asmCode.append("\n");

        // Ejecutar comando con system
        asmCode.append("    push netsh_command\n");
        asmCode.append("    call system\n");
        asmCode.append("    add esp, 4\n");
        asmCode.append("\n");

        // Mostrar mensaje de éxito
        asmCode.append("    push wifi_success\n");
        asmCode.append("    push format_str\n");
        asmCode.append("    call printf\n");
        asmCode.append("    add esp, 8\n");
        asmCode.append("    mov eax, 1    ; Retornar 1 (éxito)\n");
        asmCode.append("\n");

        // Final de la función
        asmCode.append("    mov esp, ebp\n");
        asmCode.append("    pop ebp\n");
        asmCode.append("    ret\n");
        asmCode.append("; ===== FIN DE _wifi_connect =====\n\n");
    }


    private void generateQuadruple(Quadruple quad) {
        switch (quad.op()) {
            case ASSIGN -> generateAssign(quad);
            case ADD -> generateBinaryOp(quad, "add");
            case SUB -> generateBinaryOp(quad, "sub");
            case MULTIPLY -> generateBinaryOp(quad, "imul");
            case DIVIDE -> generateDivide(quad);
            case EQUAL -> generateComparison(quad, "sete");
            case NOT_EQUAL -> generateComparison(quad, "setne");
            case LESS -> generateComparison(quad, "setl");
            case LESS_EQUAL -> generateComparison(quad, "setle");
            case GREATER -> generateComparison(quad, "setg");
            case GREATER_EQUAL -> generateComparison(quad, "setge");
            case AND -> generateLogicalAnd(quad);
            case OR -> generateLogicalOr(quad);
            case NOT -> generateNot(quad);
            case IF_FALSE -> generateIfFalse(quad);
            case GOTO -> generateGoto(quad);
            case LABEL -> generateLabel(quad);
            case PRINT -> generatePrint(quad);
            case INPUT -> generateInput(quad);
            case CALL -> generateCall(quad);
            case RETURN -> generateReturn(quad);
            case ARG -> generateArg(quad);
            case PARAM -> generateParam(quad);
            default -> asmCode.append("    ; ").append(quad).append("\n");
        }
    }

    private void generateAssign(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        if (isNumeric(quad.arg1())) {
            // Asignación directa de constante
            asmCode.append("    mov dword [").append(getVariableRef(quad.result())).append("], ")
                    .append(quad.arg1()).append("\n");
        } else {
            // Asignación de variable a variable
            asmCode.append("    mov eax, [").append(getVariableRef(quad.arg1())).append("]\n");
            asmCode.append("    mov [").append(getVariableRef(quad.result())).append("], eax\n");
        }
    }

    private void generateBinaryOp(Quadruple quad, String operation) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        // Cargar primer operando
        if (isNumeric(quad.arg1())) {
            asmCode.append("    mov eax, ").append(quad.arg1()).append("\n");
        } else {
            asmCode.append("    mov eax, [").append(getVariableRef(quad.arg1())).append("]\n");
        }

        // Realizar operación con segundo operando
        if (isNumeric(quad.arg2())) {
            if (operation.equals("imul")) {
                asmCode.append("    mov ebx, ").append(quad.arg2()).append("\n");
                asmCode.append("    imul eax, ebx\n");
            } else {
                asmCode.append("    ").append(operation).append(" eax, ").append(quad.arg2()).append("\n");
            }
        } else {
            if (operation.equals("imul")) {
                asmCode.append("    imul eax, [").append(getVariableRef(quad.arg2())).append("]\n");
            } else {
                asmCode.append("    ").append(operation).append(" eax, [").append(getVariableRef(quad.arg2())).append("]\n");
            }
        }

        // Guardar resultado
        asmCode.append("    mov [").append(getVariableRef(quad.result())).append("], eax\n");
    }

    private void generateDivide(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        // Cargar dividendo
        if (isNumeric(quad.arg1())) {
            asmCode.append("    mov eax, ").append(quad.arg1()).append("\n");
        } else {
            asmCode.append("    mov eax, [").append(getVariableRef(quad.arg1())).append("]\n");
        }

        asmCode.append("    cdq        ; Extender signo de EAX a EDX:EAX\n");

        // Cargar divisor y dividir
        if (isNumeric(quad.arg2())) {
            asmCode.append("    mov ebx, ").append(quad.arg2()).append("\n");
            asmCode.append("    idiv ebx\n");
        } else {
            asmCode.append("    idiv dword [").append(getVariableRef(quad.arg2())).append("]\n");
        }

        // Guardar resultado (cociente en EAX)
        asmCode.append("    mov [").append(getVariableRef(quad.result())).append("], eax\n");
    }

    private void generateComparison(Quadruple quad, String setInstruction) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        // Cargar primer operando
        if (isNumeric(quad.arg1())) {
            asmCode.append("    mov eax, ").append(quad.arg1()).append("\n");
        } else {
            asmCode.append("    mov eax, [").append(getVariableRef(quad.arg1())).append("]\n");
        }

        // Comparar con segundo operando
        if (isNumeric(quad.arg2())) {
            asmCode.append("    cmp eax, ").append(quad.arg2()).append("\n");
        } else {
            asmCode.append("    cmp eax, [").append(getVariableRef(quad.arg2())).append("]\n");
        }

        // Establecer resultado (0 o 1)
        asmCode.append("    ").append(setInstruction).append(" al\n");
        asmCode.append("    movzx eax, al\n");
        asmCode.append("    mov [").append(getVariableRef(quad.result())).append("], eax\n");
    }

    private void generateLogicalAnd(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        // Cargar operandos y hacer AND lógico
        if (isNumeric(quad.arg1())) {
            asmCode.append("    mov eax, ").append(quad.arg1()).append("\n");
        } else {
            asmCode.append("    mov eax, [").append(getVariableRef(quad.arg1())).append("]\n");
        }

        if (isNumeric(quad.arg2())) {
            asmCode.append("    mov ebx, ").append(quad.arg2()).append("\n");
        } else {
            asmCode.append("    mov ebx, [").append(getVariableRef(quad.arg2())).append("]\n");
        }

        asmCode.append("    test eax, eax\n");
        asmCode.append("    setne al\n");
        asmCode.append("    test ebx, ebx\n");
        asmCode.append("    setne bl\n");
        asmCode.append("    and al, bl\n");
        asmCode.append("    movzx eax, al\n");
        asmCode.append("    mov [").append(getVariableRef(quad.result())).append("], eax\n");
    }

    private void generateLogicalOr(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        // Similar a AND pero con OR
        if (isNumeric(quad.arg1())) {
            asmCode.append("    mov eax, ").append(quad.arg1()).append("\n");
        } else {
            asmCode.append("    mov eax, [").append(getVariableRef(quad.arg1())).append("]\n");
        }

        if (isNumeric(quad.arg2())) {
            asmCode.append("    mov ebx, ").append(quad.arg2()).append("\n");
        } else {
            asmCode.append("    mov ebx, [").append(getVariableRef(quad.arg2())).append("]\n");
        }

        asmCode.append("    test eax, eax\n");
        asmCode.append("    setne al\n");
        asmCode.append("    test ebx, ebx\n");
        asmCode.append("    setne bl\n");
        asmCode.append("    or al, bl\n");
        asmCode.append("    movzx eax, al\n");
        asmCode.append("    mov [").append(getVariableRef(quad.result())).append("], eax\n");
    }

    private void generateNot(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        if (isNumeric(quad.arg1())) {
            asmCode.append("    mov eax, ").append(quad.arg1()).append("\n");
        } else {
            asmCode.append("    mov eax, [").append(getVariableRef(quad.arg1())).append("]\n");
        }

        asmCode.append("    test eax, eax\n");
        asmCode.append("    sete al\n");
        asmCode.append("    movzx eax, al\n");
        asmCode.append("    mov [").append(getVariableRef(quad.result())).append("], eax\n");
    }

    private void generateIfFalse(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        if (isNumeric(quad.arg1())) {
            asmCode.append("    mov eax, ").append(quad.arg1()).append("\n");
        } else {
            asmCode.append("    mov eax, [").append(getVariableRef(quad.arg1())).append("]\n");
        }

        asmCode.append("    test eax, eax\n");
        asmCode.append("    jz ").append(quad.result()).append("\n");
    }

    private void generateGoto(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");
        asmCode.append("    jmp ").append(quad.result()).append("\n");
    }

    private void generateLabel(Quadruple quad) {
        asmCode.append(quad.arg1()).append(":\n");
    }

    private void generatePrint(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        if (quad.arg1().startsWith("\"")) {
            // Imprimir string literal
            String str = quad.arg1().substring(1, quad.arg1().length() - 1);
            str = str.replace("\\n", "\", 10, \"");
            asmCode.append("    push dword ").append("\"").append(str).append("\", 0").append("\n");
            asmCode.append("    push format_str\n");
            asmCode.append("    call printf\n");
            asmCode.append("    add esp, 8\n");
        } else if (isNumeric(quad.arg1())) {
            // Imprimir número literal
            asmCode.append("    push ").append(quad.arg1()).append("\n");
            asmCode.append("    push format_int\n");
            asmCode.append("    call printf\n");
            asmCode.append("    add esp, 8\n");
        } else {
            // Imprimir variable
            asmCode.append("    push dword [").append(getVariableRef(quad.arg1())).append("]\n");
            asmCode.append("    push format_int\n");
            asmCode.append("    call printf\n");
            asmCode.append("    add esp, 8\n");
        }
    }

    private void generateInput(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");

        // Si hay prompt, imprimirlo
        if (quad.arg1() != null && !quad.arg1().equals("null")) {
            if (quad.arg1().startsWith("\"")) {
                String prompt = quad.arg1().substring(1, quad.arg1().length() - 1);
                asmCode.append("    push dword \"").append(prompt).append("\", 0\n");
                asmCode.append("    push format_str\n");
                asmCode.append("    call printf\n");
                asmCode.append("    add esp, 8\n");
            }
        }

        // Leer entrada como string y convertir a entero
        asmCode.append("    push input_buffer\n");
        asmCode.append("    push format_input\n");
        asmCode.append("    call scanf\n");
        asmCode.append("    add esp, 8\n");

        asmCode.append("    push input_buffer\n");
        asmCode.append("    call atoi\n");
        asmCode.append("    add esp, 4\n");
        asmCode.append("    mov [").append(getVariableRef(quad.result())).append("], eax\n");
    }

    private void generateCall(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");
        asmCode.append("    call ").append(quad.arg1()).append("\n");
        if (quad.result() != null) {
            asmCode.append("    mov [").append(getVariableRef(quad.result())).append("], eax\n");
        }
    }

    private void generateReturn(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");
        if (quad.arg1() != null) {
            if (isNumeric(quad.arg1())) {
                asmCode.append("    mov eax, ").append(quad.arg1()).append("\n");
            } else {
                asmCode.append("    mov eax, [").append(getVariableRef(quad.arg1())).append("]\n");
            }
        }
        asmCode.append("    mov esp, ebp\n");
        asmCode.append("    pop ebp\n");
        asmCode.append("    ret\n");
    }

    private void generateArg(Quadruple quad) {
        asmCode.append("    ; ").append(quad.toString()).append("\n");
        if (isNumeric(quad.arg1())) {
            asmCode.append("    push ").append(quad.arg1()).append("\n");
        } else {
            asmCode.append("    push dword [").append(getVariableRef(quad.arg1())).append("]\n");
        }
    }

    private void generateParam(Quadruple quad) {
        //
        asmCode.append("    ; ").append(quad.toString()).append("\n");
    }

    private void generateFooter() {
        asmCode.append("\n");
    }

    private String getVariableRef(String name) {
        if (isTemporary(name)) {
            return getTempLocation(name);
        }
        return name;
    }

    private String getTempLocation(String temp) {
        if (!temporals.containsKey(temp)) {
            temporals.put(temp, -4 * (temporals.size() + 1));
        }
        return "ebp" + temporals.get(temp);
    }

    private boolean isTemporary(String name) {
        return name != null && name.startsWith("t");
    }

    private boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e2) {
                return false;
            }
        }
    }
}