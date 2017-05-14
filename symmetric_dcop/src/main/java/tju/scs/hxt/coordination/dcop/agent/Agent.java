package tju.scs.hxt.coordination.dcop.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tju.scs.hxt.coordination.dcop.Analyze;
import tju.scs.hxt.coordination.dcop.Config;
import tju.scs.hxt.coordination.dcop.network.Node;
import tju.scs.hxt.coordination.dcop.web.GlobalCache;

import java.util.*;

/**
 * Created by haoxiaotian on 2017/3/13 23:53.
 */
public class Agent extends Node{
    // 定义game 比较规整，简单；2 player,n-action game;
    private final int actionNum; // 每个 state 下的 action num

    @JsonIgnore
    private Map<Integer,QTable> qTables = new HashMap<Integer, QTable>();

    // 传播的消息： Key ——> this
    @JsonIgnore
    private Map<Integer,Message> messages = new HashMap<Integer, Message>();

    @JsonIgnore
    private double currentPayoff;

    @JsonIgnore
    private double learningRate; // 学习速率

    @JsonIgnore
    private double exploreRate; // 探索率

    private final int type; // 所属的网络种类

    public Agent(int id,int actionNum,double exploreRate,double learningRate,int type) {
        super(id);
        this.actionNum = actionNum;
        this.exploreRate = exploreRate;
        this.learningRate = learningRate;
        this.type = type;
    }


    // TODO:训练时，初始化网络完成后，立即调用
    public void initQTables(){
        for(int neighborId:getNeighborsId()){
            qTables.put(neighborId,new QTable(neighborId,this.actionNum));
        }
    }


    public boolean sendMessageTo(Agent neighbor) {
        // 1： 向目标发送信息
        QTable q = qTables.get(neighbor.getId());
        Message message = new Message(this,neighbor,q.getQ());

        // 2： 取到自己给 neighbor 发的上一条消息
        Message old = neighbor.getMessages().get(this.getId());

        // 3：消息发送出去
        neighbor.messages.put(this.getId(),message);

        // 4：比较当前消息，与上一条消息的differ
        double differ = calDiffer(neighbor,old,message);

        return Math.abs(differ) > Config.messageDiffer;
    }

    /**
     * 比较当前发送的消息，与上次发送消息的差距
     * @param neighbor
     * @param messageOld
     * @param messageNew
     * @return
     */
    private double calDiffer(Agent neighbor,Message messageOld,Message messageNew){
        double scoreOld = -10000,scoreNew = -10000,tempOld,tempNew;
        for(int possibleAction = 0; possibleAction < neighbor.actionNum;possibleAction++){
//            平均值
//            scoreOld += messageOld == null ? 0 : messageOld.getMax(possibleAction);
//            scoreNew += messageNew.getMax(possibleAction);

//            最大值
            tempOld = messageOld == null ? 0 : messageOld.getMax(possibleAction);
            if(tempOld > scoreOld){
                scoreOld = tempOld;
            }
            tempNew = messageNew.getMax(possibleAction);
            if(tempNew > scoreNew){
                scoreNew = tempNew;
            }
        }
//        System.out.println("messageOld: "+ scoreOld);
//        System.out.println("messageNew: "+ scoreNew);

        double differ = scoreNew - scoreOld;
        if(Math.abs(differ) > Config.messageDiffer)
            System.out.println(this.getId()+ " cal message differ : " + scoreNew + " - " + scoreOld + " = " + differ);
        return differ;
    }


    /**
     * 以一定的探索率选择action
     * @return
     */

    private int maxAction = 0;

    public void setMaxAction(int maxAction) {
        this.maxAction = maxAction;
    }

    private int selectActionWithExploration(){
        if(Math.random() < exploreRate){  // 随机探索
            return Config.getRandomNumber(0,actionNum-1);
        }else{
            return maxAction;
        }
    }

