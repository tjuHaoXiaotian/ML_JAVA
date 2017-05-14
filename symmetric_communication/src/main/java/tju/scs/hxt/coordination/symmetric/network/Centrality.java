package tju.scs.hxt.coordination.symmetric.network;

import tju.scs.hxt.coordination.symmetric.agent.Agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haoxiaotian on 2017/5/8 10:40.
 */
public class Centrality {

    public static void setCentrality(List<Agent> agents){
        // 1：degree centrality
        setDegreeCentrality(agents);
//
        // 2:Closeness Centrality
//        setClosenessCentrality(agents);
    }

    /**
     * default: degree centrality (1)
     * @param agents
     */
    public static void setDegreeCentrality(List<Agent> agents){
        for(Agent agent:agents){
            agent.setCentrality(agent.getNeighborsSize());
        }
    }


    /**
     * Closeness Centrality
     * @param agents
     */
    public static void setClosenessCentrality(List<Agent> agents){
        // 1：计算出所有点之间的最短路径
        int [][] shortestPath = ShortestPath.dijkstra(agents);

        // 2：计算 Closeness centrality
        double centrality = 0;
        for(Agent n1:agents){
            for(Agent n2:agents){
                if(!n1.equals(n2)){
                    centrality += 1.0 / shortestPath[n1.getId()][n2.getId()];
                }
            }
            n1.setCentrality(centrality);
            centrality = 0;
        }

        // 3：标准化 centrality
        normalize(agents);
    }

    /**
     * 标准化 centrality (each / sum(each))
     * @param agents
     */
    private static void normalize(List<Agent> agents){
        double total = 0;
        for(Agent agent:agents){
            total+=agent.getCentrality();
        }

        for(Agent agent:agents){
            agent.setCentrality(agent.getCentrality() / total);
        }
    }
}
