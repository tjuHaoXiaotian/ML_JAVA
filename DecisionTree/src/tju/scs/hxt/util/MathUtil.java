package tju.scs.hxt.util;

/**
 * Created by haoxiaotian on 2017/3/1 22:18.
 */
public class MathUtil {


    /**
     * 计算对数
     * @param value  计算值
     * @param base  底
     * @return
     */
    static public double log(double value, double base) {
        return Math.log(value) / Math.log(base);
    }
}
