package tju.scs.hxt.coordination.dcop.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tju.scs.hxt.coordination.dcop.Analyze;
import tju.scs.hxt.coordination.dcop.Config;
import tju.scs.hxt.coordination.dcop.network.Node;
import tju.scs.hxt.coordination.dcop.web.GlobalCache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by haoxiaotian on 2017/3/13 23:53.
 */
public class Agent extends Node{
    // 定义game 比较规整，简单；2 player,n-action game;
    private final int actionNum; // 每个 state 下的 action num

    // Q tables
    @JsonIgnore
    private Map<Integer,QTable> qTables = new HashMap<Integer, QTable>();

    // 统计到的信息，对方选择每个action的次数（概率）
    private Map<Integer,ObservedPolicy> observedPolicy = new HashMap<Integer, ObservedPolicy>();

    // 传播的消息： Key ——> this
    @JsonIgnore
    private Map<Integer,Message> messages = new HashMap<Integer, Message>();

    // coordination set
    @JsonIgnore
    private Set<Agent> coordinationSet = new HashSet<>();

    // default（init） coordination set
    @JsonIgnore
    private Set<Agent> defaultCoordinationSet = new HashSet<Agent>();

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

    /**
     * 初始化各方的 q table
     * comment:训练时，初始化网络完成后，立即调用
     */
    public void initQTables(){
        for(int neighborId:getNeighborsId()){
            qTables.put(neighborId,new QTable(neighborId,this.actionNum));
        }
    }

    /**
     * 初始化 Coordination Set，默认为所有邻居
     * comment:训练时，初始化网络完成后，立即调用
     */
    public void initCoordinationSet(){
        for(Node node:getNeighbors()){
            coordinationSet.add((Agent)node);
            defaultCoordinationSet.add((Agent)node);
        }
    }

    /**
     * 初始化对对手的action统计信息
     * comment:训练时，初始化网络完成后，立即调用
     */
    public void initObservedPolicy(){
        for(int id:getNeighborsId()){
            observedPolicy.put(id,new ObservedPolicy(id,this.actionNum));
        }
    }

    /**
     * 向 neighbor 发送 coordination message
     * @param neighbor
     * @return
     */
    public boolean sendMessageTo(Agent neighbor,int expId) {
        // 1： 向目标发送信息
        QTable q = qTables.get(neighbor.getId());
        Message message = new Message(this,neighbor,q.getQ());

        // 2： 取到自己给 neighbor 发的上一条消息
        Message old = neighbor.getMessages().get(this.getId());

        // 3：消息发送出去
        neighbor.messages.put(this.getId(),message);

        // 4：根据目前状态判断是否需要调整 coordination set
        if(!neighbor.getCoordinationSet().contains(this)){
            // 我的 coordination set 中有 neighbor，而 neighbor 的 coordination set 中没有我，则在neighbor中加入
            neighbor.getCoordinationSet().add(this);
        }

        // 5：比较当前消息，与上一条消息的differ
        double differ = calDiffer(neighbor,old,message);

        // 6：统计通信次数信息
        Analyze.incCommunicationTimes(type,expId);

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
//        if(Math.abs(differ) > Config.messageDiffer)
//            System.out.println(this.getId()+ " cal message differ : " + scoreNew + " - " + scoreOld + " = " + differ);
        return differ;
    }



    private int maxAction = 0;
    public void setMaxAction(int maxAction) {
        this.maxAction = maxAction;
    }

    public int getMaxAction() {
        return maxAction;
    }

