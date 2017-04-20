package tju.scs.hxt.coordination.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import tju.scs.hxt.coordination.Config;
import tju.scs.hxt.coordination.agent.Agent;
import tju.scs.hxt.coordination.agent.StopThread;
import tju.scs.hxt.coordination.agent.TrainingThread;
import tju.scs.hxt.coordination.web.entity.Network;
import tju.scs.hxt.coordination.q.QItem;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by haoxiaotian on 2017/3/16 10:38.
 */

@Controller
@RequestMapping("graph")
public class GraphicController {

    @RequestMapping(value = "/{type}",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    Network getTrainingNetwork(@PathVariable("type") int type){
        return generateNetwork(type);
    }


    @RequestMapping(value = "/{type}/agents/{id}",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    QItem[][] getQValues(@PathVariable("type") int type,@PathVariable("id") int id){
        return GlobalCache.getAgents(type).get(id).getqValues();
    }

    @RequestMapping(value = "/{type}/agents/rowActions",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    Map<Integer,Integer> getActions(@PathVariable("type") int type){
        Map<Integer,Integer> rowActionSelection = new HashMap<Integer, Integer>();
        int bestRow;
        for(Agent agent:GlobalCache.getAgents(type)){
            bestRow = agent.getBestRowAction();
            rowActionSelection.put(bestRow,rowActionSelection.get(bestRow) == null?1:rowActionSelection.get(bestRow)+1);
        }
        return rowActionSelection;
    }



    private Network generateNetwork(int type){
        synchronized (GlobalCache.getLock(type)){
            if(GlobalCache.getAgents(type) == null){
                switch (type){
                    case 0:  // 网格结构
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.network.Network.generateGridNetworkAsList(10, 10,0),0);
                        break;
                    case 1:  // regular
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.network.Network.generateRegularGraph(100, 5,1),1);
                        break;
                    case 2:  // random regular
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.network.Network.generateRandomRegularGraph(100, 5,2),2);
                        break;
                    case 3:  // random
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.network.Network.generateRandomGraph(100, 0.2,3),3);
                        break;
                    case 4:  // small world
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.network.Network.generateSmallWorldGraph(100, 4, 0.6,4),4);
                        break;
                    case 5:  // scale free
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.network.Network.generateScaleFreeGraph(100, 1,5),5);
                        break;
                    default: // small world
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.network.Network.generateSmallWorldGraph(100, 3, 0.6,4),4);
                        break;
                }
            }
        }

//        runAgents(type);

        runAgentsSingleThread(type);

        return GlobalCache.getNetworks(type);
    }

    private void runAgents(final int type) {
        synchronized (GlobalCache.getLock(type)){
            if(!GlobalCache.isRunningState(type)){
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final CountDownLatch startGate = new CountDownLatch(1);
                        final CountDownLatch endGate = new CountDownLatch(GlobalCache.getAgents(type).size());
                        Thread thread = null;
                        for(Agent agent:GlobalCache.getAgents(type)){
                            agent.setEndGate(endGate);
                            agent.setStartGate(startGate);
                            thread = new Thread(agent);
                            thread.start();
                        }

                        long startTime = System.currentTimeMillis();
                        // 打开起始门，放行所有线程
                        startGate.countDown();

                        // 开始轮训线程：以判断整个agent网络是否收敛
                        Thread stopThread = new StopThread(type);
                        stopThread.start();

                        try {
                            endGate.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        long endTime = System.currentTimeMillis();

                        System.out.println("网络："+ type +" 运行了 "+(((double)(endTime - startTime)) / 1000)+" s");
                        System.out.println("网络："+ type +" 平均 connection 次数 "+ GlobalCache.calMeanTrainingTimes(type));
                    }
                });
                thread.start();

                GlobalCache.setRunningState(true,type);
            }
        }


        for(Agent agent:GlobalCache.getAgents(type)){
            agent.printQTable();
        }

        Config.printRewardTable();
    }

    private void runAgentsSingleThread(final int type) {
        synchronized (GlobalCache.getLock(type)) {
            if (!GlobalCache.isRunningState(type)) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        final CountDownLatch endGate = new CountDownLatch(1);

                        // 开始训练线程
                        TrainingThread trainingThread = new TrainingThread(type);

                        trainingThread.setEndGate(endGate);

                        trainingThread.start();

                        long startTime = System.currentTimeMillis();

                        // 开始轮训线程：以判断整个agent网络是否收敛
                        Thread stopThread = new StopThread(type);
                        stopThread.start();

                        try {
                            endGate.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        long endTime = System.currentTimeMillis();

                        System.out.println("网络："+ type +" 运行了 "+(((double)(endTime - startTime)) / 1000)+" s");
                        System.out.println("网络："+ type +" 平均 connection 次数 "+ GlobalCache.calMeanTrainingTimes(type));
                    }
                });
                thread.start();


                GlobalCache.setRunningState(true, type);
            }
        }

        for(Agent agent:GlobalCache.getAgents(type)){
            agent.printQTable();
        }

        Config.printRewardTable();
    }

}
