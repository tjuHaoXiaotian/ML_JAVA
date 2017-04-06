package tju.scs.hxt.coordination.network;

import tju.scs.hxt.coordination.Config;
import tju.scs.hxt.coordination.agent.Agent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 生成几种常见网络拓扑结构
 *
 * 1：网格网络
 * 2：Regular Graph
 * 3：Random Regular Graph
 * 4：Random Graph
 * 5：Small World Graph
 * 6：Scale Free Graph
 *
 * Created by haoxiaotian on 2017/3/15 14:56,finished at 2017/3/15 22:30.
 */
public class Network {


    /**
     * 网格结构网络
     * @param rowNum
     * @param colNum
     * @return
     */
    public static List<Agent> generateGridNetworkAsList(int rowNum,int colNum){
        Agent[][] parameter = generateGridNetwork(rowNum,colNum);
        List<Agent> agents = new ArrayList<Agent>(rowNum * colNum);
        for(int i = 0; i < parameter.length;i++){
            for(int j = 0; j < parameter[i].length;j++){
                agents.add(parameter[i][j]);
            }
        }

        return agents;
    }

    /**
     * 网格结构网络
     * @param rowNum 网格行数
     * @param colNum 网格列数
     * @return
     */
    public static Agent[][] generateGridNetwork(int rowNum,int colNum){
        Agent[][] nodes = new Agent[rowNum][colNum];
        for(int i = 0; i < rowNum;i++){
            for(int j = 0; j < colNum; j++){
                nodes[i][j] = new Agent(i * colNum + j,Config.actionNum,Config.exploreRate,Config.learningRate,rowNum*colNum);
            }
        }

        for(int i = 0; i < rowNum;i++){
            for(int j = 0;j < colNum;j++){
                // 四个边
                if(i == 0 || j == 0 || i == rowNum-1 || j == colNum-1){
                    if(i == 0){
                        nodes[i][j].addNeighbor(nodes[i + 1][j]); // 下
                        if(j != 0){
                            nodes[i][j].addNeighbor(nodes[i][j - 1]);  // 左
                        }
                        if(j != colNum-1){
                            nodes[i][j].addNeighbor(nodes[i][j + 1]);  // 右
                        }
                    }else if(i == rowNum - 1){
                        nodes[i][j].addNeighbor(nodes[i - 1][j]); // 上
                        if(j != 0){
                            nodes[i][j].addNeighbor(nodes[i][j-1]);  // 左
                        }
                        if(j != colNum-1){
                            nodes[i][j].addNeighbor(nodes[i][j+1]);  // 右
                        }
                    }else if(j == 0){
                        nodes[i][j].addNeighbor(nodes[i-1][j]);  // 上
                        nodes[i][j].addNeighbor(nodes[i+1][j]);  // 下
                        nodes[i][j].addNeighbor(nodes[i][j+1]);  // 右
                    }else if(j == colNum - 1){
                        nodes[i][j].addNeighbor(nodes[i-1][j]);  // 上
                        nodes[i][j].addNeighbor(nodes[i+1][j]);  // 下
                        nodes[i][j].addNeighbor(nodes[i][j-1]);  // 左
                    }

                }else{  // 中间
                    nodes[i][j].addNeighbor(nodes[i-1][j]);  // 上
                    nodes[i][j].addNeighbor(nodes[i+1][j]);  // 下
                    nodes[i][j].addNeighbor(nodes[i][j-1]);  // 左
                    nodes[i][j].addNeighbor(nodes[i][j+1]);  // 右
                }
            }
        }
        return nodes;
    }

    /**
     * 先组成一个环，每个 node 与距离自己最近的 n（暂时定为偶数个） 个 node 相连接
     * @param nodesNum 网络节点数
     * @param neighborNum 规则网络的相邻邻居数 （此处最好为偶数）
     * @return
     */
    public static List<Agent> generateRegularGraph(int nodesNum,int neighborNum){
        return generateRegularGraph(nodesNum,neighborNum,-1);
    }


    /**
     * 先组成一个环，每个 node 与距离自己最近的 n（暂时定为偶数个） 个 node 相连接
     * @param nodesNum 网络节点数
     * @param neighborNum 规则网络的相邻邻居数 （此处最好为偶数）
     * @param totalNum 整体网络的结点个数，保证各个agent权值计算的正确性
     * @return
     */
    private static List<Agent> generateRegularGraph(int nodesNum,int neighborNum,int totalNum){
        neighborNum = neighborNum / 2;
        // 1：添加节点
        List<Agent> linkedAgents = new ArrayList<Agent>(nodesNum);
        for(int i = 0; i < nodesNum;i++){
            linkedAgents.add(new Agent(i,Config.actionNum,Config.exploreRate,Config.learningRate,totalNum > nodesNum?totalNum:nodesNum));
        }

        Agent me,neighbor;
        for(int i = 0; i < nodesNum;i++){
            for(int j = 1; j <= neighborNum;j++){
                neighbor = linkedAgents.get((i + j) % linkedAgents.size());
                me = linkedAgents.get(i);
                if(!me.hasNeighbor(neighbor)){
                    me.addNeighbor(neighbor);
                    neighbor.addNeighbor(me);
                }
            }
        }

        return linkedAgents;
    }

