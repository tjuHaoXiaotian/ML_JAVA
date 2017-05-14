package tju.scs.hxt.coordination.symmetric.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tju.scs.hxt.coordination.symmetric.Config;
import tju.scs.hxt.coordination.symmetric.network.Node;
import tju.scs.hxt.coordination.symmetric.queue.BoundedPriorityBlockingQueue;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by haoxiaotian on 2017/3/13 23:53.
 */
public class Agent extends Node{
    // 定义game 比较规整，简单；2 player,n-action game;
    private final int actionNum; // 每个 state 下的 action num

    @JsonIgnore
    private final double[] qValues; // Q Table

    // 策略，即选择某一个action的概率
    private double [] policy;

    private double []defaultPolicy;

    private double [] averagePolicy;

    private double learningRate; // 学习速率
    private double exploreRate; // 探索率

    private final int type; // 所属的网络种类

    // 学习次数
    @JsonIgnore
    private int learningTimes = 0;

    // 统计此agent与所有邻居agent的合作信息
    private Map<Integer,Statistics> cooperationStatistics = new HashMap<Integer, Statistics>();

    public Agent(int id,int actionNum,double exploreRate,double learningRate,int type) {
        super(id);
        this.actionNum = actionNum;
        this.exploreRate = exploreRate;
        this.learningRate = learningRate;
        this.qValues = new double[actionNum];
        this.type = type;
        // 初始化 q tables
        initTables();

        // 初始化 action 选择策略
        initPolicy();
    }

    // 初始化 Q Table
    private void initTables(){
        for(int j = 0; j < this.actionNum; j++){
            qValues[j] = 0;
        }
    }

    // action：priority 投票字典
    private Map<Integer,Double> counts = new HashMap<Integer, Double>();

    /**
     * 计算投票权重：agent权重与q-value 的线性组合
     * @param priority
     * @param fmq
     * @return
     */
    private double calWeightedProportion(double priority,double fmq){
        return 0.6 * priority + 0.4 * fmq;
    }

    /**
     * 简单统计加权投票排名信息，返回最大值
     * 加权信息：priority：fmqValue = 0.8:0.2
     * @param rowAction
     * @return
     */
    private int countActionPriorityPair(boolean rowAction){
        return 0;
    }

    /**
     * 更新附带权重信息
     */
    private void updateLinkedPriority(){
        double linkedPriority = 0;
        for(Node agent:getNeighbors()){
            linkedPriority += (this.cooperationStatistics.get(((Agent)agent).getId()) == null ?
                    0 :this.cooperationStatistics.get(((Agent)agent).getId()).getPositiveFrequency() * agent.getPriority());
        }
        this.setLinkedPriority(linkedPriority);
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

    /**
     * 与邻居agent双向同步策略
     * @param agent
     */
    private void communicateWithNeighbor(Agent agent){
        this.sendPolicyTo(agent,this.policy);
        agent.sendPolicyTo(this,agent.policy);
    }

    private void sendPolicyTo(Agent agent,double [] policy){
        NeighborPolicy neighborPolicy = new NeighborPolicy(agent.getId(),policy);
        policyOfNeighbors.put(neighborPolicy.id,neighborPolicy);
    }

    /**
     * 策略，即选择某一个action的概率
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


    private class NeighborPolicy{
        private int id;

        private double [] policy;

        public NeighborPolicy(int id,double[] policy) {
            this.id = id;
            backup(policy);  // 镜像邻居的策略
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

        public NeighborPolicy updatePolicy(double []policy){
            this.policy = policy;
            return this;
        }
    }

    /**
     * 根据差异度，进行轮盘赌，选择出一个agent
     * @return
     */
    private int chooseTheMostFarAgent(Set<Integer> excepts){
        Map<Integer,Double> similarity = calSimilarOfNeighbors();
        int [] ids = new int [similarity.size()];
        int [] values = new int [similarity.size()];
        int index = 0;
        for(Map.Entry<Integer,Double> entry:similarity.entrySet()){
            ids[index] = entry.getKey();
            values[index] = (int)Math.round((1 - entry.getValue()) * 100);
        }

        int id = roulette(ids,values);
        while (excepts != null && excepts.contains(id)){
            id = roulette(ids,values);
        }
        return id;
    }

    private Agent getNeighborById(int id){
        for(Node n:getNeighbors()){
            if(n.getId() == id){
                return (Agent)n;
            }
        }
        return null;
    }

    /**
     * 计算所有邻居与自己的相似度
     * @return
     */
    private Map<Integer,Double> calSimilarOfNeighbors() {
        Map<Integer, Double> similarity = new HashMap<>(getNeighborsSize());
        for (Node neighbor : getNeighbors()) {
            similarity.put(neighbor.getId(), calSimilarOfAgent(neighbor.getId()));
        }
        return similarity;
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
    private double cosSimilar(double [] policy1,double [] policy2){
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
     * 轮盘赌
     * @param ids
     * @param values
     * @return
     */
    private static int roulette(int[] ids,int[] values){
        int pointId = 0;
        int pointValue = values[pointId];
        int length = sum(values);
        int cut = Config.getRandomNumber(0,length);
        while(pointValue < cut){
            pointId++;
            pointValue+=values[pointId];
        }
        return ids[pointId];
    }

    private static int sum(int[] values){
        int sum = 0;
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
            return selectActionWithMixedPolicy(this.policy);
        }
    }

    /**
     * 以混合策略 PI，选择action
     * @param policy
     * @return
     */
    private static int selectActionWithMixedPolicy(double [] policy){
        int [] ids = new int [policy.length];
        int [] intPolicy = new int [policy.length];
        for(int i = 0; i < policy.length;i++){
            intPolicy[i] = (int)Math.round(1000 * policy[i]);
            ids[i] = i;
        }

        return roulette(ids,intPolicy);
    }


    private double climbRateWin = 0.4;
    private double climbRageLose = 0.8;

    private void updateParameters(){
        this.learningRate -= 0.15 / learningTimes;
        this.exploreRate -= 0.15 / learningTimes;
        this.climbRageLose -= 0.15 / learningTimes;
        this.climbRateWin -= 0.15 / learningTimes;

        if(learningRate < 0.2){
            learningRate = 0.2;
        }
        if(exploreRate < 0){
            exploreRate = 0;
        }
        if(climbRateWin < 0.01){
            climbRateWin = 0.01;
        }
        if(climbRageLose < 0.01){
            climbRageLose = 0.01;
        }
    }

    /**
     * Win or Learn Fast：计算学习速率
     * @return
     */
    private double getClimbingRate(){
        double currentWeightedUtility = 0,avgWeightedUtility = 0;
        for(int a = 0;a < actionNum;a++){
            currentWeightedUtility += policy[a] * qValues[a];
            avgWeightedUtility += averagePolicy[a] * qValues[a];
        }
        if(currentWeightedUtility > avgWeightedUtility){
//            System.out.println("当前策略 is good -------------------------------------- ！");
            return climbRateWin;
        }else{
//            System.out.println("当前策略 is bad！");
            return climbRageLose;
        }
    }

    /**
     * 取出当前最好的 action
     * @return
     */
    public int getMaxUtilityAction(){
        int action = Config.getRandomNumber(0,actionNum-1);

        double maxValue = qValues[action];
        int maxAction = action;
        for(int i = 0; i < qValues.length;i++){
            if(qValues[i] > maxValue){
                maxValue = qValues[i];
                maxAction = i;
            }
        }
        return maxAction;
    }

    private class Action{
        private int action;
        private double reward;

        public Action(int action,double reward){
            this.action = action;
            this.reward = reward;
        }

        public int getAction() {
            return action;
        }

        public void setAction(int action) {
            this.action = action;
        }

        public double getReward() {
            return reward;
        }

        public void setReward(double reward) {
            this.reward = reward;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Action)) return false;

            Action action1 = (Action) o;

            if (action != action1.action) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return action;
        }

        @Override
        public String toString() {
            return "Action{" +
                    "action=" + action +
                    ", reward=" + reward +
                    '}';
        }
    }

