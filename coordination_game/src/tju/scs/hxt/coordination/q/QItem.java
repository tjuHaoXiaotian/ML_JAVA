package tju.scs.hxt.coordination.q;

/**
 * Created by haoxiaotian on 2017/3/18 17:37.
 */
public class QItem {
    private double qValue;
    private double maxQValue;
    private int maxQTimes;
    private int totalTimes;
    private double frequency;
    private double fmq;

    public QItem() {
    }

    public void setFrequency(){
        this.setFrequency(((double)this.maxQTimes) / this.totalTimes);
    }

    public void increaseMaxTimes() {
        this.maxQTimes++;
    }

    public void increaseTotalTimes(){
        this.totalTimes++;
    }

    public void updateFMQ(double factor) {
        fmq = qValue + frequency * maxQValue * factor;
    }

    @Override
    public String toString() {
        return "QItem{" +
                "qValue=" + qValue +
                ", maxQValue=" + maxQValue +
                ", frequency=" + frequency +
                ", fmq=" + fmq +
                '}';
    }

    public double getqValue() {
        return qValue;
    }

    public void setqValue(double qValue) {
        this.qValue = qValue;
    }

    public double getMaxQValue() {
        return maxQValue;
    }

    public void setMaxQValue(double maxQValue) {
        this.maxQValue = maxQValue;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public double getFmq() {
        return fmq;
    }

    public void setFmq(double fmq) {
        this.fmq = fmq;
    }

    public int getMaxQTimes() {
        return maxQTimes;
    }

    public void setMaxQTimes(int maxQTimes) {
        this.maxQTimes = maxQTimes;
    }

    public int getTotalTimes() {
        return totalTimes;
    }

    public void setTotalTimes(int totalTimes) {
        this.totalTimes = totalTimes;
    }
}
