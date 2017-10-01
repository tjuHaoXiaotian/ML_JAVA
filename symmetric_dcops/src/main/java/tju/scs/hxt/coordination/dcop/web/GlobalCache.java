package tju.scs.hxt.coordination.dcop.web;

import tju.scs.hxt.coordination.dcop.Analyze;
import tju.scs.hxt.coordination.dcop.Config;
import tju.scs.hxt.coordination.dcop.agent.Agent;
import tju.scs.hxt.coordination.dcop.network.Centrality;
import tju.scs.hxt.coordination.dcop.network.Node;
import tju.scs.hxt.coordination.dcop.web.entity.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haoxiaotian on 2017/3/18 23:18.
 */
public class GlobalCache {

    // 每个网络 contrast_experiment 组对比试验
    private static ArrayList<ArrayList<Agent>>[] experiment_agents = new ArrayList[Config.network_type];

    private static ArrayList<ArrayList<AvgReward>>[] experiment_avgRewards = new ArrayList[Config.network_type];


    /**
     * 统计每一轮的平均收益
     * round
     * avg reward
     */
    public static class AvgReward{
        private int round;
        private double reward;

        public AvgReward(int round, double reward) {
            this.round = round;
            this.reward = reward;
        }

        public int getRound() {
            return round;
        }

        public void setRound(int round) {
            this.round = round;
        }

        public double getReward() {
            return reward;
        }

        public void setReward(double reward) {
            this.reward = reward;
        }
    }

    static {
        for (int i = 0; i < Config.network_type; i++) {
            experiment_agents[i] = null;
            experiment_avgRewards[i] = null;
        }
    }

    // for 可视化，网络拓扑结构
    private static Network[] networks = new Network[6];

    // 训练线程是否启动
    private static boolean[] runningState = new boolean[6];

    // 不同网络结构对应的锁
    private final static Object lock = new Object();
    private final static Object lock1 = new Object();
    private final static Object lock2 = new Object();
    private final static Object lock3 = new Object();
    private final static Object lock4 = new Object();
    private final static Object lock5 = new Object();

    /**
     * get lock，for 多线程：获取保护锁
     * @param type
     * @return
     */
    public static Object getLock(int type){
        switch (type){
            case 0:
                return lock;
            case 1:
                return lock1;
            case 2:
                return lock2;
            case 3:
                return lock3;
            case 4:
                return lock4;
            case 5:
                return lock5;
            default:
                return lock;
        }
    }

    /**
     * 全局缓存 agent 信息
     * @param parameter
     * @param type
     */
    public static void createGlobalCache(ArrayList<Agent> parameter,int type){
        ArrayList<ArrayList<Agent>> contrast_experiments = new ArrayList<ArrayList<Agent>>();
            synchronized (getLock(type)){
                if(experiment_agents[type] == null){  // 还未初始化
                    contrast_experiments.add(0,parameter);  // 添加目标试验：0
                    // 设置权重信息
                    Centrality.setCentrality(parameter);  // 设置网络中 agent 的centrality

                    for(int expId = 1; expId < Config.contrast_experiment;expId++){
                        // 添加对比试验
                        contrast_experiments.add(expId, tju.scs.hxt.coordination.dcop.network.Network.deepCopy(expId,parameter));
                    }

                    // 生成网络拓扑图，for 界面展示
                    networks[type] = new Network();
                    for(Node node:parameter){
                        networks[type].addNode(node);
                    }
                    experiment_agents[type] = contrast_experiments;
                }
            }
    }

    public static ArrayList<ArrayList<Agent>> getAgents(int type) {
        return experiment_agents[type];
    }

    public static List<Agent> getAgents(int type,int expId) {
        if(experiment_agents[type] == null){
            return null;
        }
        return experiment_agents[type].get(expId);
    }

    /**
     * 点击重置
      */
    public static void clearAgents(int type){
        synchronized (getLock(type)){
            // 1：网络结构清空
            experiment_agents[type].clear();
            experiment_avgRewards[type].clear();
            experiment_agents[type] = null;
            experiment_avgRewards[type] = null;

            // 2：初始化
            reInit(type);

            // 3：初始化 DeltaExploreRate
            Config.resetDeltaExploreRate();
        }
    }

