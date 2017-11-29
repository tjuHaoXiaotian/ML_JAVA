package dcop.network;

import dcop.agent.Agent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by haoxiaotian on 2017/5/8 15:57.
 */
public class ShortestPath {

    private static final int MAX_PATH = Integer.MAX_VALUE;

    public static int[][]  dijkstra(List<Agent> nodes){
        int [][] shortestPath = new int[nodes.size()][nodes.size()];
        for(Agent node:nodes){
            dijkstra(node,nodes,shortestPath);
        }
        return shortestPath;
    }

    private static void dijkstra(Agent node,List<Agent> nodes,int [][] shortestPath){
        List<Agent> known = new ArrayList<>();
        List<Agent> unknown = new ArrayList<>(nodes);
        // 保存 node 到其他所有点的最短路径
        int [] preNodes = new int[nodes.size()];  // preNodes [i]  = j，表示到节点 i 的最短路径中，前一个节点是 j
        for(int i = 0; i < preNodes.length;i++){
            preNodes[i] = node.getId();  //
        }
        unknown.remove(node);
        known.add(node);

        // 1：初始化最短路径
        for(Node n:nodes){
            shortestPath[node.getId()][n.getId()] = MAX_PATH;
        }
        shortestPath[node.getId()][node.getId()] = 0;  // 自己到自己是0
        for(Node neighbor:node.getNeighbors()){
            shortestPath[node.getId()][neighbor.getId()] = 1;
        }

        // 2:只要不是空
        while(!unknown.isEmpty()){
            // 2.1：找到最短路径节点
            int shortest = MAX_PATH;
            Agent shortestNode = unknown.get(0);
            for(Agent n:unknown){
                if(shortestPath[node.getId()][n.getId()] < shortest){
                    shortest = shortestPath[node.getId()][n.getId()];
                    shortestNode = n;
                }
            }
            // 找到了一个新的最小值的点，将其纳入已知点
            // 到 shortestNode 的最短路径已经找到
            known.add(shortestNode);  // 找到 node ——> shortestNode 的最短路径，shortestPath[node.getId()][shortestNode.getId()]
            unknown.remove(shortestNode);

            // 2.2：更新最短路径
            for(Node n:shortestNode.getNeighbors()){
                if(shortestPath[node.getId()][shortestNode.getId()] + 1 < shortestPath[node.getId()][n.getId()]){
                    shortestPath[node.getId()][n.getId()] = shortestPath[node.getId()][shortestNode.getId()] + 1;
                    preNodes[n.getId()] = shortestNode.getId();  // 到节点 n 的上一个节点更新为 shortestNode
                }
            }
        }

//        printPath(node,preNodes);
    }

    private static void printPath(Agent node,int [] preNodes){
        LinkedList<Integer> path = new LinkedList<>();
        int currentId;
        for(int i = 0; i < preNodes.length;i++){
            // 输出从 node 到 i 的最短路径信息
            path.clear();
            path.add(i);
            currentId = preNodes[i];
            while (currentId != node.getId()){
                path.add(currentId);
                currentId = preNodes[currentId];
            }
            path.add(currentId);
            while (!path.isEmpty()){
                System.out.print(path.pollLast());
                if(!path.isEmpty()){
                    System.out.print("——>");
                }
            }
            System.out.print("\n");
        }
    }

    private static void printShortestPath(int [][] paths){
        for(int i = 0; i < paths.length;i++){
            for(int j = 0;j < paths[i].length;j++){
                System.out.println(i + "——>" + j + ":" +paths[i][j]);
            }
        }
    }

    public static void main(String [] args){
        List<Agent> agents = Network.generateScaleFreeGraph(5,1,3);

        System.out.println(agents);
        dijkstra(agents);
//        printShortestPath(dijkstra(agents));
    }
}
