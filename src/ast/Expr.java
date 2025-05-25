package ast;

import scanner.Token;

import java.util.ArrayList;
import java.util.List;

public abstract class Expr extends Node {

    public static class Assignment extends Expr {
        public final Token name;
        public final Expr value;

        public Assignment(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Array extends Expr {
        public final Token name;
        public final Expr size;
        public final Expr fillValue;
        public final List<Expr> values;

        // Para arrays con tamaño explícito [3: 'default']
        public Array(Token name, Expr size, Expr fillValue) {
            this.name = name;
            this.size = size;
            this.fillValue = fillValue;
            this.values = new ArrayList<>();

            if (size instanceof Expr.Literal) {
                Double arraySize = (Double) ((Expr.Literal) size).value;
                for (int i = 0; i < arraySize; i++) {
                    this.values.add(fillValue);
                }
            }
        }

        // Para arrays literales [1,2,3]
        public Array(Token name, List<Expr> values) {
            this.name = name;
            this.size = new Expr.Literal(values != null ? values.size() : 0.0);
            this.fillValue = null;
            this.values = values;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    // Necesario para semántica de cortocircuito
    public static class Logical extends Expr {
        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Binary extends Expr {
        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Call extends Expr {
        public final Expr.Variable callee;
        public final Token paren;
        public final List<Expr> arguments;

        public Call(Expr.Variable callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    // Ejemplo: (1 + 2) * 3
    public static class Group extends Expr {
        public final Expr expression;

        public Group(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Literal extends Expr {
        public final Object value;

        public Literal(Object value) {
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Subscript extends Expr {
        public final Expr.Variable var;
        public final Token name;
        public final Expr index;

        public Subscript(Expr.Variable var, Token name, Expr index) {
            this.var = var;
            this.name = name;
            this.index = index;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Unary extends Expr {
        public final Token operator;
        public final Expr right;

        public Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Variable extends Expr {
        public final Token name;

        public Variable(Token name) {
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }
}
