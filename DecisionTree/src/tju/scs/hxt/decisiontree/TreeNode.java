package tju.scs.hxt.decisiontree;

import tju.scs.hxt.node.Logical;
import tju.scs.hxt.node.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haoxiaotian on 2017/3/1 13:58.
 */
public class TreeNode extends Node {

    // 锁对象
    private final Object lock = new Object();

    private int id;

    private boolean isLeaf;
    private Object classifyResult;

    private List<Node> children;

    private Object key;

    private Object branchValue;

    private Logical logic;

    private int height;

    public TreeNode(){
        super();
    }

    /**
     * 添加一个 child
     * @param child
     */
    public void addChild(TreeNode child){
        // 判断是否未初始化
        if(this.children == null){
            synchronized (lock){
                if(this.children == null){
                    this.children = new ArrayList<Node>();
                }
            }
        }

        this.children.add(0, child);
    }

    /**
     * 删除一个 child
     * @param child
     */
    public void removeChild(TreeNode child){
        this.children.remove(child);  // 注意容器元素重写 equals()
    }




//    @Override
//    public String toString() {
//        if(isLeaf){
//            return "leaf node:"+classifyResult;
//        }
//    }


    @Override
    public String toString() {
//        return "TreeNode{" +
//                "id=" + id +
//                "isLeaf=" + isLeaf +
//                ", classifyResult=" + classifyResult +
//                ", height=" + height +
//                ", key=" + key +
//                ", branchValue=" + branchValue +
//                ", logic=" + logic +
//                ", children=" + children +
//                ", parent =" + parent +
//                '}';

        if(isLeaf){
            return "TreeNode{" +
                    "id=" + id +
                    ", classifyResult=" + classifyResult +
                    ", height=" + height +
                    ", parent =" + parent +
                    ", branchValue=" + branchValue +
                    '}';
        }else{
           return  "TreeNode{" +
                    "id=" + id +
                    ", height=" + height +
                    ", parent =" + parent +
                    ", key=" + key +
                    ", branchValue=" + branchValue +
                    ", logic=" + logic +
                    ", children=\n" + children +
                    '}';
        }

//        if(isLeaf){
//            return  id +
//                    ": " +branchValue +
////                    ", height=" + height +
//                    ", " + classifyResult;
//        }else{
//            return  id +": " + (branchValue == null ? "":"" + branchValue) + "   "
//                    +  key + logic;
//        }
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Object getBranchValue() {
        return branchValue;
    }

    public void setBranchValue(Object branchValue) {
        this.branchValue = branchValue;
    }

    public Logical getLogic() {
        return logic;
    }

    public void setLogic(Logical logic) {
        this.logic = logic;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public Object getClassifyResult() {
        return classifyResult;
    }

    public void setClassifyResult(Object classifyResult) {
        this.classifyResult = classifyResult;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCondition(){
        if(isLeaf()){
            return ""+classifyResult;
        }else{
            return ""  + key + logic;
        }
    }
}
