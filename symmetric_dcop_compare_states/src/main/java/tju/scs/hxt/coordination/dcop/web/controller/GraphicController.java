package tju.scs.hxt.coordination.dcop.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import tju.scs.hxt.coordination.dcop.Analyze;
import tju.scs.hxt.coordination.dcop.Config;
import tju.scs.hxt.coordination.dcop.agent.Agent;
import tju.scs.hxt.coordination.dcop.agent.StopThread;
import tju.scs.hxt.coordination.dcop.agent.TrainingThread;
import tju.scs.hxt.coordination.dcop.web.GlobalCache;
import tju.scs.hxt.coordination.dcop.web.entity.Network;

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
            // 让所有线程自动退出
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(Thread.activeCount());

        GlobalCache.clearAgents(type);

        Map<String,Integer> result = new HashMap<>();
        result.put("status",200);
        return result;
    }

    @RequestMapping(value = "/{type}/stop",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    Map isStop(@PathVariable("type") int type){
        Map<String,Boolean> result = new HashMap<>();
        boolean stop = true;
        for(int i = 0; i < Config.contrast_experiment;i++){
            stop = GlobalCache.isConverge(type,i);
            if(!stop){
                result.put("status",false);
                return result;
            }
        }
        result.put("status", true);
        return result;
    }

    @RequestMapping(value = "/{type}/stopRun",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    Map setStop(@PathVariable("type") int type){
        Map<String,Boolean> result = new HashMap<>();
        GlobalCache.setConverge(type,true);
        result.put("status", true);
        return result;
    }

    @RequestMapping(value = "/{type}/avgPayoffs",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    List<ArrayList<GlobalCache.AvgReward>> getAvgPayoffs(@PathVariable("type") int type){
//   ArrayList<GlobalCache.AvgReward> getAvgPayoffs(@PathVariable("type") int type){
//        return GlobalCache.getAvgReward(type).get(0);
        return GlobalCache.getAvgReward(type);
    }

    @RequestMapping(value = "/{type}/communications",produces = {"application/json;charset=utf8"})
    public @ResponseBody
    Map<Integer,Integer> getCommunicationTimes(@PathVariable("type") int type){
        return Analyze.getCommunications(type);
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
    Map<Integer,Map<Integer,Integer>> getActions(@PathVariable("type") int type){
        Map<Integer,Map<Integer,Integer>> result = new HashMap<Integer, Map<Integer,Integer>>();

        int bestAction;
        for(int expId = 0; expId < Config.contrast_experiment;expId++){
            Map<Integer,Integer> actionSelection = new HashMap<Integer, Integer>();
            if(GlobalCache.getAgents(type,expId) == null){
                result.put(expId,new HashMap<Integer, Integer>());
            }else{
                for(Agent agent:GlobalCache.getAgents(type,expId)){
                    bestAction = agent.getMaxUtilityAction().getAction();
                    actionSelection.put(bestAction,actionSelection.get(bestAction) == null?1:actionSelection.get(bestAction)+1);
                }
                result.put(expId,actionSelection);
            }
        }
        return result;
    }



    private Network generateNetwork(int type){
        synchronized (GlobalCache.getLock(type)){
            if(GlobalCache.getAgents(type) == null){
                switch (type){
                    case 0:  // 网格结构
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateGridNetworkAsList(10, 10,0),0);
//                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateGridNetworkAsList(2, 2,0),0);
                        break;
                    case 1:  // regular
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateRegularGraph(100, 5,1),1);
//                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateRegularGraph(10, 5,1),1);
                        break;
                    case 2:  // random regular
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateRandomRegularGraph(100, 5,2),2);
//                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateRandomRegularGraph(100, 5,2),2);
                        break;
                    case 3:  // random
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateRandomGraph(100, 0.08,3),3);
//                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateRandomGraph(25, 0.3,3),3);
                        break;
                    case 4:  // small world
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateSmallWorldGraph(100, 5, 0.6,4),4);
//                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateSmallWorldGraph(35, 4, 0.6,4),4);
                        break;
                    case 5:  // scale free
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateScaleFreeGraph(100, 1,5),5);
//                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateScaleFreeGraph(35, 1,5),5);
                        break;
                    default: // small world
                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateSmallWorldGraph(100, 3, 0.6,4),4);
//                        GlobalCache.createGlobalCache(tju.scs.hxt.coordination.dcop.network.Network.generateSmallWorldGraph(100, 3, 0.6,4),4);
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
//                    if(i == 3){
                        System.out.println("new thread for type"+type+":"+i+" restart");
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
                                Thread stopThread = new StopThread(type,expId,trainingThread);
                                stopThread.start();

                                try {
                                    endGate.await();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                long endTime = System.currentTimeMillis();

                                System.out.println("网络：["+ type + "," + expId + "] 运行了 "+(((double)(endTime - startTime)) / 1000)+" s");
                            }
                        });
                        thread.start();
                    }
                }


                GlobalCache.setRunningState(type,true);
            }
//        }

        Config.printRewardTable();
    }

}
