package edu.fdu.se.graphgenerate.model;

import java.util.ArrayList;
import java.util.List;

public class Vertex {

    private Long id;

    private List<String> labels;

    private String name;

    private String correspondingPackage; //关联包

    private String correspondingClass;  //关联类

    private Long endId;

    private Long startId;//只有end节点有这个属性

    public Vertex() {
        labels = new ArrayList<>();
    }

    public Vertex(String label, String name) {
        labels = new ArrayList<>();
        this.labels.add(label);
        this.name = name;
    }

    public void addLabel(String label) {
        this.labels.add(label);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCorrespondingPackage() {
        return correspondingPackage;
    }

    public void setCorrespondingPackage(String correspondingPackage) {
        this.correspondingPackage = correspondingPackage;
    }

    public String getCorrespondingClass() {
        return correspondingClass;
    }

    public void setCorrespondingClass(String correspondingClass) {
        this.correspondingClass = correspondingClass;
    }

    public Long getEndId() {
        return endId;
    }

    public void setEndId(Long endId) {
        if(endId != null)
            this.endId = endId;
    }

    public Long getStartId() {
        return startId;
    }

    public void setStartId(Long startId) {
        this.startId = startId;
    }
}
