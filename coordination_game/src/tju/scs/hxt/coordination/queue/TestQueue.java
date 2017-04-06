package tju.scs.hxt.coordination.queue;

import tju.scs.hxt.coordination.agent.Agent;
import tju.scs.hxt.coordination.agent.Request;

import java.util.Comparator;

/**
 * Created by haoxiaotian on 2017/3/17 17:16.
 */
public class TestQueue {

    public static void main(String [] args){
        BoundedPriorityBlockingQueue<Request> boundedPriorityBlockingQueue = new BoundedPriorityBlockingQueue<Request>(3
        , new Comparator<Request>() {
            @Override
            public int compare(Request o1, Request o2) {
                return (int)(o1.getPriority() - o2.getPriority());
            }
        },true);

//        boundedPriorityBlockingQueue.add(3);
//        boundedPriorityBlockingQueue.add(3);
//        boundedPriorityBlockingQueue.add(3);

        for(int i = 1; i < 10;i ++){
            boundedPriorityBlockingQueue.add(new Request(new Agent(1,2,3,4,2),new Agent(2,2,3,4,2),1,1,1,1,i));
        }

        Request ele;
        while ((ele = boundedPriorityBlockingQueue.poll()) != null){
            System.out.println(ele);
        }
    }
}
