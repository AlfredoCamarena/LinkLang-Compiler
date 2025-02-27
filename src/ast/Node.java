package ast;

public abstract class Node {
    public abstract <R> R accept(Visitor<R> visitor);
}
