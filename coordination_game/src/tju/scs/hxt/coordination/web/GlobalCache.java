package tju.scs.hxt.coordination.web;

import tju.scs.hxt.coordination.agent.Agent;
import tju.scs.hxt.coordination.web.entity.Network;
import tju.scs.hxt.coordination.network.Node;

import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;

/**
 * Created by haoxiaotian on 2017/3/18 23:18.
 */
public class GlobalCache {

    private static volatile List<Agent>[] agents = new List[6];

    static {
        for(int i = 0; i < 6;i++){
            agents[i] = null;
        }
    }

    private static Network[] networks = new Network[6];

    private static boolean[] runningState = new boolean[6];

    private final static Object lock = new Object();

    public static Object getLock(){
        return lock;
    }

    public static void createGlobalCache(List<Agent> parameter,int type){
            synchronized (lock){
                if(agents[type] == null){
                    GlobalCache.agents[type] = parameter;
                    networks[type] = new Network();
                    for(Node node:agents[type]){
                        networks[type].addNode(node);
                    }
                }
            }
    }

    public static List<Agent> getAgents(int type) {
        return agents[type];
    }

    public static Network getNetworks(int type) {
        return networks[type];
    }

    public static boolean isRunningState(int type) {
        return runningState[type];
    }

    public static void setRunningState(boolean runningState,int type) {
        GlobalCache.runningState[type] = runningState;
    }


    private volatile static boolean converge_0 = false;
    private volatile static boolean converge_1 = false;
    private volatile static boolean converge_2 = false;
    private volatile static boolean converge_3 = false;
    private volatile static boolean converge_4 = false;
    private volatile static boolean converge_5 = false;

    /**
     * 判断当前网络结构下，agent 是否收敛
     * @param type 网络结构类型
     * @param trainingTimes 判断指标：自己设定的训练次数
     * @return
     */
    public static boolean isConverge(int type,boolean trainingTimes){
        if(trainingTimes){
            for(Agent agent:agents[type]){
                if(!agent.getEnoughTraining()){
                    return false;
                }
            }

            return true;
        }else{  // 判断是否收敛于同一个action
            switch (type){
                case 0:
                    return converge_0;
                case 1:
                    return converge_1;
                case 2:
                    return converge_2;
                case 3:
                    return converge_3;
                case 4:
                    return converge_4;
                case 5:
                    return converge_5;
                default:
                    return converge_4;
            }

        }
    }

    /**
     * 对应网络结构下：设置其状态为收敛
     * @param type
     */
    public static void setConverge(int type){
        switch (type){
            case 0:
                converge_0 = true;
            case 1:
                converge_1 = true;
            case 2:
                converge_2 = true;
            case 3:
                converge_3 = true;
            case 4:
                converge_4 = true;
            case 5:
                converge_5 = true;
            default:
                converge_4 = true;
        }
        System.out.println("type:" + type + " set converge = true.");
    }

    /**
     * 计算达到收敛时，平均训练轮数
     * @param type
     * @return
     */
    public static double calMeanTrainingTimes(int type){
        double totalTimes = 0;
        for(Agent agent:agents[type]){
            totalTimes += agent.getConnectionTimes();
        }
        return totalTimes / agents[type].size();
    }



}
