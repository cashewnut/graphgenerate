package edu.fdu.se.graphgenerate.service;

import edu.fdu.se.graphgenerate.model.Edge;
import edu.fdu.se.graphgenerate.model.Vertex;

public interface IVertexService {

    void saveVertex(Vertex v);

    void saveEdge(Long left, Long right, Edge e);

    void deleteProject(String label);

}