    /**
     * 生成随机规则网络结构
     * 共有 nodesNum 个节点，每个节点有 neighborNum 个边连接
     * @param nodesNum 网络节点数
     * @param neighborNum 规则网络的相邻邻居数
     * @return
     */
    public static List<Agent> generateRandomRegularGraph(int nodesNum,int neighborNum){

        // 1：生成 nodesNum 个网络节点
        List<Agent> groupWithoutLink = new LinkedList<Agent>();
        for(int i = 0; i < nodesNum;i++){
            groupWithoutLink.add(new Agent(i,Config.actionNum,Config.exploreRate,Config.learningRate,nodesNum));
        }

        List<Agent> linkedAgents = new ArrayList<Agent>(nodesNum);
        // 2:从旧的group中依次拿出一个node，与新group中节点连接 (进行必要的连接，以便整个网络可以拎起来)
        Agent node = null,targetAgent = null;
        while (!groupWithoutLink.isEmpty()){
            node = groupWithoutLink.remove(0);
            if(!linkedAgents.isEmpty()){
                // 随机选出一个 node
                targetAgent = linkedAgents.get(Config.getRandomNumber(0,linkedAgents.size()-1));
                while (targetAgent.getNeighborsSize() == neighborNum){
                    targetAgent = linkedAgents.get(Config.getRandomNumber(0,linkedAgents.size()-1));
                }
                targetAgent.addNeighbor(node);
                node.addNeighbor(targetAgent);
            }

            linkedAgents.add(node);
        }

        // 3：将其余的边随机连接起来
        List<Integer> excepts = null;
        for(Agent n:linkedAgents){
            System.out.println("loop1 ");

            excepts = new ArrayList<Integer>();
            while (n.getNeighborsSize() < neighborNum){
                System.out.print("loop2 ");
                // 随机选出一个 node
                targetAgent = linkedAgents.get(Config.getRandomNumber(0,linkedAgents.size()-1,excepts));
                // 当挑选出来的结点已经满了，或者两个节点之间已经连接过
                while (targetAgent.getNeighborsSize() == neighborNum || n.hasNeighbor(targetAgent) || n.equals(targetAgent)){
                    System.out.print("loop3 ");
                    excepts.add(targetAgent.getId());
                    targetAgent = linkedAgents.get(Config.getRandomNumber(0,linkedAgents.size()-1,excepts));
                }
                n.addNeighbor(targetAgent);
                targetAgent.addNeighbor(n);
            }
        }
        return linkedAgents;
    }

    /**
     * 生成随机网络,ER模型
     * 一个典型的模型是埃尔德什和雷尼共同研究的ER模型。ER模型是指在给定 n 个顶点后，规定每两个顶点之间都有 p 的概率连起来（ 0 ⩽ p ⩽ 1）
     * 而且这些判定之间两两无关。
     * @param nodesNum 网络节点数目
     * @return
     */
    public static List<Agent> generateRandomGraph(int nodesNum,double p){

        // 1：生成 nodesNum 个网络节点
        List<Agent> groupWithoutLink = new LinkedList<Agent>();
        for(int i = 0; i < nodesNum;i++){
            groupWithoutLink.add(new Agent(i,Config.actionNum,Config.exploreRate,Config.learningRate,nodesNum));
        }

        List<Agent> linkedAgents = new ArrayList<Agent>(nodesNum);
        // 2:从旧的group中依次拿出一个node，与新group中节点连接(进行必要的连接，以便整个网络可以拎起来)
        Agent node = null,targetAgent = null;
        while (!groupWithoutLink.isEmpty()){
            node = groupWithoutLink.remove(0);
            if(!linkedAgents.isEmpty()){
                // 随机选出一个 node
                targetAgent = linkedAgents.get(Config.getRandomNumber(0,linkedAgents.size()-1));
                targetAgent.addNeighbor(node);
                node.addNeighbor(targetAgent);
            }

            linkedAgents.add(node);
        }

        // 3：for 循环，两两之间以概率 P 连接
        for(int i = 0; i < linkedAgents.size();i++){
            for(int j = i + 1; j < linkedAgents.size();j++){
                if(Math.random() <= p){  // 在概率范围之内,选择连接
                    if(!linkedAgents.get(i).equals(linkedAgents.get(j)) && !linkedAgents.get(i).hasNeighbor(linkedAgents.get(j))){
                        linkedAgents.get(i).addNeighbor(linkedAgents.get(j));
                        linkedAgents.get(j).addNeighbor(linkedAgents.get(i));
                    }
                }
            }
        }

        return linkedAgents;
    }

    /**
     * small-world 网络；
     * 瓦茨-斯特罗加茨模型
     * WS模型是基于两人的一个假设：小世界模型是介于规则网络和随机网络之间的网络。因此模型从一个完全的规则网络出发，以一定的概率将网络中的连接打乱重连。
     * @param nodesNum 网络节点数
     * @param neighborNum 规则网络的相邻邻居数
     * @param p  以概率 p 重连
     * @return
     */
    public static List<Agent> generateSmallWorldGraph(int nodesNum,int neighborNum,double p){
        return generateSmallWorldGraph(nodesNum,neighborNum,p,-1);
    }

