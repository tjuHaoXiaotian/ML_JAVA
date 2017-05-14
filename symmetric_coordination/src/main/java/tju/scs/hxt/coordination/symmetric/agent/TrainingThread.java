package tju.scs.hxt.coordination.symmetric.agent;

import tju.scs.hxt.coordination.symmetric.web.GlobalCache;

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

            while (!GlobalCache.isConverge(type)) {
//                System.out.println("not converge");
                runNextRound();
            }
        }finally {
            endGate.countDown();
        }
    }


    private void runNextRound(){
        training();
    }

    private void training(){
        for(Agent agent: GlobalCache.getAgents(type)){
            agent.training();
        }
    }


    public void setEndGate(CountDownLatch endGate) {
        this.endGate = endGate;
    }
}
