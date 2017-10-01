package tju.scs.hxt.coordination.symmetric.web;

import tju.scs.hxt.coordination.symmetric.agent.Agent;
import tju.scs.hxt.coordination.symmetric.network.Node;
import tju.scs.hxt.coordination.symmetric.web.entity.Network;

import java.util.List;

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
    private final static Object lock1 = new Object();
    private final static Object lock2 = new Object();
    private final static Object lock3 = new Object();
    private final static Object lock4 = new Object();
    private final static Object lock5 = new Object();

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

    public static void createGlobalCache(List<Agent> parameter,int type){
            synchronized (getLock(type)){
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
     * @return
     */
    public static boolean isConverge(int type){  // 判断是否收敛于同一个action
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

    /**
     * 对应网络结构下：设置其状态为收敛
     * @param type
     */
    public static void setConverge(int type){
        switch (type){
            case 0:
                converge_0 = true;
                break;
            case 1:
                converge_1 = true;
                break;
            case 2:
                converge_2 = true;
                break;
            case 3:
                converge_3 = true;
                break;
            case 4:
                converge_4 = true;
                break;
            case 5:
                converge_5 = true;
                break;
            default:
                converge_4 = true;
                break;
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