    /**
     * small-world 网络；
     * 瓦茨-斯特罗加茨模型
     * WS模型是基于两人的一个假设：小世界模型是介于规则网络和随机网络之间的网络。因此模型从一个完全的规则网络出发，以一定的概率将网络中的连接打乱重连。
     * @param nodesNum 网络节点数
     * @param neighborNum 规则网络的相邻邻居数
     * @param p  以概率 p 重连
     * @param totalNum 整体网络的结点个数，保证各个agent权值计算的正确性
     * @return
     */
    private static List<Agent> generateSmallWorldGraph(int nodesNum,int neighborNum,double p,int totalNum){
        // 1: 生成规则网络
        List<Agent> nodes = generateRegularGraph(nodesNum,neighborNum,totalNum);
        neighborNum = neighborNum / 2;

        Agent me,neighbor,relinkNeighbor;
        for(int i = 0; i < nodesNum;i++){
            for(int j = 1; j <= neighborNum;j++){
                if(Math.random() < p){  // 概率范围之内，需要重连
                    neighbor = nodes.get((i + j) % nodes.size());
                    me = nodes.get(i);
                    if(me.hasNeighbor(neighbor)){
                        me.removeNeighbor(neighbor);
                    }
                    if(neighbor.hasNeighbor(me)){
                        neighbor.removeNeighbor(me);
                    }

                    relinkNeighbor = nodes.get(Config.getRandomNumber(0,nodes.size()-1));
                    while (me.equals(relinkNeighbor) || neighbor.equals(relinkNeighbor) || me.hasNeighbor(relinkNeighbor)){
                        relinkNeighbor = nodes.get(Config.getRandomNumber(0,nodes.size()-1));
                    }
                    me.addNeighbor(relinkNeighbor);
                    relinkNeighbor.addNeighbor(me);
                }

            }
        }
        return nodes;
    }


    /**
     * BA模型
     * 增长模式：不少现实网络是不断扩大不断增长而来的，例如互联网中新网页的诞生，人际网络中新朋友的加入，新的论文的发表，航空网络中新机场的建造等等。
     * 优先连接模式：新的节点在加入时会倾向于与有更多连接的节点相连，例如新网页一般会有到知名的网络站点的连接，新加入社群的人会想与社群中的知名人士结识，
     *             新的论文倾向于引用已被广泛引用的著名文献，新机场会优先考虑建立与大机场之间的航线等等。\
     * @param nodesNum 网络总共节点数目
     * @param m 每增加一个节点，向原始网络中添加 m 条边
     * @return
     */
    public static List<Agent> generateScaleFreeGraph(int nodesNum,int m){
        int smallWordAgents = 4;
        if(nodesNum > 10){
            smallWordAgents = 10;
        }
        List<Agent> nodes = generateSmallWorldGraph(smallWordAgents,2,0.6,nodesNum);  // 以此参数生成 small word 网络
        int gapNum = nodesNum - smallWordAgents,gapLink = m,totalDegree = calTotalDegree(nodes);
        Agent newAgent = null,oldAgent = null;
        while (gapNum > 0){  // 依次添加节点
            newAgent = new Agent(smallWordAgents++,Config.actionNum,Config.exploreRate,Config.learningRate,nodesNum);
            gapLink = m;
            while (gapLink > 0){  // 为每个新节点，添加
                // 随机选择一个旧的节点
                oldAgent = nodes.get(Config.getRandomNumber(0,nodes.size()-1));
                if(!oldAgent.hasNeighbor(newAgent)){
                    if(Math.random() < ((double)oldAgent.getNeighborsSize()) / totalDegree){   // 在概率范围之内
                        newAgent.addNeighbor(oldAgent);
                        oldAgent.addNeighbor(newAgent);
                        gapLink--;
                    }
                }
            }
            nodes.add(newAgent);
            totalDegree += m * 2;
            gapNum--;
        }

        return nodes;
    }

    private static int calTotalDegree(List<Agent> nodes){
        int totalDegree = 0;
        for(Agent node:nodes){
            totalDegree += node.getNeighbors().size();
        }
        return totalDegree;
    }

    public static void main(String [] args){
//        Agent[][] nodes = generateGridNetwork(3,3);
//        for(int i = 0; i < 3;i++){
//            for(int j = 0; j < 3;j++){
//                System.out.println(nodes[i][j]);
//            }
//        }

        System.out.println(generateRandomRegularGraph(30,4));
//        System.out.println(generateRegularGraph(10,10));
//        System.out.println(generateRandomGraph(10, 0.2));
//        System.out.println(generateSmallWorldGraph(10,2, 0.6));
//        System.out.println(generateScaleFreeGraph(20,1));
        System.out.println("123".hashCode());
        String s = "123";
        System.out.println(s.hashCode());
    }

}