package tju.scs.hxt.coordination.entity;

import tju.scs.hxt.coordination.network.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by haoxiaotian on 2017/3/16 10:30.
 */
public class Network {

    private List<Node> nodes;

    private Set<Edge> edges;

    public Network() {
        this.nodes = new ArrayList<>();
        this.edges = new HashSet<>();
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public Set<Edge> getEdges() {
        return edges;
    }

    public void setEdges(Set<Edge> edges) {
        this.edges = edges;
    }

    public void addNode(Node node){
        this.nodes.add(node);
        Edge edge = null;
        for(Integer id:node.getNeighborsId()){
            edge = new Edge(node.getId(),id);
            this.edges.add(edge);
        }
    }
}
