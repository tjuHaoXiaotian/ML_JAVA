package tju.scs.hxt.coordination.dcop.agent;

import tju.scs.hxt.coordination.dcop.Config;

/**
 * Created by haoxiaotian on 2017/5/14 21:09.
 */
public class ObservedPolicy {
    private int agentId;

    private int [][] actionSelectedTimes; // actionSelectedTimes[i][j] action:i is me，action:j is partner

    private int actionNum;

    public ObservedPolicy(int agentId,int actionNum){
        this.agentId = agentId;
        this.actionNum = actionNum;
        this.initActionSelectedTimes(actionNum);
    }

    private void initActionSelectedTimes(int actionNum){
        actionSelectedTimes = new int[actionNum][actionNum+1];  // 多一列，统计总的次数
        for(int i = 0; i < actionNum;i++){
            for(int j = 0; j < actionNum;j++){
                actionSelectedTimes[i][j] = 0;
            }
        }
    }

    public double getProbability(int actionMe,int actionTarget){
        if(!recordedEnough(actionMe)){  // 还未统计过消息或统计的次数不够
            return 1.0 / actionNum;
        }else{
            return actionSelectedTimes[actionMe][actionTarget] * 1.0 / actionSelectedTimes[actionMe][actionNum];  // 当前次数 / 总次数
        }
    }

    private boolean recordedEnough(int actionMe){
        return (actionSelectedTimes[actionMe][actionNum] != 0 && actionSelectedTimes[actionMe][actionNum] >= Config.recordedTimes);
    }

    public int getAgentId() {
        return agentId;
    }

    public void selectActions(int actionMe,int actionTarget){
        // 1：对应次数
        actionSelectedTimes[actionMe][actionTarget] += 1;
        // 2：总次数
        actionSelectedTimes[actionMe][actionNum] += 1;
    }
}
