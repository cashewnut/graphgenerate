package edu.fdu.se.graphgenerate.model;

public class MethodDeclarationVertex extends Vertex {

    private String returnType;  //返回类型

    private String parameters;  //参数列表，以逗号隔开

    private Integer callBackFlag;   //0:false,1:true

    private String relevantView;    //关联view的基类，callBackFlag为false的时候不设置

    private Integer inheritFlag;    //0:false,1:true关联view继承的标志

    private String relevantBaseView;

    private Long blockId;

    private Vertex finalVertex; //方法声明节点的最后一个节点的endId

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getRelevantView() {
        return relevantView;
    }

    public void setRelevantView(String relevantView) {
        this.relevantView = relevantView;
    }

    public String getRelevantBaseView() {
        return relevantBaseView;
    }

    public void setRelevantBaseView(String relevantBaseView) {
        this.relevantBaseView = relevantBaseView;
    }

    public Integer getCallBackFlag() {
        return callBackFlag;
    }

    public void setCallBackFlag(Integer callBackFlag) {
        this.callBackFlag = callBackFlag;
    }

    public Integer getInheritFlag() {
        return inheritFlag;
    }

    public void setInheritFlag(Integer inheritFlag) {
        this.inheritFlag = inheritFlag;
    }

    public Long getBlockId() {
        return blockId;
    }

    public void setBlockId(Long blockId) {
        this.blockId = blockId;
    }

    public Vertex getFinalVertex() {
        return finalVertex;
    }

    public void setFinalVertex(Vertex finalVertex) {
        this.finalVertex = finalVertex;
    }
}
