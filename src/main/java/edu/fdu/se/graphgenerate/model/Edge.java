package edu.fdu.se.graphgenerate.model;

import java.util.Objects;

public class Edge {

    private String label;
    private String name;

    private String sort;

    public Edge(String label, String name) {
        this.label = label;
        this.name = name;
    }

    public Edge(String label, String name, String sort) {
        this.label = label;
        this.name = name;
        this.sort = sort;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(label, edge.label) &&
                Objects.equals(name, edge.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, name);
    }
}
