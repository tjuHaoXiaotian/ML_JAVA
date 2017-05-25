package tju.scs.hxt.coordination.dcop.agent;

import tju.scs.hxt.coordination.dcop.Analyze;
import tju.scs.hxt.coordination.dcop.Config;
import tju.scs.hxt.coordination.dcop.network.Node;
import tju.scs.hxt.coordination.dcop.web.GlobalCache;

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
            // 初始化当前网络中的配置
            initConfiguration();

            // 当网络中 agent 没有收敛
            while (!GlobalCache.isConverge(type,expId)) {
                runNextRound();
            }

            // 更新缓存的平均收益
            if(!cache.isEmpty()){
                avgPayoffs.addAll(cache);
                GlobalCache.setAvgReward(type,expId,avgPayoffs);
            }
        }finally {
            endGate.countDown();
        }
    }

    // 统计agents平均reward
    ArrayList<GlobalCache.AvgReward> avgPayoffs = new ArrayList<>();


    private ArrayList<GlobalCache.AvgReward> cache = new ArrayList<>();

    /**
     * run every episode
     */
    private void runNextRound(){
        // 1：run dcop(max-plus) through the whole network
        training();


        // 2： 统计 round - avg reward 信息
        round++;
        System.out.println(type+":"+expId+" round: " + round + ",explore rate: "+getAvgExploreRate());
        Analyze.incRoundTimes(type,expId,round);

        int num = ((expId == 0||expId == 1)?1:5);  // 记录曲线打点
        int num2 = ((expId == 0||expId == 1)?20:20);  // delta 相关
        if(round % num == 0){
            if(round % num2 == 0){
                Config.deltaExploreRate[type][expId] = Config.deltaExploreRate[type][expId] * 2;
            }
            avgPayoffs.add(new GlobalCache.AvgReward(round, getAvgReward()));
            GlobalCache.setAvgReward(type,expId,avgPayoffs);
            cache.clear();
        }else{

            cache.add(new GlobalCache.AvgReward(round, getAvgReward()));
        }
    }

    public int getCurrentRound(){
        return round;
    }

    private void initConfiguration(){
        for(Agent agent:GlobalCache.getAgents(type,expId)){
            // 1：初始化与各 agent 的 q-table
            agent.initQTables();

            // 2：初始化对各 agent 的 action 统计
            agent.initObservedPolicy();

            // 3：初始化各个 agent 的 coordination set
            agent.initCoordinationSet();

            // 4:初始化网络中，avgReward
            GlobalCache.initAvgReward(type);
        }
    }


    private void training(){
        if(expId == 0 || expId == 1){
            // 1：DCOP 同步消息
            runDCOP(false);
        }

        // 2：选取 action，获取reward，更新 Q table
        for(Agent agent: GlobalCache.getAgents(type,expId)){
            agent.training(expId);
        }
    }

    private void runDCOP(boolean anyTimeExtension){
        int deadline = Config.deadline;  //TODO:设计一个deadline去衡量当前计算是否超时
        int currentTime = 0;
        double m = -1000000;
        boolean fixedPoint = false,differEnough;  // 停止标志
        // 1：如果仍然需要计算，并且允许计算
        while(!fixedPoint && currentTime < deadline){
            // run one iteration
            fixedPoint = true;
            // 1：传播信息
            for(Agent agent:GlobalCache.getAgents(type,expId)){
                // 向 coordination set 中的每一个 agent 发送信息
                for(Agent neighbor:agent.getCoordinationSet()){
                    differEnough = agent.sendMessageTo(neighbor,expId);
                    if(differEnough){
                        fixedPoint = false;
                    }
                }
            }

            // 2：设置当前最好action
            for(Agent agent:GlobalCache.getAgents(type,expId)){
                Agent.ActionUtility actionUtility = agent.getMaxUtilityAction();
                if(anyTimeExtension){
                    if(actionUtility.getUtility() > m){
//                        System.out.println("max utility: " + actionUtility.getUtility());
                        agent.setMaxAction(actionUtility.getAction());
                        m = actionUtility.getUtility();
                    }
//                    System.out.println("max utility: " + actionUtility.getUtility() + ";     m" + m);

                }else{
                    agent.setMaxAction(actionUtility.getAction());
                }
            }
            currentTime++;
        }
    }

    private double getAvgReward(){
        double avgReward = 0;
        for(Agent agent:GlobalCache.getAgents(type,expId)){
            avgReward += agent.getCurrentPayoff();
        }

        return avgReward / GlobalCache.getAgents(type,expId).size();
    }

    private double getAvgExploreRate(){
        double avgExploreRate = 0;
        for(Agent agent:GlobalCache.getAgents(type,expId)){
            avgExploreRate += agent.getExploreRate();
        }

        return avgExploreRate / GlobalCache.getAgents(type,expId).size();
    }


    public void setEndGate(CountDownLatch endGate) {
        this.endGate = endGate;
    }
}
