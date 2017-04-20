package tju.scs.hxt.coordination.agent;

import tju.scs.hxt.coordination.web.GlobalCache;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by haoxiaotian on 2017/4/17 20:33.
 */
public class TrainingThread extends Thread {

    private int type;

    private CountDownLatch endGate;

    public TrainingThread(int type) {
        super();
        this.type = type;
    }

    @Override
    public void run() {
        try {
            super.run();
//            System.out.println("thread run");

            while (!GlobalCache.isConverge(type, false)) {
//                System.out.println("not converge");
                runNextRound();
            }
        }finally {
            endGate.countDown();
        }
    }


    private void runNextRound(){
        // 1：每一个agent都向其邻居发一个请求
        sendRequest();

        // 2: 每个agent，收集请求，投票选择采取的action
        training();
    }

    private void sendRequest(){
        for(Agent agent: GlobalCache.getAgents(type)){
            agent.sendConnectionRequestToNeighbors(false);
        }
    }

    private void training(){
        for(Agent agent: GlobalCache.getAgents(type)){
            agent.randomTraining();
        }
    }


    public void setEndGate(CountDownLatch endGate) {
        this.endGate = endGate;
    }
}
