package tju.scs.hxt.coordination.dcop.agent;

import tju.scs.hxt.coordination.dcop.web.GlobalCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hxt on 17-4-7.
 */
public class StopThread extends Thread{
    private int type;

    private int expId;

    public StopThread(int type,int expId) {
        super();
        this.type = type;
        this.expId = expId;
    }

    // 目前所有 agent 的最有action统计信息
    private Map<Integer,Integer> actionSelection = new HashMap<Integer, Integer>();

    private int convergeTimes = 0;

    /**
     * 判断agent是否收敛：选择统一action的agent数量 > 98%
     * @return
     */
    private boolean isConverge(){
        boolean part1,part2;

        // 1：选择了同一个action
        actionSelection.clear();
        int bestAction,maxCount = 0;
        if(GlobalCache.getAgents(type,expId) != null) {
            for (Agent agent : GlobalCache.getAgents(type,expId)) {
                bestAction = agent.getMaxUtilityAction().getAction();
                actionSelection.put(bestAction, actionSelection.get(bestAction) == null ? 1 : actionSelection.get(bestAction) + 1);
                if (actionSelection.get(bestAction) > maxCount) {
                    maxCount = actionSelection.get(bestAction);
                }
            }
            part1 = ((double)maxCount) / GlobalCache.getAgents(type,expId).size() > 0.99;
        }else{
            part1 = false;
        }

        if(part1){  // 各个agent已经选到了同一个action上

        }

        // 2：平均收益是否逼近 1
        part2 = getAvgReward() > 0.99;

        return part1 && part2;
    }

    @Override
    public void run(){
        // 当目标网络未收敛，即自己工作没有完成
        while (!GlobalCache.isConverge(type,expId)){
            if(isConverge()){
                GlobalCache.setConverge(type,expId,true);
            }

            try {
                // 睡眠 5 s
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private double getAvgReward(){
        double avgReward = 0;
        for(Agent agent:GlobalCache.getAgents(type,expId)){
            if(GlobalCache.isConverge(type,expId)){
                return 1;
            }
            avgReward += agent.getCurrentPayoff();
        }

        return avgReward / GlobalCache.getAgents(type,expId).size();
    }

}
