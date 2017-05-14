package tju.scs.hxt.coordination.dcop.agent;

import java.util.*;

/**
 * Created by haoxiaotian on 2017/5/12 12:25.
 */
public class Message {
    private Agent fromAgent;

    private Agent toAgent;

    private double [][] qFromTO;

    public Message(Agent fromAgent,Agent toAgent,double [][] qFromTO){
        this.fromAgent = fromAgent;
        this.toAgent = toAgent;
        this.qFromTO = copyOf(qFromTO);

        initCandidates();
    }

    /**
     * 消息主函数
     * @param maxToAction
     * @return
     */
    public double getMax(int maxToAction){
        // 减去一个平均值参数，防止有环图无限增大
//        System.out.println("max :"+maxCandidates.get(maxToAction));
//        System.out.println("avg :"+avgOfCandidate());
        return maxCandidates.get(maxToAction) - avgOfCandidate();
    }

    private double[][] copyOf(double[][] qFromTO) {
        double [][] qCopy = new double[qFromTO.length][];
        for(int i = 0; i < qFromTO.length;i++){
//            System.out.println(Arrays.toString(qFromTO[i]));
            qCopy[i] = Arrays.copyOf(qFromTO[i],qFromTO[i].length);
        }
        return qCopy;
    }

    /**
     * 返回最大的累积reward
     * @param maxToAction
     * @return
     */
    private Map<Integer,Double> maxCandidates = new HashMap<Integer, Double>();
    private void initCandidates(){
        // TODO：递归太深
        // 对没一个 toAction；都提前计算一下最大的reward
        int maxActionMe = 0;
        double maxValue = 0,temp = 0;
        for(int toAction = 0; toAction < toAgent.getActionNum();toAction++) {
            maxValue = -10000;
            for (maxActionMe = 0; maxActionMe < fromAgent.getActionNum(); maxActionMe++) {  // 对于我的没一个 action，都有可能成为将来最大的
                temp = qFromTO[maxActionMe][toAction] + sumOfMessages(maxActionMe, fromAgent.getMessages());
                if (temp > maxValue) {
                    maxValue = temp;
                }
            }
            maxCandidates.put(toAction, maxValue);
        }
    }


    private double avgOfCandidate(){
        double avg = 0;
        for(Double value:maxCandidates.values()){
            avg += value;
        }
        return avg / maxCandidates.size();
    }


    /**
     * 计算我每一个action 对应的 入边 message
     * @param actionMe
     * @param messagesToMe
     * @return
     */
    private double sumOfMessages(int actionMe, Map<Integer, Message> messagesToMe){
        double sum = 0;
        for(Map.Entry<Integer,Message> entry:messagesToMe.entrySet()){   // 开始 message 可能为空，返回 0
            if(entry.getKey() != toAgent.getId()){
                sum += entry.getValue().getMax(actionMe);
            }
        }
        return sum;
    }
}
