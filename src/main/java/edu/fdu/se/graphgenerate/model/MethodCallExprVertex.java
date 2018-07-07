package edu.fdu.se.graphgenerate.model;

public class MethodCallExprVertex extends Vertex {

    private Integer apiFlag; //0:false,1:true

    private String objectClass; //调用该方法的对象所对应的类

    private String parameters; //参数列表，逗号隔开

    private Integer inheritFlag; //继承标志，0:false,1:true

    private String objectBaseClass; //0:false,1:true,inheritFlag为false为不设置该属性

    private Long nextVertexId;

    public Integer getApiFlag() {
        return apiFlag;
    }

    public void setApiFlag(Integer apiFlag) {
        this.apiFlag = apiFlag;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Integer getInheritFlag() {
        return inheritFlag;
    }

    public void setInheritFlag(Integer inheritFlag) {
        this.inheritFlag = inheritFlag;
    }

    public String getObjectBaseClass() {
        return objectBaseClass;
    }

    public void setObjectBaseClass(String objectBaseClass) {
        this.objectBaseClass = objectBaseClass;
    }

    public Long getNextVertexId() {
        return nextVertexId;
    }

    public void setNextVertexId(Long nextVertexId) {
        this.nextVertexId = nextVertexId;
    }
}
