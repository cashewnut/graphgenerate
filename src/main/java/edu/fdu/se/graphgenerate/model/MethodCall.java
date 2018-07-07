package edu.fdu.se.graphgenerate.model;

import edu.fdu.se.graphgenerate.utils.Relation;

import java.util.Objects;

public class MethodCall {

    private Vertex methodCallVertex;
    private String name;
    private Edge edge = Relation.ENTRY;

    public Vertex getMethodCallVertex() {
        return methodCallVertex;
    }

    public void setMethodCallVertex(Vertex methodCallVertex) {
        this.methodCallVertex = methodCallVertex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Edge getEdge() {
        return edge;
    }

    public void setEdge(Edge edge) {
        this.edge = edge;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodCall that = (MethodCall) o;
        return Objects.equals(methodCallVertex, that.methodCallVertex) &&
                Objects.equals(name, that.name) &&
                Objects.equals(edge, that.edge);
    }

    @Override
    public int hashCode() {

        return Objects.hash(methodCallVertex, name, edge);
    }
}
