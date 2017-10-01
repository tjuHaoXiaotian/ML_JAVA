package tju.scs.hxt.coordination.dcop.agent.max_sum;

import tju.scs.hxt.coordination.dcop.agent.Agent;
import tju.scs.hxt.coordination.dcop.network.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by haoxiaotian on 2017/5/25 20:37.
 */
public class MaxSumAgent extends Agent{

    public MaxSumAgent(int id, int actionNum, double exploreRate, double learningRate, int type) {
        super(id, actionNum, exploreRate, learningRate, type);
    }

    Map<String,Double> qTable = new HashMap<>();

    private int[] qOrder;  // (agent order1)action1,(agent order2)action2,(agent order3)action3 = 5

    public void initQTable(){
        int neighborNum = getNeighborsSize();
        List<Node> neighbors = getNeighbors();
        // 初始化顺序
        qOrder = new int[neighborNum+1];
        qOrder[0] = this.getId();  //  自己是第一位

        for(int i = 0;i < neighborNum;i++){
            qOrder[i+1] = neighbors.get(i).getId();
        }
    }

    /**
     *
     * @param agent_action <agentId,actionSelected> 谁 选了 什么action
     * @param reward
     */
    public void setQ(Map<Integer,Integer> agent_action,double reward){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0;i < getNeighborsSize()+1;i++){
            stringBuilder.append(agent_action.get(qOrder[i]));
            if(i!=getNeighborsSize()){
                stringBuilder.append(",");
            }
        }
        qTable.put(stringBuilder.toString(),reward);
    }

//    public
}