    public static void reInit(int type){
        // 2：初始化统计信息
        Analyze.init(type);
        // 3： 设置收敛状态为为收敛
        for(int i = 0; i < Config.contrast_experiment;i++){
            setConverge(type,i,false);
        }
        // 4：设置线程运行状态为 false
        setRunningState(type,false);
    }

    public static Network getNetworks(int type) {
        return networks[type];
    }

    public static boolean isRunningState(int type) {
        synchronized (getLock(type)) {
            return runningState[type];
        }
    }

    /**
     * 标记对应线程的运行状态
     * @param type
     * @param runningState
     */
    public static void setRunningState(int type,boolean runningState) {
        synchronized (getLock(type)) {
            GlobalCache.runningState[type] = runningState;
        }
    }

    public static void initAvgReward(int type){
        if(GlobalCache.experiment_avgRewards[type] == null){
            synchronized (getLock(type)){
                if(GlobalCache.experiment_avgRewards[type] == null){
                    GlobalCache.experiment_avgRewards[type] = new ArrayList<ArrayList<AvgReward>>(Config.contrast_experiment);
                    for(int i = 0;i < Config.contrast_experiment;i++){
                        GlobalCache.experiment_avgRewards[type].add(new ArrayList<AvgReward>());
                    }
                }
            }
        }
    }

    /**
     * 设置平均收益情况
     * @param type 网络拓扑类型
     * @param expId 实验 id：target 实验 or 对比实验
     * @param avgRewards
     */
    public static void setAvgReward(int type,int expId,ArrayList<AvgReward> avgRewards) {
        if(GlobalCache.experiment_avgRewards[type] == null){
            synchronized (getLock(type)){
                if(GlobalCache.experiment_avgRewards[type] == null){
                    GlobalCache.experiment_avgRewards[type] = new ArrayList<ArrayList<AvgReward>>(Config.contrast_experiment);
                    for(int i = 0;i < Config.contrast_experiment;i++){
                        GlobalCache.experiment_avgRewards[type].add(new ArrayList<AvgReward>());
                    }
                }
            }
        }
        GlobalCache.experiment_avgRewards[type].get(expId).clear();
        GlobalCache.experiment_avgRewards[type].get(expId).addAll(avgRewards);
//        GlobalCache.experiment_avgRewards[type].set(expId,avgRewards);
    }

    public static List<ArrayList<AvgReward>> getAvgReward(int type) {
        return GlobalCache.experiment_avgRewards[type];
    }

    // 默认会初始化
    private static boolean [] converge_0 = new boolean[Config.contrast_experiment];
    private static boolean [] converge_1 = new boolean[Config.contrast_experiment];
    private static boolean [] converge_2 = new boolean[Config.contrast_experiment];
    private static boolean [] converge_3 = new boolean[Config.contrast_experiment];
    private static boolean [] converge_4 = new boolean[Config.contrast_experiment];
    private static boolean [] converge_5 = new boolean[Config.contrast_experiment];

    /**
     * 判断当前网络结构下，对应实验下，agent 是否收敛
     * @param type 网络结构类型
     * @return
     */
    public static boolean isConverge(int type,int expId) {  // 判断是否收敛于同一个action
            switch (type) {
                case 0:
                    return converge_0[expId];
                case 1:
                    return converge_1[expId];
                case 2:
                    return converge_2[expId];
                case 3:
                    return converge_3[expId];
                case 4:
                    return converge_4[expId];
                case 5:
                    return converge_5[expId];
                default:
                    return converge_3[expId];
            }
    }

    public static void setConverge(int type,boolean value){
        for(int i = 0; i < Config.contrast_experiment;i++){
            setConverge(type,i,value);
        }
    }

    /**
     * 对应网络结构下：设置其状态为收敛
     * @param type
     */
    public static void setConverge(int type,int expId,boolean value){
        switch (type){
            case 0:
                converge_0[expId] = value;
                break;
            case 1:
                converge_1[expId] = value;
                break;
            case 2:
                converge_2[expId] = value;
                break;
            case 3:
                converge_3[expId] = value;
                break;
            case 4:
                converge_4[expId] = value;
                break;
            case 5:
                converge_5[expId] = value;
                break;
            default:
                converge_3[expId] = value;
                break;
        }

        System.out.println("type:" + type + " set converge = " + value);
    }
}
