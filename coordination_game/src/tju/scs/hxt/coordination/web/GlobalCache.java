package tju.scs.hxt.coordination.web;

import tju.scs.hxt.coordination.agent.Agent;
import tju.scs.hxt.coordination.web.entity.Network;
import tju.scs.hxt.coordination.network.Node;

import java.util.List;

/**
 * Created by haoxiaotian on 2017/3/18 23:18.
 */
public class GlobalCache {

    private static List<Agent>[] agents = new List[6];

    private static Network[] networks = new Network[6];

    private static boolean[] runningState = new boolean[6];

    private final static Object lock = new Object();

    public static Object getLock(){
        return lock;
    }

    public static void createGlobalCache(List<Agent> parameter,int type){
        if(agents[type] == null){
            synchronized (lock){
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

    public static boolean isConverge(int type){
        for(Agent agent:agents[type]){
            if(!agent.getEnoughTraining()){
                return false;
            }
        }

        return true;
    }
}
