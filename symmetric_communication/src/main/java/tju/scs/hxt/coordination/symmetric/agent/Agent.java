package tju.scs.hxt.coordination.symmetric.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tju.scs.hxt.coordination.symmetric.Analyze;
import tju.scs.hxt.coordination.symmetric.Config;
import tju.scs.hxt.coordination.symmetric.network.Node;
import tju.scs.hxt.coordination.symmetric.q.QItem;
import tju.scs.hxt.coordination.symmetric.web.GlobalCache;

import java.util.*;

/**
 * Created by haoxiaotian on 2017/3/13 23:53.
 */
public class Agent extends Node{
    // 定义game 比较规整，简单；2 player,n-action game;
    private final int actionNum; // 每个 state 下的 action num

    @JsonIgnore
    private final QItem[] qValues; // Q Table

    // 策略，即选择某一个action的概率
    private double [] policy;

    private double []defaultPolicy;

    private double [] averagePolicy;

    private double currentPayoff;

    private double learningRate; // 学习速率
    private double exploreRate; // 探索率


    private final int type; // 所属的网络种类

    private int currentBadTimes; // 当前bad reward 次数

    public void setCurrentBadTimes(int currentBadTimes) {
        this.currentBadTimes = currentBadTimes;
    }

    // 对邻居 agent 行为的观察
    private Observations observations = new Observations();

    private final Integer resources = Config.resources;

    // 学习次数
    @JsonIgnore
    private int learningTimes = 0;


    private class Observation{
        private int times;

        private int [] actions;

        private double [] policy;

        private int badTimes = 0;

        public Observation(int actionNum){
            actions = new int[actionNum];
            policy = new double[actionNum];
            initActions();
        }

        private void initActions(){
            clear();
        }

        private void clear(){
            for(int a = 0; a < actions.length;a++){
                actions[a] = 0;
                policy[a] = 0;
            }
            this.times = 0;
        }

        private void observe(int action,double reward){
            this.actions[action]++;
            this.times++;
            if(reward < 0){
                badTimes++;
            }
        }

        private boolean observeEnough(){
            return this.times == 25;
        }

        private double [] getPolicy(){
            for(int a = 0;a < actions.length;a++){
                policy[a] = actions[a] / 100.0;
            }

            return policy;
        }
    }

    private class Observations{
        Map<Integer,Observation> observations = new HashMap<>();

        public void observeNewNeighbor(Agent agent){
            observations.put(agent.getId(),new Observation(agent.actionNum));
        }

        public void reObserve(Agent agent){
            if(observations.get(agent.getId()) == null){
                observeNewNeighbor(agent);
                return;
            }

            observations.get(agent.getId()).clear();
        }

        public int getBadTimesFor(Agent agent){
            if(observations.get(agent.getId()) == null){
                return 0;
            }
            return observations.get(agent.getId()).badTimes;
        }

        public void observe(Agent agent,int action,double reward){
            if(observations.get(agent.getId()) == null){
                observeNewNeighbor(agent);
            }
            observations.get(agent.getId()).observe(action,reward);
        }

        public boolean observeEnough(Agent agent){
            if(observations.get(agent.getId()) == null){
                return false;
            }

            return observations.get(agent.getId()).observeEnough();
        }

        public double [] getPolicy(Agent agent){
            return observations.get(agent.getId()).getPolicy();
        }
    }

    public Agent(int id,int actionNum,double exploreRate,double learningRate,int type) {
        super(id);
        this.actionNum = actionNum;
        this.exploreRate = exploreRate;
        this.learningRate = learningRate;
        this.qValues = new QItem[actionNum];
        this.type = type;
        // 初始化 q tables
        initTables();

        // 初始化 action 选择策略
        initPolicy();
    }

    // 初始化 Q Table
    private void initTables(){
        for(int j = 0; j < this.actionNum; j++){
            qValues[j] = new QItem();
        }
    }


    /**
     * 计算投票权重：agent权重与q-value 的线性组合
     * @param priority
     * @param fmq
     * @return
     */
    private double calWeightedProportion(double priority,double fmq){
        return 0.4 * priority + 0.6 * fmq;
    }


