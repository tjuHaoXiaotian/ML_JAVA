package tju.scs.hxt.coordination.agent;

/**
 * 统计与某个邻居agent的合作信息
 * Created by haoxiaotian on 2017/4/7 0:17.
 */
public class Statistics {

    // 与某个邻居合作，出现正 reward 次数
    private int positiveTimes;

    // 与某个邻居合作的总次数
    private int totalTimes;

    public Statistics() {
    }

    public Statistics(int positiveTimes, int totalTimes) {
        this.positiveTimes = positiveTimes;
        this.totalTimes = totalTimes;
    }

    public int getPositiveTimes() {
        return positiveTimes;
    }

    public void setPositiveTimes(int positiveTimes) {
        this.positiveTimes = positiveTimes;
    }

    public int getTotalTimes() {
        return totalTimes;
    }

    public void setTotalTimes(int totalTimes) {
        this.totalTimes = totalTimes;
    }

    public void increasePositive(){
        this.positiveTimes++;
    }

    public void increaseTotal(){
        this.totalTimes++;
    }

    public double getPositiveFrequency(){
//        System.out.println(((double)this.positiveTimes) / this.totalTimes);
        return ((double)this.positiveTimes) / this.totalTimes;
    }

    public double getNegativeFrequency(){
        return (1 - getPositiveFrequency());
    }
}
