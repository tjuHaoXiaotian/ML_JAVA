package tju.scs.hxt.coordination.symmetric.agent;

/**
 * 通信连接请求
 * Created by haoxiaotian on 2017/3/16 16:35.
 */
public class Request {
    // 通信发起者
    private Agent source;
    // 目标接收方
    private Agent target;
    // 通信优先级
    private double priority;

    // 选择此action的回报
    private double fmqValueRow;
    private double fmqValueColumn;

    // row action
    private int rowAction;

    // column action
    private int columnAction;

    // agent 当前策略通知给对方，以衡量差异
    private double [][] policy;

    public Request() {
    }

    public Request(Agent source, Agent target, int rowAction,int columnAction,double fmqValueRow,double fmqValueColumn,double priority,double [][] policy) {
        this.source = source;
        this.target = target;
        this.rowAction = rowAction;
        this.columnAction = columnAction;
        this.fmqValueRow = fmqValueRow;
        this.fmqValueColumn = fmqValueColumn;
        this.priority = priority;
        this.policy = getPolicyImage(policy);
    }

    private double [][] getPolicyImage(double [][] policy){
        this.policy = new double[policy.length][policy[0].length];
        for(int i = 0;i < policy.length;i++){
            for(int j = 0; j < policy[i].length;j++){
                this.policy[i][j] = policy[i][j];
            }
        }
        return this.policy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;

        Request request = (Request) o;

        if (source != null ? !source.equals(request.source) : request.source != null) return false;
        if (target != null ? !target.equals(request.target) : request.target != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Request{" +
                "source=" + source.getId() +
                ", target=" + target.getId() +
                ", priority=" + priority +
                '}';
    }


    public Agent getSource() {
        return source;
    }

    public void setSource(Agent source) {
        this.source = source;
    }

    public Agent getTarget() {
        return target;
    }

    public void setTarget(Agent target) {
        this.target = target;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public int getRowAction() {
        return rowAction;
    }

    public void setRowAction(int rowAction) {
        this.rowAction = rowAction;
    }

    public int getColumnAction() {
        return columnAction;
    }

    public void setColumnAction(int columnAction) {
        this.columnAction = columnAction;
    }

    public double getFmqValueRow() {
        return fmqValueRow;
    }

    public void setFmqValueRow(double fmqValueRow) {
        this.fmqValueRow = fmqValueRow;
    }

    public double getFmqValueColumn() {
        return fmqValueColumn;
    }

    public void setFmqValueColumn(double fmqValueColumn) {
        this.fmqValueColumn = fmqValueColumn;
    }

    public double[][] getPolicy() {
        return policy;
    }

    public void setPolicy(double[][] policy) {
        this.policy = policy;
    }
}
