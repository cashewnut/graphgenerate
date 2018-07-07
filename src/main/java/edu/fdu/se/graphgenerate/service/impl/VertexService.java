package edu.fdu.se.graphgenerate.service.impl;

import edu.fdu.se.graphgenerate.model.Edge;
import edu.fdu.se.graphgenerate.model.Vertex;
import edu.fdu.se.graphgenerate.persistence.IVertexDAO;
import edu.fdu.se.graphgenerate.persistence.impl.VertexDAO;
import edu.fdu.se.graphgenerate.service.IVertexService;

public class VertexService implements IVertexService {

    private IVertexDAO vertexDAO;

    public VertexService() {
        vertexDAO = new VertexDAO();
    }

    @Override
    public void saveVertex(Vertex v) {
        vertexDAO.saveVertex(v);
        if(v.getEndId() == null)
            v.setEndId(v.getId());
    }

    @Override
    public void saveEdge(Long left, Long right, Edge e) {
        vertexDAO.saveEdge(left,right,e);
    }

    @Override
    public void deleteProject(String label) {
        vertexDAO.deleteProject(label);
    }
}
