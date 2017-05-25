package tju.scs.hxt.coordination.dcop.network;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haoxiaotian on 2017/3/14 11:07.
 */
public class Node {

    // node 编号
    private int id;

    // node 优先级：centrality
    /**
     * default: degree centrality (1)
     * closeness centrality (2) 到其他所有点的最短距离
     * betweenness centrality (3) 每个点出现在其他所有点对的最短路径上的次数
     * page rank centrality (4)
     * cross-clique centrality (5) 跨派别
     */
    private double centrality;

    @JsonIgnore
    private List<Integer> neighborsId;

    @JsonIgnore
    private List<Node> neighbors;

    @JsonIgnore
    private final Object lock = new Object();

    public Node() {
    }

    public Node(int id) {
        this.id = id;
        neighbors = new ArrayList<Node>();
    }


    @JsonIgnore
    public int getNeighborsSize(){
        if(neighborsId == null){
            return 0;
        }
        return neighborsId.size();
    }

    public boolean hasNeighbor(Node node){
        if(neighbors == null){
            return false;
        }
        return neighbors.contains(node);
    }

    public void addNeighbor(Node node){
        if(this.neighbors == null){
            synchronized (this.lock){
                if(this.neighbors == null){
                    this.neighbors = new ArrayList<Node>();
                }
            }
        }

        this.neighbors.add(node);
        addNeighborId(node.getId());
    }

    public void removeNeighbor(Node node){
        this.neighbors.remove(node);
        this.removeNeighborId(node.getId());
    }

    private void addNeighborId(int id){
        if(this.neighborsId == null){
            synchronized (this.lock){
                if(this.neighborsId == null){
                    this.neighborsId = new ArrayList<Integer>();
                }
            }
        }

        this.neighborsId.add(id);
    }

    private void removeNeighborId(int id){
        this.neighborsId.remove((Integer)id);
    }


    @Override
    public String toString() {
//        return "Node[" + this.id + ','+this.centrality + ']' + "——>" + this.neighborsId;
        return this.id+"";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;

        if (id != node.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public List<Node> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<Node> neighbors) {
        this.neighbors = neighbors;
    }

    public List<Integer> getNeighborsId() {
        return neighborsId;
    }

    public void setNeighborsId(List<Integer> neighborsId) {
        this.neighborsId = neighborsId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getCentrality() {
        return centrality;
    }

    public void setCentrality(double centrality) {
        this.centrality = centrality;
    }
}