    /**
     * 以一定的探索率选择action
     * @return selected action with current policy
     */
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
                for(Integer id:getNeighborsId()){
                    printQ(id);
                }
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
    }

    private void printQ(int partnerId){
        System.out.println("Q["+this.getId() +"," + partnerId+"] ============================================================");
        for(double [] a:qTables.get(partnerId).getQ()){
            System.out.println(toString(a));
        }
        System.out.println("====================================================================");
    }

    public static String toString(double[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        double v1;
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            v1 = (double)Math.round(a[i]*1000)/1000;
            b.append(v1);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * 每一次训练
     * expId 0: run DCOP,not adjust the coordination set (loss rate 0)
     * expId 1: run DCOP,adjuest the coordination set, loss rate 0
     * expId 2: run DCOP,adjuest the coordination set, loss rate 0.01
     * expId 3: run DCOP,adjuest the coordination set, loss rate 0.1
     * @param expId
     */
    public void training(int expId){
//       learning============================================================================

        // 1:随机选择一个agent，交互
        int randomPartner = Config.getRandomNumber(0, getNeighborsSize() - 1);
        Agent partner = (Agent) getNeighbors().get(randomPartner);

        // 2:select action a with probability pi(s,a) with some exploration.
        int action = selectActionWithExploration();
        int partnerAction = partner.selectActionWithExploration();

        // 3：observe 双方的action 选择 TODO:目前是统计所有 action（未加入最近限制）
        this.observedPolicy.get(partner.getId()).selectActions(action,partnerAction);
        partner.observedPolicy.get(this.getId()).selectActions(partnerAction,action);

        // 3:更新双方Q
        double reward = Config.rewards[action][partnerAction];
        updateQ(partner, action, partnerAction, reward);

        // 4：更新 parameters（学习率，探索率等）
        updateLearningParameters(expId);
//        partner.updateLearningParameters(expId);

        // 5:current reward
        currentPayoff = reward;

        if(expId != 0){
            // 6：TODO 更新 Coordination Set
            selectCoordinationSet(expId);
//            partner.selectCoordinationSet(expId);
        }
    }

    /**
     * TODO: dynamically select the coordination set (to reduce the messages sent by the algorithm)
     */
    private void selectCoordinationSet(int expId) {
        // 1：cal max loss
        double maxLoss = Config.loseRate[expId] * Math.max(Math.abs(potentialExpectedUtility(defaultCoordinationSet)),potentialLossInLockOfCoordination(new HashSet<Agent>()));

        // 2：计算 coordination set
        Set<Agent> tempCoordinationSet = new HashSet<Agent>();
        // 2.1：如果 coordination set 可以为空
        if(potentialLossInLockOfCoordination(tempCoordinationSet) < maxLoss){
            coordinationSet.clear();
            return;
        }

        // 2.2 coordination set，从小到大选择
        double markedLoss = 1000000,tempLoss;
        Set<Agent> markedCoordinationSet = null;
        // 从最小一个元素开始，逐渐递增遍历
        boolean found = false;
        for(int csNum = 1; csNum < defaultCoordinationSet.size() && !found;csNum++){
            // 依次遍历所有包含 csNum 个元素的集合
            for(Set<Agent> selected:selectSet(defaultCoordinationSet,csNum)){
                tempLoss = potentialLossInLockOfCoordination(selected);
                if(tempLoss < maxLoss){
                    found = true;
                    if(tempLoss < markedLoss){
                        markedLoss = tempLoss;
                        markedCoordinationSet = selected;
                    }
                }
            }
        }
        if(found){
            coordinationSet = markedCoordinationSet;
            System.out.println("resize the coordination set!" + defaultCoordinationSet.size() + " ——> " + coordinationSet.size());
        }else{
            coordinationSet = defaultCoordinationSet;
//            System.out.println("use the default coordination set!" + defaultCoordinationSet.size() + " == " + coordinationSet.size());
        }
    }

    /**
     * 递归组合算法，遍历所有子集
     * @param range
     * @param csNum
     * @return
     */
    private static List<Set<Agent>> selectSet(Set<Agent> range,int csNum) {
        List<Set<Agent>> result = new ArrayList<Set<Agent>>();
        if(csNum == 0){
            result.add(new HashSet<Agent>());  // 一个空解
            return result;
        }

        if(csNum == range.size()){
            result.add(new HashSet<Agent>(range));
            return result;
        }

        if(csNum > range.size()){
            return result;
        }

        Set<Agent> temp = new HashSet<>();
        List<Set<Agent>> subResult;
        Set<Agent> subRange = new HashSet<>(range);
        for(Iterator<Agent> iterator = range.iterator();iterator.hasNext();){
            Agent currentEle = iterator.next();
            // 1：放当前元素
            temp.add(currentEle);
            subRange.remove(currentEle);
            subResult = selectSet(subRange,csNum-1);
            if(subResult.size() > 0){
                for(Set<Agent> subItem:subResult){
                    subItem.addAll(temp);
                }
                result.addAll(subResult);
                // 2：不放当前元素
                temp.remove(currentEle);
            }else{
                break;
            }
        }
        return result;
    }

    /**
     * Potential Loss In Lock Of Coordination with NC,NC = (U - CS)
     * @param CS only coordinating with CS
     * @return
     */
    private double potentialLossInLockOfCoordination(Set<Agent> CS){
        return potentialExpectedUtility(defaultCoordinationSet) - potentialExpectedUtility(CS);
    }

    /**
     * 计算 max potential expected utility of me exclusively coordinating with coordinationSet
     * @param coordinationSet
     * @return
     */
    private double potentialExpectedUtility(Set<Agent> coordinationSet){
        double [] poes = new double[actionNum];
        for(int actionMe = 0; actionMe < actionNum;actionMe++){
            poes[actionMe] = potentialExpectedUtility(coordinationSet,actionMe);
        }

        return getMax(poes);
    }

    /**
     * 计算 potential expected utility of me (with actionMe) exclusively coordinating with coordinationSet
     * @param coordinationSet
     * @param actionMe
     * @return
     */
    private double potentialExpectedUtility(Set<Agent> coordinationSet,int actionMe){
        double poe = 0;
        // 1：显示的 coordination set 中的每一个 agent
        for(Agent partner:coordinationSet){
            poe += getMax(qTables.get(partner.getId()).getQ()[actionMe]);
        }

        // 2：隐式的，虽然未在 coordination set，中，但根据统计对手选择action的概率，计算一个 reward 期望
        Set<Agent> others = new HashSet<Agent>();
        for(Agent agent:defaultCoordinationSet){
            if(!coordinationSet.contains(agent)){
                others.add(agent);
            }
        }
        for(Agent partner:others){
            poe += calExpectedReward(partner,actionMe);
        }

        return poe;
    }

    /**
     * 根据统计信息，计算期望reward
     * @param partner
     * @param actionMe
     * @return
     */
    private double calExpectedReward(Agent partner,int actionMe){
        double er = 0;
        double [] q = qTables.get(partner.getId()).getQ()[actionMe];
        for(int actionPartner = 0; actionPartner < partner.actionNum;actionPartner++){
            er += observedPolicy.get(partner.getId()).getProbability(actionMe,actionPartner) * q[actionPartner];
        }
        return er;
    }

    private static double getMax(double [] array){
        double max = array[0];
        for(int i = 1;i < array.length;i++){
            if(array[i] > max){
                max = array[i];
            }
        }
        return max;
    }

    public double getCurrentPayoff() {
        return currentPayoff;
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

    public Set<Agent> getCoordinationSet() {
        return coordinationSet;
    }

    public double getExploreRate() {
        return exploreRate;
    }

    public void setExploreRate(double exploreRate) {
        this.exploreRate = exploreRate;
    }

    public static void main(String args []){
//        double [] array = new double[300];
//        System.out.println(Arrays.toString(array));

        Set<Agent> set = new HashSet<>();
        for(int i = 0; i < 5;i++){
            set.add(new Agent(i,3,0,0,0));
        }
        System.out.println(selectSet(set, 5));
    }
}