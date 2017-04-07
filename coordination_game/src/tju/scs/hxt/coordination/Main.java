package tju.scs.hxt.coordination;

import tju.scs.hxt.coordination.agent.Agent;
import tju.scs.hxt.coordination.network.Network;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by haoxiaotian on 2017/3/14 1:10.
 */
public class Main {

    public static void main(String [] args) throws InterruptedException {

        /**
         * 网格模型
         */
//        Agent[][] agents = Network.generateGridNetwork(4, 4);
//
//        final CountDownLatch startGate = new CountDownLatch(1);
//        final CountDownLatch endGate = new CountDownLatch(16);
//        Thread thread = null;
//        for(Agent[] array:agents){
//            for(Agent agent:array){
//                agent.setEndGate(endGate);
//                agent.setStartGate(startGate);
//                thread = new Thread(agent);
//                thread.start();
//            }
//        }
//        for(Agent[] array:agents){
//            for(Agent agent:array){
//                agent.printQTable();
//            }
//        }
        /**
         * 其他网络
         */
        int agentSize = 30;
        List<Agent> agents = Network.generateSmallWorldGraph(agentSize,2,0.6,4);
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(agentSize);
        Thread thread = null;
        for(Agent agent:agents){
            agent.setEndGate(endGate);
            agent.setStartGate(startGate);
            thread = new Thread(agent);
            thread.start();
        }

        long startTime = System.currentTimeMillis();
        // 打开起始门，放行所有线程
        startGate.countDown();
        endGate.await();

        long endTime = System.currentTimeMillis();

        System.out.println("运行了 "+(((double)(endTime - startTime)) / 1000)+" s");

        for(Agent agent:agents){
            agent.printQTable();
        }

        Config.printRewardTable();
    }
}
