package tju.scs.hxt.coordination.symmetric.agent;

import tju.scs.hxt.coordination.symmetric.web.GlobalCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by haoxiaotian on 2017/4/17 20:33.
 */
public class TrainingThread extends Thread {

    private int type;

    private int expId;

    private CountDownLatch endGate;

    private int round = 0;

    public TrainingThread(int type,int expId) {
        super();
        this.type = type;
        this.expId = expId;
    }

    @Override
    public void run() {
        try {
            super.run();
//            System.out.println("thread run");
            GlobalCache.setAvgReward(type,expId,avgPayoffs);

            while (!GlobalCache.isConverge(type,expId)) {
//                System.out.println("not converge");
                runNextRound();
            }
            if(!cache.isEmpty()){
                avgPayoffs.addAll(cache);
            }
        }finally {
            endGate.countDown();
        }
    }

    // 统计agents平均reward
    ArrayList<GlobalCache.AvgReward> avgPayoffs = new ArrayList<>();


    private ArrayList<GlobalCache.AvgReward> cache = new ArrayList<>();
    private void runNextRound(){
        training();
        round++;
        if(round % 100 == 0){
            avgPayoffs.add(new GlobalCache.AvgReward(round, getAvgReward()));
            cache.clear();
        }else{
            cache.add(new GlobalCache.AvgReward(round, getAvgReward()));
        }
    }

    private double getAvgReward(){
        double avgReward = 0;
        for(Agent agent:GlobalCache.getAgents(type,expId)){
            avgReward += agent.getCurrentPayoff();
        }

        return avgReward / GlobalCache.getAgents(type,expId).size();
    }

    private void training(){
        for(Agent agent: GlobalCache.getAgents(type,expId)){
            agent.training(expId);
        }
    }

    public void setEndGate(CountDownLatch endGate) {
        this.endGate = endGate;
    }
}
