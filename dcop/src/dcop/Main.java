package dcop;

import dcop.agent.StopThread;
import dcop.agent.TrainingThread;
import dcop.network.Network;

import java.util.concurrent.CountDownLatch;

/**
 * Created by haoxiaotian on 2017/11/9 10:52.
 */
public class Main {

    public static void main(String [] args){
        generateNetwork(5);
    }

    private static void generateNetwork(int type){
        synchronized (GlobalCache.getLock(type)){
            if(GlobalCache.getAgents(type) == null){
                switch (type){
                    case 0:  // 网格结构
                        GlobalCache.createGlobalCache(Network.generateGridNetworkAsList(10, 10,0),0);
                        break;
                    case 1:  // regular
                        GlobalCache.createGlobalCache(Network.generateRegularGraph(100, 5,1),1);
                        break;
                    case 2:  // random regular
                        GlobalCache.createGlobalCache(Network.generateRandomRegularGraph(100, 5,2),2);
                        break;
                    case 3:  // random
                        GlobalCache.createGlobalCache(Network.generateRandomGraph(100, 0.03,3),3);
                        break;
                    case 4:  // small world
                        GlobalCache.createGlobalCache(Network.generateSmallWorldGraph(100, 5, 0.6,4),4);
                        break;
                    case 5:  // scale free
                        GlobalCache.createGlobalCache(Network.generateScaleFreeGraph(100, 1,5),5);
                        break;
                    default: // small world
                        GlobalCache.createGlobalCache(Network.generateSmallWorldGraph(100, 3, 0.6,4),4);
                        break;
                }
            }
        }

        runAgentsSingleThread(type);
    }

    private static void runAgentsSingleThread(final int type) {
        synchronized (GlobalCache.getLock(type)) {
            if (!GlobalCache.isRunningState(type)) {
                for(int i = 0; i <Config.contrast_experiment;i++){
//                    if(i == 0){
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
//                }


                GlobalCache.setRunningState(type,true);
            }
        }

        Config.printRewardTable();
    }
}