    /**
     * 更新学习参数
     * @param expId
     */
    private boolean printed = false;
    private void updateLearningParameters(int expId){
        this.learningRate -= Config.deltaLearningRate;

        this.exploreRate -= Config.deltaExploreRate[type][expId];

        if(learningRate < 0.6){
            learningRate = 0.6;
        }

        if(exploreRate < 0){
            if(!printed){
                System.out.println(this.getId() + " explore rate decorate to zero !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                printed = true;
            }
            exploreRate = 0;
        }
    }

    public int getMaxSelfUtilityAction(){
        // 1：统计各个 action 在不同合作对象中的平均表现
        double [] actionsReward = new double[actionNum];
        for(QTable qTable:qTables.values()){
            for(int i = 0; i < qTable.getQ().length;i++){  // 我的action
                for(int j = 0;j < qTable.getQ()[i].length;j++){
                    actionsReward[i] += qTable.getQ()[i][j];
                }
            }
        }

        // 2：选择表现最好的 action
        int action = Config.getRandomNumber(0,actionNum-1);
        double maxValue = actionsReward[action];
        int maxAction = action;
        for(int i = 0; i < actionsReward.length;i++){
            if(actionsReward[i] > maxValue){
                maxValue = actionsReward[i];
                maxAction = i;
            }
        }
        return maxAction;
    }

    /**
     * 取出当前对整体最好的 action
     * // TODO: run dcop(max plus algorithm)
     * @return
     */
    public static class ActionUtility{
        int action;
        double utility;

        public ActionUtility(int action, double utility) {
            this.action = action;
            this.utility = utility;
        }

        public int getAction() {
            return action;
        }

        public double getUtility() {
            return utility;
        }
    }
    public ActionUtility getMaxUtilityAction(){
        int action = Config.getRandomNumber(0,actionNum-1);

        double maxValue = 0,temp = 0;
        for(Map.Entry<Integer,Message> entry:messages.entrySet()){
            maxValue += entry.getValue().getMax(action);
        }

        int maxAction = action;
        for(int i = 0; i < actionNum;i++){
            if(i != action){
                temp = 0;
                for(Map.Entry<Integer,Message> entry:messages.entrySet()){
                    temp += entry.getValue().getMax(i);
                }
                if(temp > maxValue){
                    maxValue = temp;
                    maxAction = i;
                }
            }
        }
        return new ActionUtility(maxAction,maxValue);
    }


    /**
     * 规定交互的两个agent可以看到对方彼此的 action，及最优action
     * @param partner
     * @param action
     * @param partnerAction
     * @param reward
     */
    private void updateQ(Agent partner,int action,int partnerAction,double reward){
        QTable qTable = qTables.get(partner.getId());
        QTable partnerQTable = partner.qTables.get(this.getId());

        // 1): 更新各自 Q table

//        int globalActionMe = getMaxUtilityAction(),globalActionPartner = partner.getMaxUtilityAction();
//        qTable.getQ()[action][partnerAction] = (1 - learningRate) * qTable.getQ()[action][partnerAction] + learningRate * (reward + Config.discountParameter * qTable.getQ()[globalActionMe][globalActionPartner]);
//        partnerQTable.getQ()[partnerAction][action] = (1 - partner.learningRate) * partnerQTable.getQ()[partnerAction][action] + partner.learningRate * (reward + Config.discountParameter * partnerQTable.getQ()[globalActionPartner][globalActionMe]) ;

        qTable.getQ()[action][partnerAction] = (1 - learningRate) * qTable.getQ()[action][partnerAction] + learningRate * reward;
        partnerQTable.getQ()[partnerAction][action] = (1 - partner.learningRate) * partnerQTable.getQ()[partnerAction][action] + partner.learningRate * reward;

//        System.out.println("Q["+this.getId() +"," + partner.getId()+"] ============================================================");
//        for(double [] a:qTable.getQ()){
//            System.out.println(Arrays.toString(a));
//        }
//        System.out.println("====================================================================");
    }

    public void training(int network){
//       learning============================================================================
        Analyze.connectionTimes++;

        // 1:随机选择一个agent，交互
        int randomPartner = Config.getRandomNumber(0, getNeighborsSize() - 1);
        Agent partner = (Agent) getNeighbors().get(randomPartner);

        // 2:select action a with probability pi(s,a) with some exploration.
        int action = selectActionWithExploration();
        int partnerAction = partner.selectActionWithExploration();

        // 3:更新双方Q
        double reward = Config.rewards[action][partnerAction];
        updateQ(partner, action, partnerAction, reward);

        // 4：更新 parameters（学习率，探索率等）
        updateLearningParameters(network);
        partner.updateLearningParameters(network);

        // 5:current reward
        currentPayoff = reward;
    }

    public double getCurrentPayoff() {
        return currentPayoff;
    }

    public void setCurrentPayoff(double currentPayoff) {
        this.currentPayoff = currentPayoff;
    }

    public int getType() {
        return type;
    }

    public int getActionNum() {
        return actionNum;
    }

    public Map<Integer, Message> getMessages() {
        return messages;
    }

    public static void main(String args []){
        double [] array = new double[300];
        System.out.println(Arrays.toString(array));
    }
}