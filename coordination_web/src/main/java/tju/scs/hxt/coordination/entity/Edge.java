package tju.scs.hxt.coordination.entity;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by haoxiaotian on 2017/3/16 10:34.
 * 边
 */
public class Edge {

    private int source;
    private int target;

    public Edge() {
    }

    public Edge(int source, int target) {
        this.source = source;
        this.target = target;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;

        Edge edge = (Edge) o;

        if(source == edge.source && target == edge.target){
            return true;
        }

        if(source == edge.target && target == edge.source){
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "source=" + source +
                ", target=" + target +
                '}';
    }

    @Override
    public int hashCode() {
        int term1 = source * 31 + target;
        int term2 = target * 31 + source;
        return term1 + term2;
    }


    public static void main(String args[]){
        Set<Edge> set = new HashSet<>();
        set.add(new Edge(0,1));
        set.add(new Edge(0,1));
        set.add(new Edge(1,0));
        set.add(new Edge(2,3));
        set.add(new Edge(3,2));
        System.out.println(set);
    }
}