    public int getConnectionTimes(){
        return learningTimes;
    }

    /**
     * 打印 Q Table
     */
    public void printQTable(){
        if(Config.printLog) {
            System.out.println("==================================== agent"+getId()+" Q TABLE=========================================");
            System.out.println("agent"+getId()+" update q table " + learningTimes +" times.");
            for(int j = 0; j < actionNum; j++){
                if(j == Config.expectedAction){
                    System.out.print(" ["+qValues[j] + "] ");
                }else{
                    System.out.print("  "+qValues[j] + "  ");
                }
            }
            System.out.println();
            System.out.println("   ===========================================Q TABLE===========================================");
            System.out.println();
        }
    }

    // 统计neighbor的策略，以便衡量与某个neighbor的相似度（neighbor的策略由通信是负责传递）
    private Map<Integer,NeighborPolicy> policyOfNeighbors = new HashMap<>();

    /**
     * 与所有邻居双向同步策略
     */
    private void communicateWithAllNeighbors(){
        for(Node node:getNeighbors()){
            communicateWithNeighbor((Agent)node);
        }
    }

    private double [] getFMQ(){
        double [] fmq = new double[actionNum];
        int a = 0;
        for(QItem qItem:qValues){
            fmq[a] = qItem.getFmq();
            a++;
        }
        return fmq;
    }

    /**
     * 与邻居agent双向同步策略
     * @param agent
     */
    private void communicateWithNeighbor(Agent agent){
        this.sendPolicyTo(agent,this.policy,getFMQ());
    }

    private void sendPolicyTo(Agent agent,double [] policy,double [] fmq){
        NeighborPolicy neighborPolicy = new NeighborPolicy(agent.getId(),policy,fmq);
        policyOfNeighbors.put(neighborPolicy.id,neighborPolicy);
    }

    /**
     * 初始化策略，即选择某一个action的概率
     * 初始化为 1/n
     */
    private void initPolicy(){
        policy = new double[actionNum];
        defaultPolicy = new double[actionNum];
        averagePolicy = new double[actionNum];
        for(int j = 0; j < actionNum;j++){
            policy[j] = defaultPolicy[j] = averagePolicy[j] = ((double)1)/actionNum;
        }
    }

    /**
     * agent 更新自己的策略(选择不同action的概率)
     */
    private void updatePolicy(){
        int bestAction = getMaxUtilityAction();
        // 更新 row action
        for(int i = 0; i < actionNum;i++){
            policy[i] = exploreRate/actionNum;
        }
        policy[bestAction] += (1-exploreRate);
//        printPolicy();
    }

    private void printPolicy(){
        for(int i = 0; i < actionNum;i++){
            System.out.print(policy[i] + ", ");
        }
        System.out.print("\n");
    }

    /**
     * 沟通时根据对手的 Q table 进行调整
     * @param agent2
     */
    private void adjustQWithNeighbor(Agent agent2){
        if(policyOfNeighbors.get(agent2.getId()) == null){  // 之前：他们之间没有互通消息
            for(int a = 0; a < this.actionNum;a++){  // 对没一个action
                this.qValues[a].setEq(this.qValues[a].getFmq());
            }
        }else{
            for(int a = 0; a < this.actionNum;a++){  // 对没一个action
                this.qValues[a].setEq(calEq(this.qValues[a].getFmq(), policyOfNeighbors.get(agent2.getId()).fmq[a]));
//            this.qValues[a].setEq(calEq(calWeightedProportion(this.getPriority(), this.qValues[a].getFmq()),calWeightedProportion(agent2.getPriority(),policyOfNeighbors.get(agent2.getId()).fmq[a])));
            }
        }
    }

    /**
     * 费米函数
     * @param fmq1
     * @param fmq2
     * @return
     */
    private double calImitationP(double fmq1,double fmq2){
        return 1 / (1 + Math.pow(Math.E,-Config.imitationParameter * (fmq2 - fmq1)));
    }

