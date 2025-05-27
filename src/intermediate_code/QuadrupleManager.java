package intermediate_code;

import java.util.ArrayList;
import java.util.List;

public class QuadrupleManager {
    private final List<Quadruple> quadruples = new ArrayList<>();
    private int tempCounter = 0;
    private int labelCounter = 0;

    public String newTemp() {
        return "t" + tempCounter++;
    }

    public String newLabel() {
        return "L" + labelCounter++;
    }

    public void addQuadruple(OpCode op, String arg1, String arg2, String result) {
        quadruples.add(new Quadruple(op, arg1, arg2, result));
    }

    public List<Quadruple> getQuadruples() {
        return quadruples;
    }
}