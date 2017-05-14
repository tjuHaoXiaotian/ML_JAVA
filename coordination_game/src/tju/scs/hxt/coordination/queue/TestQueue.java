package tju.scs.hxt.coordination.queue;

import tju.scs.hxt.coordination.agent.Agent;
import tju.scs.hxt.coordination.agent.Request;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

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
            boundedPriorityBlockingQueue.add(new Request(new Agent(1,2,3,4,0),new Agent(2,2,3,4,0),1,1,1,1,i,null));
        }

        Request ele;
        while ((ele = boundedPriorityBlockingQueue.poll()) != null){
            System.out.println(ele);
        }


        PriorityBlockingQueue<Integer> votes = new PriorityBlockingQueue<>(10, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2 > 0 ? -1: 1;
            }
        });
        votes.add(6);
        votes.add(7);
        votes.add(8);
        votes.add(3);
        votes.add(4);
        votes.add(5);

        Integer element;
        while ((element = votes.poll()) != null){
            System.out.println(element);
        }
    }
}
