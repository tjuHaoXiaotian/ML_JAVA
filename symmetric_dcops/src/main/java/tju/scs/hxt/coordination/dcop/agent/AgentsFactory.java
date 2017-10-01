package tju.scs.hxt.coordination.dcop.agent;

import tju.scs.hxt.coordination.dcop.agent.max_plus.MaxPlusAgent;
import tju.scs.hxt.coordination.dcop.agent.max_sum.MaxSumAgent;

/**
 * Created by haoxiaotian on 2017/5/25 17:50.
 */
public class AgentsFactory {

    public static Agent getAgent(int expId,int id, int actionNum, double exploreRate, double learningRate, int type){
        switch (expId){
            case 0:
                return new MaxPlusAgent(id,actionNum,exploreRate,learningRate,type);

            default:
                return new MaxSumAgent(id,actionNum,exploreRate,learningRate,type);
        }
    }

}
