package edu.fdu.se.graphgenerate.utils;

import edu.fdu.se.graphgenerate.enums.EnumRelationShipType;
import edu.fdu.se.graphgenerate.model.Edge;

public class Relation {

    public final static Edge PARNET = new Edge(EnumRelationShipType.PARENT.getValue(),
            EnumRelationShipType.PARENT.getValue());
    public final static Edge TRUE = new Edge(EnumRelationShipType.CDEPENDENCY.getValue(),
            EnumRelationShipType.TRUE.getValue());
    public final static Edge FALSE = new Edge(EnumRelationShipType.CDEPENDENCY.getValue(),
            EnumRelationShipType.FALSE.getValue());
    public final static Edge EQUALS = new Edge(EnumRelationShipType.CDEPENDENCY.getValue(),
            EnumRelationShipType.EQUALS.getValue());
    public final static Edge DEFAULT = new Edge(EnumRelationShipType.CDEPENDENCY.getValue(),
            EnumRelationShipType.DEFAULT.getValue());
    public final static Edge IN = new Edge(EnumRelationShipType.CDEPENDENCY.getValue(),
            EnumRelationShipType.IN.getValue());
    public final static Edge CDEPENDENCY = new Edge(EnumRelationShipType.CDEPENDENCY.getValue(),
            EnumRelationShipType.CDEPENDENCY.getValue());
    public final static Edge ORDER = new Edge(EnumRelationShipType.ORDER.getValue(),
            EnumRelationShipType.ORDER.getValue());
    public final static Edge CALL = new Edge(EnumRelationShipType.CALL.getValue(),
            EnumRelationShipType.CALL.getValue());
    public final static Edge ASYNCTASK = new Edge(EnumRelationShipType.ASYNCTASK.getValue(),
            EnumRelationShipType.ASYNCTASK.getValue());
    public final static Edge THREAD = new Edge(EnumRelationShipType.THREAD.getValue(),
            EnumRelationShipType.THREAD.getValue());
    public final static Edge MESSAGE = new Edge(EnumRelationShipType.MESSAGE.getValue(),
            EnumRelationShipType.MESSAGE.getValue());
    public final static Edge POST = new Edge(EnumRelationShipType.POST.getValue(),
            EnumRelationShipType.POST.getValue());
    public final static Edge ENTRY = new Edge(EnumRelationShipType.ENTRY.getValue(),
            EnumRelationShipType.ENTRY.getValue());
    public final static Edge RETURN = new Edge(EnumRelationShipType.RETURN.getValue(),
            EnumRelationShipType.RETURN.getValue());


}
