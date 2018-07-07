package edu.fdu.se.graphgenerate.persistence;

import edu.fdu.se.graphgenerate.model.Edge;
import edu.fdu.se.graphgenerate.model.Vertex;

public interface IVertexDAO {

    void saveVertex(Vertex v);

    void saveEdge(Long left, Long right, Edge e);

    void deleteProject(String label);

}
