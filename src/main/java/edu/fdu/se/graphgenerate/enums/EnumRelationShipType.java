package edu.fdu.se.graphgenerate.enums;

public enum EnumRelationShipType {
    PARENT("Parent"),
    CDEPENDENCY("CDependency"),
    DDEPENDENCY("DDependency"),
    TRUE("True"),
    FALSE("False"),
    EQUALS("Equals"),
    IN("In"),
    ORDER("Order"),
    DEFAULT("Default"),
    MESSAGE("Message"),
    THREAD("Thread"),
    ASYNCTASK("AsyncTask"),
    CALL("Call"),
    RETURN("Return"),
    POST("Post"),
    ENTRY("Entry");

    private String value;

    private EnumRelationShipType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