    PriorityBlockingQueue<Action> votes = new PriorityBlockingQueue<>(10, new Comparator<Action>() {
        @Override
        public int compare(Action o1, Action o2) {
            return o1.getReward() - o2.getReward() > 0 ? -1: 1;
        }
    });


    private int getBestActionForNeighbors(){
        votes.clear();
        Action action;
        double weightedReward = 0;
        for(int a = 0; a < actionNum;a++){
            weightedReward = 0;
            for(Node neighbor:getNeighbors()){
                weightedReward += ((Agent)neighbor).calWeightedReward(a);
            }

            action = new Action(a,weightedReward);
            votes.add(action);
        }
//        System.out.println(votes);
        return votes.peek().getAction();
    }

    private double calWeightedReward(int action){
        double reward = 0;
        for(int a = 0; a < actionNum;a++){
            reward += policy[a] * Config.rewards[action][a];
        }
        return reward;
    }

    public void training(){
        // 1:communication 与邻居同步信息
        communicateWithAllNeighbors();

        // 2:select action a with probability pi(s,a) with some exploration.
//        int action = selectActionWithExploration();
        int action = getBestActionForNeighbors();

        // 3:随机选择一个agent，交互
        int randomPartner = Config.getRandomNumber(0,getNeighborsSize()-1);
        Agent partner = (Agent) getNeighbors().get(randomPartner);

        // 4:更新Q
        int partnerAction = partner.selectActionWithExploration();
        double reward = Config.rewards[action][partnerAction];
        qValues[action] = (1 - learningRate) * qValues[action] + learningRate * reward;
        partner.qValues[partnerAction] = (1 - partner.learningRate) * partner.qValues[partnerAction] + partner.learningRate * reward;

        // 5:更新历史平均策略
        this.learningTimes++;
        partner.learningTimes++;
        for(int i = 0;i < actionNum;i++){
            averagePolicy[i] = averagePolicy[i] + (policy[i] - averagePolicy[i]) / learningTimes;
            partner.averagePolicy[i] = partner.averagePolicy[i] + (partner.policy[i] - partner.averagePolicy[i]) / partner.learningTimes;
        }

        // 6：根据Q and a 更新策略
        if(action == getMaxUtilityAction()){
            policy[action] = policy[action] + getClimbingRate();
        }else{
            policy[action] = policy[action] - (getClimbingRate() / (actionNum - 1));
        }

        if(partnerAction == partner.getMaxUtilityAction()){
            partner.policy[partnerAction] += partner.getClimbingRate();
        }else {
            partner.policy[partnerAction] += -(partner.getClimbingRate() / (partner.actionNum - 1));
        }

        // 7：更新 parameters
        updateParameters();
        partner.updateParameters();
    }


    public static void main(String args[]){
        double [] values = new double[]{0.5,0.1,0.2,0.1,0.1};
        Map<Integer,Integer> counts = new HashMap<>();
        int select;
        for(int i = 0; i < 1000; i++){
            select = Agent.selectActionWithMixedPolicy(values);
            counts.put(select,counts.get(select) == null ? 1:counts.get(select)+1);
        }

        System.out.println(counts);
    }

    public double[] getQValues(){
        return qValues;
    }
}