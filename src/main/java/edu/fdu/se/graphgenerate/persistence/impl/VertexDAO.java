package edu.fdu.se.graphgenerate.persistence.impl;

import edu.fdu.se.graphgenerate.model.Edge;
import edu.fdu.se.graphgenerate.model.MethodCallExprVertex;
import edu.fdu.se.graphgenerate.model.MethodDeclarationVertex;
import edu.fdu.se.graphgenerate.model.Vertex;
import edu.fdu.se.graphgenerate.persistence.IVertexDAO;
import edu.fdu.se.graphgenerate.utils.Neo4JUtil;
import org.neo4j.driver.v1.*;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.stream.Collectors;

import static java.lang.String.format;

public class VertexDAO implements IVertexDAO {

    @Override
    public void saveVertex(Vertex v) {
        Driver driver = Neo4JUtil.getDriver();
        Session session = Neo4JUtil.getSession(driver);
        Long id = session.writeTransaction(tx -> {
            StatementResult result = null;
            StringBuilder sb = new StringBuilder("name:$name,correspondingPackage:$correspondingPackage,correspondingClass:$correspondingClass,startId:$startId");
            if (v instanceof MethodDeclarationVertex) {
                MethodDeclarationVertex md = (MethodDeclarationVertex) v;
                sb.append(",returnType:$returnType,parameters:$parameters,callBackFlag:$callBackFlag,relevantView:$relevantView,inheritFlag:$inheritFlag,relevantBaseView:$relevantBaseView");
                result = tx.run(splice(md, sb.toString()), parameters("name", md.getName(), "correspondingPackage", md.getCorrespondingPackage(),
                        "correspondingClass", md.getCorrespondingClass(), "returnType", md.getReturnType(), "parameters", md.getParameters(),
                        "callBackFlag", md.getCallBackFlag(), "relevantView", md.getRelevantView(), "inheritFlag", md.getInheritFlag(), "relevantBaseView", md.getRelevantBaseView(), "startId", md.getStartId()));
            } else if (v instanceof MethodCallExprVertex) {
                MethodCallExprVertex mc = (MethodCallExprVertex) v;
                sb.append(",apiFlag:$apiFlag,objectClass:$objectClass,parameters:$parameters,inheritFlag:$inheritFlag,objectBaseClass:$objectBaseClass");
                result = tx.run(splice(mc, sb.toString()), parameters("name", mc.getName(), "correspondingPackage", mc.getCorrespondingPackage(),
                        "correspondingClass", mc.getCorrespondingClass(), "apiFlag", mc.getApiFlag(), "objectClass", mc.getObjectClass(), "parameters", mc.getParameters(),
                        "inheritFlag", mc.getInheritFlag(), "objectBaseClass", mc.getObjectBaseClass(), "startId", mc.getStartId()));
            } else {
                result = tx.run(splice(v, sb.toString()), parameters("name", v.getName(), "correspondingPackage", v.getCorrespondingPackage(),
                        "correspondingClass", v.getCorrespondingClass(), "startId", v.getStartId()));
            }
            return result.single().get(0).asLong();
        });
        v.setId(id);
        Neo4JUtil.closeSession(session);
        Neo4JUtil.closeDriver(driver);

    }

    @Override
    public void saveEdge(Long left, Long right, Edge e) {
        Driver driver = Neo4JUtil.getDriver();
        Session session = Neo4JUtil.getSession(driver);
        session.writeTransaction(tx -> {
            tx.run(String.format("Start a=node($left),b=node($right) create (a)-[r:%s{name:$name,sort:$sort}]->(b)", e.getLabel()),
                    parameters("left", left, "right", right, "name", e.getName(), "sort", e.getSort()));
            return true;
        });
        Neo4JUtil.closeSession(session);
        Neo4JUtil.closeDriver(driver);
    }

    @Override
    public void deleteProject(String label) {
        Driver driver = Neo4JUtil.getDriver();
        Session session = Neo4JUtil.getSession(driver);
        session.writeTransaction(tx -> {
            tx.run("match(n:" + label + ") detach delete n");
            return true;
        });
        Neo4JUtil.closeSession(session);
        Neo4JUtil.closeDriver(driver);
    }

    private String splice(Vertex v, String param) {
        String label = v.getLabels().stream().collect(Collectors.joining(":"));
        return format("CREATE(n:cspg:%s{%s}) return id(n)", label, param);
    }

    public static void main(String[] args) {
        MethodDeclarationVertex md = new MethodDeclarationVertex();
        md.addLabel("rr");
        md.setCallBackFlag(1);
        md.setName("ff");
        Vertex v = new Vertex();
        v.addLabel("test");
        v.addLabel("vertex");
        v.setCorrespondingClass("class");
        IVertexDAO vertexDAO = new VertexDAO();
        vertexDAO.saveEdge(0L, 20L, new Edge("e", "e"));

    }
}
