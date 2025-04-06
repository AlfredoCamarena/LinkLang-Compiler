package intermediate_code;

public record Quadruple(OpCode op, String arg1, String arg2, String result) {

    @Override
    public String toString() {
        return String.format("(%s, %s, %s, %s)", op, arg1, arg2, result);
    }
}