    /**
     * 比例模仿
     * @param fmq1 自己是fmq1
     * @param fmq2 对方是fmq2
     * @return
     */
    private double calEq(double fmq1,double fmq2){
        double p = calImitationP(fmq1,fmq2);
        return (1 - p) * fmq1 + p * fmq2;
    }


    /**
     * 发送给对手的信息
     */
    private class NeighborPolicy{
        private int id;

        private double [] policy;

        private double [] fmq;

        public NeighborPolicy(int id,double[] policy,double [] fmq) {
            this.id = id;
            backup(policy);  // 镜像邻居的策略
            this.fmq = fmq;
        }

        private void backup(double [] policy){
            this.policy = Arrays.copyOf(policy,policy.length);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NeighborPolicy)) return false;
            NeighborPolicy that = (NeighborPolicy) o;
            if (id != that.id) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }


    /**
     * 计算与neighbor id 的策略相似度
     * @param id
     * @return
     */
    private double calSimilarOfAgent(int id){
        // 还未收到过对方的通知
        if(policyOfNeighbors.get(id) == null){
            return cosSimilar(this.policy,this.defaultPolicy);
        }else{
            double [] policy = policyOfNeighbors.get(id).policy;
            return cosSimilar(this.policy,policy);
        }
    }

    /**
     * 计算余弦相似度
     * @param policy1
     * @param policy2
     * @return
     */
    private static double cosSimilar(double [] policy1,double [] policy2){
        double numerator = 0,denominator,part1 = 0,part2 = 0;
        for(int i = 0;i < policy1.length;i++){
            numerator += policy1[i] * policy2[i];
            part1 += policy1[i] * policy1[i];
            part2 += policy2[i] * policy2[i];
        }
        denominator = Math.sqrt(part1) * Math.sqrt(part2);
        return numerator / denominator;
    }

    /**
     * default 认为输入数组中都是 > 0 的数
     * @param input
     * @return
     */
    private int[] toRouletteIntArray(double [] input){
        double total = sum(input);
        int [] result = new int[input.length];
        for(int i = 0;i < input.length;i++){
            result[i] = (int)(input[i] * 100 / total);
        }
        return result;
    }

    /**
     * 轮盘赌
     * @param ids
     * @param values
     * @return
     */
    private static Object roulette(Object[] ids,int[] values){
        int pointId = 0;
        int pointValue = values[pointId];
        int length = sum(values);
        int cut = Config.getRandomNumber(1,length);
        try {
            while(pointValue < cut){
                pointId++;
                pointValue+=values[pointId];
            }
            return ids[pointId];
        }catch (Exception e){
            printArray(values);
//            System.out.println(cut);
//            throw e;
        }
        return ids[0];
    }

    private static void printArray(int []array){
        for(int i:array){
            System.out.print(i + " ");
        }
        System.out.print("\n");
    }

    private static void printArray(double []array){
        for(double i:array){
            System.out.print(i + " ");
        }
        System.out.print("\n");
    }

    private static int sum(int[] values){
        int sum = 0;
        for(int ele:values){
            sum += ele;
        }
        return sum;
    }

    private static double sum(double[] values){
        double sum = 0;
        for(double ele:values){
            sum += ele;
        }
        return sum;
    }



    /**
     * 以一定的探索率选择action
     * @return
     */
    private int selectActionWithExploration(){
        if(Math.random() < exploreRate){  // 随机探索
            return Config.getRandomNumber(0,actionNum-1);
        }else{
            return getMaxUtilityAction();
        }
    }

    private int toZeroTimes = 0;

    private void updateParameters(int expId){
        this.learningRate -= Config.deltaLearningRate;

        this.exploreRate -= Config.deltaExploreRate[type][expId];

        if(learningRate < 0.7){
            learningRate = 0.7;
        }

        if(exploreRate < 0){
            exploreRate = 0;
            if(toZeroTimes == 0) {
                toZeroTimes++;
//                System.out.println("exploreRage is 0");
            }
        }
    }

    /**
     * 取出当前最好的 action
     * @return
     */
