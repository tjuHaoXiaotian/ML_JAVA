package tju.scs.hxt.data;

import java.util.List;
import java.util.Set;

/**
 * Created by haoxiaotian on 2017/3/1 14:30.
 */
public class Property {

    private Object key;

    private final Set<Object> values;

    public Property(Object key,Set<Object> values) {
        this.key = key;
        this.values = values;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Set<Object> getValues() {
        return values;
    }

    public void addValue(Object value){
        this.values.add(value);
    }

    public void removeValue(Object value){
        this.values.remove(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Property)) return false;

        Property property = (Property) o;

        if (!key.equals(property.key)) return false;
        if (values != null ? !values.equals(property.values) : property.values != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
