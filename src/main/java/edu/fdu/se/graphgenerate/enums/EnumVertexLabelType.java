package edu.fdu.se.graphgenerate.enums;

public enum EnumVertexLabelType {

    IFSTMT("IfStmt"),
    ATOM("Atom"),
    RETURNSTMT("ReturnStmt"),
    WHILESTMT("WhileStmt"),
    FORSTMT("ForStmt"),
    FORINIT("ForInit"),
    FORUPDATE("ForUpdate"),
    DOWHILESTMT("DoWhileStmt"),
    FOREACHSTMT("ForEachStmt"),
    ASSIGNEXPR("AssignExpr"),
    SWITCHENTRY("SwitchEntry"),
    VARIBLEDECLARATIONEXPR("VaribleDeclarationExpr"),
    VARIBLEDECLARATOR("VariableDeclartor"),
    TYPE("Type"),
    BINARYEXPR("BinaryExpr"),
    METHODCALLEXPR("MethodCallExprVertex"),
    BLOCKSTMT("BlockStmt"),
    ELSE("Else"),
    SWITCHSTMT("SwitchStmt"),
    PROJECT("Project"),
    PACKAGE("Package"),
    CLASSORINTERFACE("ClassOrInterface"),
    METHODDECLARATION("MethodDeclarationVertex"),
    CONDITION("Condition"),
    TRYSTMT("Try"),
    CATCHSTMT("Catch"),
    CATCHCLAUSE("CatchClause"),
    FINALLY("Finally"),
    BLOCK("Block"),
    BRANCH("Branch"),
    LOOP("Loop"),
    END("End"),
    FIELDACCESSEXPR("FieldAccessExpr");

    private String value;

    private EnumVertexLabelType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }


}
