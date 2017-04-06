package tju.scs.hxt.coordination.network;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haoxiaotian on 2017/3/14 11:07.
 */
public class Node {

    // node 编号
    private int id;

    // node 优先级
    private int priority;

    // 网络中节点的总个数
    @JsonIgnore
    private int totalNodes;

    @JsonIgnore
    private List<Integer> neighborsId;

    @JsonIgnore

    private List<Node> neighbors;

    @JsonIgnore
    private final Object lock = new Object();

    public Node() {
    }

    public Node(int id,int totalNodes) {
        this.id = id;
        this.totalNodes = totalNodes;
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
        updatePriority();
    }

    public void removeNeighbor(Node node){
        this.neighbors.remove(node);
        this.removeNeighborId(node.getId());
        updatePriority();
    }

    /**
     * 当成员数量变动时，更新优先级
     */
    private void updatePriority(){
        this.priority = totalNodes * getNeighborsSize() + id;
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
        return "Node[" + this.id + ']' + "——>" + this.neighborsId;
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
