package tju.scs.hxt.coordination.dcop.agent.max_plus;

import org.omg.PortableInterceptor.INACTIVE;
import tju.scs.hxt.coordination.dcop.Config;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by haoxiaotian on 2017/5/14 21:09.
 */
public class ObservedPolicy {

    private static class ObservationQueue{
        private LinkedList<Integer> queue;
        private int size;
        public ObservationQueue(int size){
            this.size = size;
            this.queue = new LinkedList<Integer>();
        }

        public boolean isFull(){
            return this.queue.size() == size;
        }

        public Integer pop(){
            return this.queue.removeFirst();
        }

        public void add(Integer item){
            this.queue.add(item);
        }
    }

    private int agentId;

    private int [][] actionSelectedTimes; // actionSelectedTimes[i][j] action:i is me，action:j is partner

    private List<ObservationQueue> observationQueues;

    private int actionNum;

    public ObservedPolicy(int agentId,int actionNum){
        this.agentId = agentId;
        this.actionNum = actionNum;
        this.initActionSelectedTimes(actionNum);
        this.initObservationQueues(actionNum);
    }

    private void initActionSelectedTimes(int actionNum){
        actionSelectedTimes = new int[actionNum][actionNum+1];  // 多一列，统计总的次数
        for(int i = 0; i < actionNum;i++){
            for(int j = 0; j < actionNum;j++){
                actionSelectedTimes[i][j] = 0;
            }
        }
    }

    private void initObservationQueues(int actionNum){
        this.observationQueues = new ArrayList<ObservationQueue>(actionNum);
        for(int i = 0; i < actionNum;i++){
            this.observationQueues.add(new ObservationQueue(Config.observationWindow));
        }
    }

    public double getProbability(int actionMe,int actionTarget){
        if(!recordedEnough(actionMe)){  // 还未统计过消息或统计的次数不够
            return 1.0 / actionNum;
        }else{
            return actionSelectedTimes[actionMe][actionTarget] * 1.0 / actionSelectedTimes[actionMe][actionNum];  // 当前次数 / 总次数
        }
    }

    private boolean recordedEnough(int actionMe){
        return (actionSelectedTimes[actionMe][actionNum] != 0 && actionSelectedTimes[actionMe][actionNum] >= Config.recordedTimesFloor);
    }

    public int getAgentId() {
        return agentId;
    }

    public void selectActions(int actionMe,int actionTarget){
        if(!observationQueues.get(actionMe).isFull()){
            // 1：对应次数
            actionSelectedTimes[actionMe][actionTarget] += 1;
            // 2：总次数
            actionSelectedTimes[actionMe][actionNum] += 1;
            // 3：入队列(对方选了 actionTarget)
            observationQueues.get(actionMe).add(actionTarget);
        }else{
            // 1：对应次数
            actionSelectedTimes[actionMe][actionTarget] += 1;
            // 2：remove the oldest
            int actionOld = observationQueues.get(actionMe).pop();
            actionSelectedTimes[actionMe][actionOld] -= 1;
            // 3：入队列(对方选了 actionTarget)
            observationQueues.get(actionMe).add(actionTarget);
        }

    }
}
