package tju.scs.hxt.coordination.symmetric.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import tju.scs.hxt.coordination.symmetric.Config;
import tju.scs.hxt.coordination.symmetric.agent.Agent;
import tju.scs.hxt.coordination.symmetric.agent.StopThread;
import tju.scs.hxt.coordination.symmetric.agent.TrainingThread;
import tju.scs.hxt.coordination.symmetric.q.QItem;
import tju.scs.hxt.coordination.symmetric.web.GlobalCache;
import tju.scs.hxt.coordination.symmetric.web.entity.Network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @RequestMapping(value = "/clear/{type}",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    Map resetNetwork(@PathVariable("type") int type){
        GlobalCache.setConverge(type,true);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        GlobalCache.clearAgents(type);
        Map<String,Integer> result = new HashMap<>();
        result.put("status",200);
        return result;
    }

    @RequestMapping(value = "/{type}/stop",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    Map isStop(@PathVariable("type") int type){
        Map<String,Boolean> result = new HashMap<>();
        result.put("status",GlobalCache.isConverge(type,0) && GlobalCache.isConverge(type,1) && GlobalCache.isConverge(type,2));
        return result;
    }


    @RequestMapping(value = "/{type}/agents/{id}",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    QItem[] getQValues(@PathVariable("type") int type,@PathVariable("id") int id,@RequestParam("expId") int expId){
        return getAgentById(GlobalCache.getAgents(type,expId),id).getQValues();
    }

    @RequestMapping(value = "/{type}/avgPayoffs",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    List<ArrayList<GlobalCache.AvgReward>> getAvgPayoffs(@PathVariable("type") int type){
//   ArrayList<GlobalCache.AvgReward> getAvgPayoffs(@PathVariable("type") int type){
//        return GlobalCache.getAvgReward(type).get(0);
        return GlobalCache.getAvgReward(type);
    }

    private Agent getAgentById(List<Agent> agents,int id){
        for(Agent agent:agents){
            if(agent.getId() == id){
                return agent;
            }
        }
        return null;
    }

    @RequestMapping(value = "/{type}/agents/rowActions",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    Map<Integer,Integer> getActions(@PathVariable("type") int type,@RequestParam("expId") int expId){
        Map<Integer,Integer> actionSelection = new HashMap<Integer, Integer>();
        int bestAction;
        if(GlobalCache.getAgents(type,expId) == null){
            return actionSelection;
        }
        for(Agent agent:GlobalCache.getAgents(type,expId)){
            bestAction = agent.getMaxUtilityAction();
            actionSelection.put(bestAction,actionSelection.get(bestAction) == null?1:actionSelection.get(bestAction)+1);
        }
        return actionSelection;
    }



    private Network generateNetwork(int type){
        synchronized (GlobalCache.getLock(type)){
            if(GlobalCache.getAgents(type) == null){
                switch (type){
                    case 0:  // 网格结构
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.symmetric.network.Network.generateGridNetworkAsList(10, 10,0),0);
                        break;
                    case 1:  // regular
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.symmetric.network.Network.generateRegularGraph(100, 10,1),1);
                        break;
                    case 2:  // random regular
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.symmetric.network.Network.generateRandomRegularGraph(100, 10,2),2);
                        break;
                    case 3:  // random
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.symmetric.network.Network.generateRandomGraph(100, 0.6,3),3);
                        break;
                    case 4:  // small world
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.symmetric.network.Network.generateSmallWorldGraph(100, 15, 0.6,4),4);
                        break;
                    case 5:  // scale free
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.symmetric.network.Network.generateScaleFreeGraph(100, 1,5),5);
                        break;
                    default: // small world
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.symmetric.network.Network.generateSmallWorldGraph(100, 3, 0.6,4),4);
                        break;
                }
            }
        }

//        runAgents(type);

        runAgentsSingleThread(type);

        return GlobalCache.getNetworks(type);
    }

    private void runAgentsSingleThread(final int type) {
        synchronized (GlobalCache.getLock(type)) {
            if (!GlobalCache.isRunningState(type)) {
                for(int i = 0; i <Config.contrast_experiment;i++){
                    final int expId = i;
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            final CountDownLatch endGate = new CountDownLatch(1);

                            // 开始训练线程
                            TrainingThread trainingThread = new TrainingThread(type,expId);

                            trainingThread.setEndGate(endGate);

                            trainingThread.start();

                            long startTime = System.currentTimeMillis();

                            // 开始轮训线程：以判断整个agent网络是否收敛
                            Thread stopThread = new StopThread(type,expId);
                            stopThread.start();

                            try {
                                endGate.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            long endTime = System.currentTimeMillis();

                            System.out.println("网络："+ type +" 运行了 "+(((double)(endTime - startTime)) / 1000)+" s");
                        }
                    });
                    thread.start();
                }



                GlobalCache.setRunningState(type,true);
            }
        }

        for(Agent agent:GlobalCache.getAgents(type,0)){
            agent.printQTable();
        }

        Config.printRewardTable();
    }

}
