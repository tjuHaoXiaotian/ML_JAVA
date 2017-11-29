package dcop.agent;

import java.util.Arrays;

/**
 * Created by haoxiaotian on 2017/5/12 11:48.
 */
public class QTable {

    private int agentId;

    private double [][] q;  // q[i][j] action:i is me，action:j is partner

    public QTable(int agentId,int actionNum){
        this.agentId = agentId;
        this.initQTable(actionNum);
    }

    private void initQTable(int actionNum){
        q = new double[actionNum][actionNum];
        for(int i = 0; i < actionNum;i++){
            for(int j = 0; j < actionNum;j++){
                q[i][j] = 0;
            }
        }
    }

    public double[][] getQ() {
        return q;
    }

    public void setQ(double[][] q) {
        this.q = q;
    }

    public int getAgentId() {
        return agentId;
    }

    public void setAgentId(int agentId) {
        this.agentId = agentId;
    }

    @Override
    public String toString() {
        return "QTable{" +
                "agentId=" + agentId +
                ", q=" + Arrays.toString(q) +
                '}';
    }
}
