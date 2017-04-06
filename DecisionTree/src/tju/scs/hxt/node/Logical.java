package tju.scs.hxt.node;

/**
 * Created by haoxiaotian on 2017/3/1 13:41.
 */
public enum  Logical {


    BIGGER("bigger",">"),
    SMALLER("smaller","<"),
    EQUAL("equal","=="),
    BIGGER_OR_EQUAL("bigger_or_equal",">="),
    SMALLER_OR_EQUAL("smaller_or_equal","<=");

    private Logical(String name,String val){
        this.name = name;
        this.val = val;
    }

    private String name;

    private String val;


    @Override
    public String toString() {
        return val;
    }
}