//    public int getMaxUtilityAction(){
//        int action = Config.getRandomNumber(0,actionNum-1);
//
//        double maxValue = qValues[action].getFmq();
//        int maxAction = action;
//        for(int i = 0; i < qValues.length;i++){
//            if(qValues[i].getFmq() > maxValue){
//                maxValue = qValues[i].getFmq();
//                maxAction = i;
//            }
//        }
//        return maxAction;
//    }
    public int getMaxUtilityAction(){
        int action = Config.getRandomNumber(0,actionNum-1);

        double maxValue = qValues[action].getEq();
        int maxAction = action;
        for(int i = 0; i < qValues.length;i++){
            if(qValues[i].getEq() > maxValue){
                maxValue = qValues[i].getEq();
                maxAction = i;
            }
        }
        return maxAction;
    }

    private Object selectActionWithPartner(Agent agent){
        int actionMe = getMaxUtilityAction();
        int actionOther = agent.getMaxUtilityAction();

        if(Math.random() < exploreRate){  // 随机探索
            return Config.getRandomNumber(0,actionNum-1);
        }else{
//            if(Math.random() < 0.5){
//                return actionMe;
//            }else {
//                return actionOther;
//            }

            double valueMe = qValues[actionMe].getFmq(),valueOther = agent.qValues[actionOther].getFmq();

            if(valueMe < 0 && valueOther < 0){
                double temp = -valueMe;
                valueMe = -valueOther;
                valueOther = temp;
            }

            if(valueMe < 0 || valueOther < 0){
                if(valueMe > valueOther){
                    return actionMe;
                }else {
                    return actionOther;
                }
            }

            return roulette(new Object[]{actionMe,actionOther},new int[]{(int)Math.ceil(valueMe * 1000),(int)Math.ceil(valueOther * 1000)});
        }
    }


    private void updateFMQ(Agent partner,int action,int partnerAction,double reward){
        // 1): 更新各自 Q table
        qValues[action].setqValue((1 - learningRate) *  qValues[action].getqValue() + learningRate * (reward + 0.8 * qValues[getMaxUtilityAction()].getqValue()));
//        partner.qValues[partnerAction].setqValue((1 - partner.learningRate) *  partner.qValues[partnerAction].getqValue() + partner.learningRate * reward);

        // 2): 更新总（s,a）次数
        qValues[action].increaseTotalTimes();  // 总次数
//        partner.qValues[partnerAction].increaseTotalTimes();

        // 3): 更新各自 maxReward under (s,a)
        if(reward > qValues[action].getMaxQValue()) {
            qValues[action].setMaxQValue(reward);  // maxReward
            qValues[action].setMaxQTimes(1);
        }else if(reward == qValues[action].getMaxQValue()){
            qValues[action].increaseMaxTimes();
        }

//        if(reward > partner.qValues[partnerAction].getMaxQValue()) {
//            partner.qValues[partnerAction].setMaxQValue(reward);
//            partner.qValues[partnerAction].setMaxQTimes(1);
//        }else if(reward == partner.qValues[partnerAction].getMaxQValue()){
//            partner.qValues[partnerAction].increaseMaxTimes();
//        }

        // 4): 更新 max frequency under (s,a)
        qValues[action].setFrequency();
//        partner.qValues[partnerAction].setFrequency();

        // 5): 更新 FMQ
        qValues[action].updateFMQ(Config.weightFactorForFMQ);  // 总次数
//        partner.qValues[partnerAction].updateFMQ(Config.weightFactorForFMQ);
    }

    public static String getLevel(double similar){
        if(similar <= 0.1){
            return "0~0.1";
        }else if(similar <= 0.2){
            return "0.1~0.2";
        }else if(similar <= 0.3){
            return "0.2~0.3";
        }else if(similar <= 0.4){
            return "0.3~0.4";
        }else if(similar <= 0.5){
            return "0.4~0.5";
        }else if(similar <= 0.6){
            return "0.5~0.6";
        }else if(similar <= 0.7){
            return "0.6~0.7";
        }else if(similar <= 0.8){
            return "0.7~0.8";
        }else if(similar <= 0.9){
            return "0.8~0.9";
        }else if(similar <= 0.92){
            return "0.9~0.92";
        }else if(similar <= 0.94){
            return "0.92~0.94";
        }else if(similar <= 0.96){
            return "0.94~0.96";
        }else if(similar <= 0.98){
            return "0.96~0.98";
        }else{
            return "0.98~1";
        }
    }


    private boolean needCommunicate(Agent agent){
        if(observations.observeEnough(agent)){
            double [] policyObserved = observations.getPolicy(agent);
            double [] policyOfMe = this.policy;
            double [] policyLastCommunicated = policyOfNeighbors.get(agent.getId()) == null?defaultPolicy:policyOfNeighbors.get(agent.getId()).policy;
//            double similar = cosSimilar(policyLastCommunicated,policyObserved);
            double similar = cosSimilar(policyObserved,policyOfMe);
            double similar2 = cosSimilar(policyOfMe,policyLastCommunicated);
            double similar3 = cosSimilar(policyObserved,policyLastCommunicated);
//            printArray(policyLastCommunicated);
//            printArray(policyObserved);
//            printArray(policyOfMe);
//            System.out.println(similar + " ; "+observations.observations.get(agent.getId()).times);
            String label = getLevel(similar);
            Analyze.times.put(label, Analyze.times.get(label) == null ? 1:Analyze.times.get(label)+1);
            observations.reObserve(agent);
//            return Math.random() < 0.5;
//            return similar < 0.985 || similar2 < 0.985 || similar3 < 0.985;
            return similar < 0.91;
        }else{
            return false;
        }
    }

    /**
     * 选择出需要沟通的agents
     * @param neighbors
     * @return
     */
    private List<Agent> getCommunicateTarget(List<Node> neighbors){
        ArrayList<Agent> candidate = new ArrayList<>();
        List<Agent> result = new ArrayList<>();

        // 选出需要 communicate 的agents
        for(Node node:neighbors){
            if(needCommunicate((Agent)node)){
                candidate.add((Agent)node);
            }
        }

        if(candidate.size() <= this.resources){
            return candidate;
        }else{  //候选agent > 资源总数
            Object [] ids;
            double [] priority;
//            System.out.println(candidate);
            while(result.size() < this.resources && candidate.size() > 0){
                ids = candidate.toArray();

                priority = new double[ids.length];
                int index = 0;
                for(Object o:ids){
                    priority[index++] = ((Agent)o).getCentrality();
                }
                // 根据agent 的 centrality 进行轮盘赌
                Agent selected = (Agent)roulette(ids,toRouletteIntArray(priority));
                result.add(selected);
                candidate.remove(selected);
            }
            return result;
        }
    }


    PriorityQueue<Agent> priorityQueue = new PriorityQueue<>(10, new Comparator<Agent>() {
        @Override
        public int compare(Agent o1, Agent o2) {
            return o2.currentBadTimes - o1.currentBadTimes;
        }
    });
    private Agent getCommunicateTargetWithMostBad(List<Node> neighbors){
        priorityQueue.clear();
        ArrayList<Agent> candidate = new ArrayList<>();
        List<Agent> result = new ArrayList<>();

        boolean needCommunication = false;
        // 选出需要 communicate 的agents
        for(Node node:neighbors){
            if(observations.observeEnough((Agent)node)){
                observations.reObserve((Agent)node);
                needCommunication = true;
            }
        }

        if(needCommunication){
            for(Node node:neighbors){
                ((Agent)node).setCurrentBadTimes(observations.getBadTimesFor(((Agent)node)));
                priorityQueue.add((Agent)node);
            }
        }

        return priorityQueue.poll();
    }


    int remainResources = 20;
    int curPos = 0;
    boolean canCommunicate = true;

    public void training(int expId){

//      communication ============================================================================
        // 每 100 轮，只有 20 次communication的机会
        curPos++;
        if(curPos < 100){  // 当前一轮
            if(remainResources > 0){  // 有可用的通信资源
                canCommunicate = true;
                remainResources--;
            }else{
                canCommunicate = false;
            }
        }else if(curPos == 100){  // 当前一轮结束
            System.out.println(type + ":" + expId + " resources refreshed =========================== !");
            curPos = 0;
            canCommunicate = true;
            remainResources = 20;
        }
        curPos %= 100;

        Analyze.connectionTimes++;

        if(expId == 0){
            if(canCommunicate){   // 以某种策略选择通信与否
                // 1：选择出需要 communicate 的agents : 蓝色的
                List<Agent> communicateTargets = getCommunicateTarget(getNeighbors());
                for(Agent target:communicateTargets){
                    // 2:communication 与邻居互相同步信息
                    communicateWithNeighbor(target);
                    target.communicateWithNeighbor(this);
                    Analyze.communicationTimes++;

                    // 3:根据邻居的 Q value，互相进行调整
                    adjustQWithNeighbor(target);
                    (target).adjustQWithNeighbor(this);
                }

//                Agent target = getCommunicateTargetWithMostBad(getNeighbors());
//
//                communicateWithNeighbor(target);
//                target.communicateWithNeighbor(this);
//                Analyze.communicationTimes++;
//
//                // 3:根据邻居的 Q value，互相进行调整
//                adjustQWithNeighbor(target);
//                (target).adjustQWithNeighbor(this);
            }else{
                System.out.println(type + ":" + expId + " use up resources!");
            }
        }else if(expId == 1){   // 通信资源不受限制，并且随机通信
            if(Math.random() < 0.5){   // ：红色
                int randomPartner = Config.getRandomNumber(0, getNeighborsSize() - 1);
                Agent partner = (Agent) getNeighbors().get(randomPartner);
                // 2:communication 与邻居互相同步信息
                communicateWithNeighbor(partner);
                partner.communicateWithNeighbor(this);
                Analyze.communicationTimes++;

                // 3:根据邻居的 Q value，互相进行调整
                adjustQWithNeighbor(partner);
                (partner).adjustQWithNeighbor(this);
            }
        }else if(expId == 2){   // 通信资源受限，随机通信
            if(canCommunicate) {
                if (Math.random() < 0.5) {
                    int randomPartner = Config.getRandomNumber(0, getNeighborsSize() - 1);
                    Agent partner = (Agent) getNeighbors().get(randomPartner);
                    // 2:communication 与邻居互相同步信息 ：绿色
                    communicateWithNeighbor(partner);
                    partner.communicateWithNeighbor(this);
                    Analyze.communicationTimes++;

                    // 3:根据邻居的 Q value，互相进行调整
                    adjustQWithNeighbor(partner);
                    (partner).adjustQWithNeighbor(this);
                }
            }else{
                System.out.println(type + ":" + expId + " use up resources!");
            }
        }


//       learning============================================================================

        // 1:随机选择一个agent，交互
        int randomPartner = Config.getRandomNumber(0, getNeighborsSize() - 1);
        Agent partner = (Agent) getNeighbors().get(randomPartner);
//            Agent partner = (Agent)agent;

        // 2:select action a with probability pi(s,a) with some exploration.
        int action = selectActionWithExploration();
        int partnerAction = partner.selectActionWithExploration();

        // 3:更新 FMQ
        double reward = Config.rewards[action][partnerAction];
        updateFMQ(partner, action, partnerAction, reward);

        // 4：互相统计对手选择action情况
        observations.observe(partner,partnerAction,reward);
//            partner.observations.observe(this,action);

        // 5:更新彼此学习次数
        this.learningTimes++;
//            partner.learningTimes++;

        // 6：更新 parameters（学习率，探索率等）
        updateParameters(expId);
//            partner.updateParameters(ref);

        // 7: 更新选择action 的策略
        updatePolicy();
//            partner.updatePolicy();

        // 8:current reward
        currentPayoff = reward;

    }


    public QItem[] getQValues(){
        return qValues;
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


    public static void main(String args[]){
        System.out.println(cosSimilar(new double[]{-1,-1,-1,-1,0.8,-1,-1,-1,-1,-1},new double[]{-1,-1,-1,-1,-1,-1,-1,0.5,-1,-1}));

        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>(10, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });

        priorityQueue.add(1);
        priorityQueue.add(2);
        priorityQueue.add(3);
        priorityQueue.add(4);
        System.out.println(priorityQueue.poll());
    }
}