package tju.scs.hxt.coordination.dcop.agent;

import tju.scs.hxt.coordination.dcop.Config;
import tju.scs.hxt.coordination.dcop.web.GlobalCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hxt on 17-4-7.
 */
public class StopThread extends Thread{
    private int type;

    private int expId;

    private TrainingThread trainingThread;

    public StopThread(int type,int expId,TrainingThread trainingThread) {
        super();
        this.type = type;
        this.expId = expId;
        this.trainingThread = trainingThread;
    }

    // 目前所有 agent 的最有action统计信息
    private Map<Integer,Integer> actionSelection = new HashMap<Integer, Integer>();

    private int convergeTimes = 0;

    /**
     * 判断agent是否收敛：选择统一action的agent数量 > 98%
     * @return
     */
    private boolean stopMark = false;
    private int stopRound;
    private boolean isConverge(){
        if(!stopMark){
        boolean part1,part2;

        // 1：选择了同一个action
        actionSelection.clear();
        int bestAction,maxCount = 0;
        if(GlobalCache.getAgents(type,expId) != null) {
            for (Agent agent : GlobalCache.getAgents(type,expId)) {
//                bestAction = agent.getMaxUtilityAction().getAction();
                bestAction = agent.getMaxAction();
                actionSelection.put(bestAction, actionSelection.get(bestAction) == null ? 1 : actionSelection.get(bestAction) + 1);
                if (actionSelection.get(bestAction) > maxCount) {
                    maxCount = actionSelection.get(bestAction);
                }
            }
            part1 = ((double)maxCount) / GlobalCache.getAgents(type,expId).size() > 0.98;
        }else{
            part1 = false;
        }
            if(!part1) {
                System.out.println("agents 没有选择到同一个action");
            }

            // 2：平均收益是否逼近 1
            part2 = getAvgReward() > 0.98;

            if(!part2){
                System.out.println("agents 平均收益没有 == 1");
            }

            if(part1 && part2){
                stopMark = true;
                stopRound = trainingThread.getCurrentRound();
            }
            return false;
        }else{
            if(trainingThread.getCurrentRound() - stopRound > Config.rounds_after_converge[expId]){
                return true;
            }else{
                return false;
            }
        }
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
        if(GlobalCache.getAgents(type,expId) == null){
            return avgReward;
        }
        for(Agent agent:GlobalCache.getAgents(type,expId)){
            if(GlobalCache.isConverge(type,expId)){
                return 1;
            }
            avgReward += agent.getCurrentPayoff();
        }

        return avgReward / GlobalCache.getAgents(type,expId).size();
    }

}
