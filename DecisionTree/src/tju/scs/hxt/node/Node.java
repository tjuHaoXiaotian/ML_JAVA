package tju.scs.hxt.node;

import java.util.List;

/**
 * Created by haoxiaotian on 2017/3/1 13:35.
 */
public abstract class Node {

    protected int parent;

    public Node() {
    }

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }
}
