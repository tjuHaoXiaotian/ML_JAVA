package tju.scs.hxt.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by haoxiaotian on 2017/3/1 14:11.
 */
public class Data {


    // 锁对象
    private final Object lock = new Object();

    private int id;

    private HashMap<Object,Object> properties;

    public Data() {
    }

    public void addProperty(Object key,Object val){
        if(this.properties == null){
            synchronized (lock){
                if(this.properties == null){
                    this.properties = new HashMap<Object, Object>();
                }
            }
        }

        this.properties.put(key,val);
    }

    public void removeProperty(Property property){
        this.properties.remove(property);
    }

    public HashMap<Object, Object> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<Object, Object> properties) {
        this.properties = properties;
    }

    public Object getProperty(Object label){
        return this.properties.get(label);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
//        return "Data{" +
//                "properties=" + properties +
//                "}\n";
        return id+"";
    }
}
