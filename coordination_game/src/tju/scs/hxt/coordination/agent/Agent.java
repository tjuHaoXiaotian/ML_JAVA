package tju.scs.hxt.coordination.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tju.scs.hxt.coordination.Config;
import tju.scs.hxt.coordination.network.Node;
import tju.scs.hxt.coordination.q.QItem;
import tju.scs.hxt.coordination.queue.BoundedPriorityBlockingQueue;
import tju.scs.hxt.coordination.web.GlobalCache;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by haoxiaotian on 2017/3/13 23:53.
 */
public class Agent extends Node implements Runnable{
    // 2 个状态，代表自己是 row player or column player（0-row；1-column）
    // 为了应付那种 reward 非对称的 game
    private final int stateNum = 2;

    // 定义game 比较规整，简单；2 player,n-action game;
    private final int actionNum; // 每个 state 下的 action num

    @JsonIgnore
    private final QItem[][] qValues; // Q Table

    private double exploreRate; // 探索速率

    private double learningRate; // 学习速率

    private final int type; // 所属的网络种类

    // 请求队列
    private AbstractQueue<Request> requestQueue;

    // 通信锁：获取锁以建立连接(默认是非公平锁)
    private Lock connectionLock = new ReentrantLock();

    // 与不同agent的连接次数
    @JsonIgnore
    private int connectionTimes = 0;

    // 统计此agent与所有邻居agent的合作信息
    private Map<Integer,Statistics> cooperationStatistics = new HashMap<Integer, Statistics>();

    public Agent(int id,int actionNum, double exploreRate, double learningRate,int type) {
        super(id);
        this.actionNum = actionNum;
        this.exploreRate = exploreRate;
        this.learningRate = learningRate;
        this.qValues = new QItem[stateNum][actionNum];
        this.type = type;
        // 初始化 q tables
        initTables();

        // 初始化 action 选择策略
        initPolicy();

        // 初始化请求队列
//        initRequestQueue(true);
        initRequestQueue(false);
    }

    // 初始化 Q Table
    private void initTables(){
        for(int i = 0; i < this.stateNum; i++){
            for(int j = 0; j < this.actionNum; j++){
                qValues[i][j] = new QItem();
            }
        }
    }

    /**
     * 初始化消息队列
     * @param bounded 是否使用有界队列
     */
    private void initRequestQueue(boolean bounded){
        if(bounded){
            // 最多保留最新的10条消息
            requestQueue = new BoundedPriorityBlockingQueue<Request>(Config.requestQueueLength, new Comparator<Request>() {
                @Override
                public int compare(Request request1, Request request2) {
                    return (int)(request1.getPriority() - request2.getPriority());
                }
            },true);
        }
        else{
            // 保留最新的10条消息
            requestQueue = new PriorityBlockingQueue<Request>(Config.requestQueueLength, new Comparator<Request>() {
                @Override
                public int compare(Request request1, Request request2) {
                    return (int)(request2.getPriority() - request1.getPriority());
                }
            });  // 大顶堆，按优先级从大到小排序
        }
    }

    /**
     * 添加邻居agent发来的连接请求
     * @param request
     */
    public void addRequest(Request request){
        requestQueue.add(request);
    }

    /**
     * 向 agent 发起通信连接请求；
     * 1：使用 socket，高仿真模拟；
     * 2：直接共享内存模式模拟；
     * @param agent
     */
    private void sendConnectionRequest(Agent agent){
        // 默认策略，越新的位置越靠前，及优先考虑当前最新的连接请求。
        int rowAction = getBestRowAction(),columnAction = getBestColumnAction();
        agent.addRequest(new Request(this,agent,rowAction,columnAction,this.qValues[0][rowAction].getFmq(),this.qValues[1][columnAction].getFmq(),this.getCalPriority(),this.policy));
    }

    /**
     * 向所有邻居发起通信请求
     */
    public void sendConnectionRequestToNeighbors(boolean random){
        if(random){   // 1: randomly 发送请求
            for(Node neighbor:this.getNeighbors()){
                if(Math.random() > 0.5){
                    sendConnectionRequest((Agent)neighbor);
                }
            }
        }else{
            int resourceNum = 0,id;
            Set<Integer> excepts = new HashSet<>();
            while (resourceNum < RESOURCE_NUM && resourceNum < getNeighborsSize()){
                id = chooseTheMostFarAgent(excepts);
                sendConnectionRequest(getNeighborById(id));
                excepts.add(id);
                resourceNum++;
            }
        }

    }

    /**
     * 判断是否训练充足：是否达到既定轮数
     * @return
     */
    public boolean getEnoughTraining(){
        return connectionTimes >= Config.eachConnectionTimes;
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

        double maxPriority = 0,maxFMQValue = 0;
        int myBestAction;
        counts.clear();
        if(rowAction){
            myBestAction = getBestRowAction();
            maxPriority = this.getPriority();
            maxFMQValue = this.qValues[0][myBestAction].getFmq();

            for(Request request:requestQueue){
                if(request.getPriority() > maxPriority){
                    maxPriority = request.getPriority();
                }
                if(request.getFmqValueRow() > maxFMQValue){
                    maxFMQValue = request.getFmqValueRow();
                }
            }

            // 自己先投一票；
            counts.put(myBestAction,calWeightedProportion(this.getPriority() * 100 / maxPriority,this.qValues[0][myBestAction].getFmq() * 100 / maxFMQValue));
            for(Request request:requestQueue){
                counts.put(request.getRowAction(),counts.get(request.getRowAction()) == null ?
                        calWeightedProportion(request.getPriority() * 100 / maxPriority,request.getFmqValueRow()  * 100 / maxFMQValue):
                        counts.get(request.getRowAction()) + calWeightedProportion(request.getPriority() * 100 / maxPriority, request.getFmqValueRow() * 100 / maxFMQValue));
            }
        }else{
            myBestAction = getBestColumnAction();
            maxPriority = this.getPriority();
            maxFMQValue = this.qValues[1][myBestAction].getFmq();

            for(Request request:requestQueue){
                if(request.getPriority() > maxPriority){
                    maxPriority = request.getPriority();
                }
                if(request.getFmqValueColumn() > maxFMQValue){
                    maxFMQValue = request.getFmqValueColumn();
                }
            }

            // 自己先投一票；
            counts.put(myBestAction,calWeightedProportion(this.getPriority() * 100 / maxPriority,this.qValues[1][myBestAction].getFmq() * 100 / maxFMQValue));
            for(Request request:requestQueue){
                counts.put(request.getColumnAction(),counts.get(request.getColumnAction()) == null ?
                        calWeightedProportion(request.getPriority() * 100 / maxPriority,request.getFmqValueColumn()  * 100 / maxFMQValue):
                        counts.get(request.getColumnAction()) + calWeightedProportion(request.getPriority() * 100 / maxPriority,request.getFmqValueColumn()  * 100 / maxFMQValue));
            }
        }

        // 统计最大投票信息
        maxPriority = 0;
        int maxAction = 0;
        for(Map.Entry<Integer,Double> entry:counts.entrySet()){
            if(entry.getValue() > maxPriority){
                maxPriority = entry.getValue();
                maxAction = entry.getKey();
            }
        }
        return maxAction;
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

    /**
     * 更新学习率
     */
    private void updateLearningRate(){
        this.learningRate -= Config.deltaLearningRate;
        if(this.learningRate < 0.2){
            this.learningRate = 0.2;
        }
    }

    /**
     * 更新探索率
     */
    private void updateExploreRate(){
//        this.exploreRate -= Config.deltaExploreRate;
//        if(this.exploreRate < 0.1){
//            this.exploreRate = 0.5;
//        }
        this.exploreRate = 1 / Math.sqrt(connectionTimes == 0?1:connectionTimes);
    }


    public void randomTraining(){
        // 取出请求中的策略，同步到本地
        synchronizeSimilarity();

        int maxRowAction = countActionPriorityPair(true);
        int maxColumnAction = countActionPriorityPair(false);

//        int randomPartner = Config.getRandomNumber(0,getNeighborsSize()-1);
//        Agent partner = (Agent) getNeighbors().get(randomPartner);
        Agent partner = getNeighborById(chooseTheMostFarAgent(null));
        // TODO 1：  我是 row player，他是 column player ================================================================================================

        // TODO：投票的时候，加上过滤或者统计信息，根据maxFrequency——>接受度
        int rowAction = selectActionWithRecommended(maxRowAction); // 自己是 row player: 根据投票选择 action：
//        int columnAction = partner.selectActionWithRecommended(maxColumnAction); // 对方是 column player
        int columnAction = partner.selectAction(1); // 对方是 column player

        double reward = Config.rewards[rowAction][columnAction];

        // 记录统计信息
        if(this.cooperationStatistics.get(partner.getId()) == null){
            this.cooperationStatistics.put(partner.getId(),new Statistics(0,0));
        }

        if(partner.cooperationStatistics.get(this.getId()) == null){
            partner.cooperationStatistics.put(this.getId(),new Statistics(0,0));
        }

        // 记录统计信息
        this.cooperationStatistics.get(partner.getId()).increaseTotal();
        partner.cooperationStatistics.get(this.getId()).increaseTotal();

        if(reward > 0){
            this.cooperationStatistics.get(partner.getId()).increasePositive();
            partner.cooperationStatistics.get(this.getId()).increasePositive();
        }

        // 1: 更新各自 Q table
        qValues[0][rowAction].setqValue((1 - learningRate) *  qValues[0][rowAction].getqValue() + learningRate * reward);
        partner.qValues[1][columnAction].setqValue((1 - partner.learningRate) *  partner.qValues[1][columnAction].getqValue() + partner.learningRate * reward);

        // 2: 更新总（s,a）次数
        qValues[0][rowAction].increaseTotalTimes();  // 总次数
        partner.qValues[1][columnAction].increaseTotalTimes();

        // 3: 更新各自 maxReward under (s,a)
        if(reward > qValues[0][rowAction].getMaxQValue()) {
            qValues[0][rowAction].setMaxQValue(reward);  // maxReward
            qValues[0][rowAction].setMaxQTimes(1);
        }else if(reward == qValues[0][rowAction].getMaxQValue()){
            qValues[0][rowAction].increaseMaxTimes();
        }

        if(reward > partner.qValues[1][columnAction].getMaxQValue()) {
            partner.qValues[1][columnAction].setMaxQValue(reward);
            partner.qValues[1][columnAction].setMaxQTimes(1);
        }else if(reward == partner.qValues[1][columnAction].getMaxQValue()){
            partner.qValues[1][columnAction].increaseMaxTimes();
        }

        // 4: 更新 max frequency under (s,a)
        qValues[0][rowAction].setFrequency();
        partner.qValues[1][columnAction].setFrequency();

        // 5: 更新 FMQ
        qValues[0][rowAction].updateFMQ(Config.weightFactorForFMQ);  // 总次数
        partner.qValues[1][columnAction].updateFMQ(Config.weightFactorForFMQ);


        // TODO 2：  我是 column player，他是 row player ================================================================================================
        // TODO：投票的时候，加上过滤或者统计信息，根据maxFrequency——>接受度
        columnAction = selectActionWithRecommended(maxColumnAction); // 自己是 column player: 根据投票选择 action：

//        rowAction = partner.selectActionWithRecommended(maxRowAction); // 对方是 row player
        rowAction = partner.selectAction(0); // 对方是 row player
        reward = Config.rewards[rowAction][columnAction];

        // 记录统计信息
        this.cooperationStatistics.get(partner.getId()).increaseTotal();
        partner.cooperationStatistics.get(this.getId()).increaseTotal();

        if(reward > 0) {
            this.cooperationStatistics.get(partner.getId()).increasePositive();
            partner.cooperationStatistics.get(this.getId()).increasePositive();
        }

        // 1: 更新各自 Q table
        qValues[1][columnAction].setqValue((1 - learningRate) *  qValues[1][columnAction].getqValue() + learningRate * reward);
        partner.qValues[0][rowAction].setqValue((1 - partner.learningRate) *  partner.qValues[0][rowAction].getqValue() + partner.learningRate * reward);

        // 2: 更新总（s,a）次数
        qValues[1][columnAction].increaseTotalTimes();  // 总次数
        partner.qValues[0][rowAction].increaseTotalTimes();

        // 3: 更新各自 maxReward under (s,a)
        if(reward > qValues[1][columnAction].getMaxQValue()) {
            qValues[1][columnAction].setMaxQValue(reward);  // maxReward
            qValues[1][columnAction].setMaxQTimes(1);
        }else if(reward == qValues[1][columnAction].getMaxQValue()){
            qValues[1][columnAction].increaseMaxTimes();
        }

        if(reward > partner.qValues[0][rowAction].getMaxQValue()) {
            partner.qValues[0][rowAction].setMaxQValue(reward);
            partner.qValues[0][rowAction].setMaxQTimes(1);
        }else if(reward == partner.qValues[0][rowAction].getMaxQValue()){
            partner.qValues[0][rowAction].increaseMaxTimes();
        }

        // 4: 更新 max frequency under (s,a)
        qValues[1][columnAction].setFrequency();
        partner.qValues[0][rowAction].setFrequency();

        // 5: 更新 FMQ
        qValues[1][columnAction].updateFMQ(Config.weightFactorForFMQ);  // 总次数
        partner.qValues[0][rowAction].updateFMQ(Config.weightFactorForFMQ);


        // 更新通信连接次数
        connectionTimes++;
        partner.connectionTimes++;

        // 更新学习率
        updateLearningRate();
        partner.updateLearningRate();

        // 更新探索率
        updateExploreRate();
        partner.updateExploreRate();

        // 更新策略（action 选择概率）
        updatePolicy();
        partner.updatePolicy();

        // 更新各自附加权重
        this.updateLinkedPriority();
        partner.updateLinkedPriority();

        this.requestQueue.clear();
    }



    /**
     * 选择action
     * @param state
     * @return
     */
    private int selectAction(int state){
        // 先随机选一个，防重复选择同一个action
        int action = Config.getRandomNumber(0,actionNum-1);
        if(Math.random() < exploreRate){  // 随机探索
            return action;
        }
        double maxValue = qValues[state][action].getFmq();
        for(int i = 0; i < actionNum;i++){
            if(qValues[state][i].getFmq() > maxValue){
                maxValue = qValues[state][i].getFmq();
                action = i;
            }
        }
        return action;
    }

    /**
     * 取出当前最好的 row：action
     * @return
     */
    public int getBestRowAction(){
        int action = Config.getRandomNumber(0,actionNum-1);

        double maxValue = qValues[0][action].getFmq();
        int maxAction = action;
        for(int i = 0; i < qValues[0].length;i++){
            if(qValues[0][i].getFmq() > maxValue){
                maxValue = qValues[0][i].getFmq();
                maxAction = i;
            }
        }
        return maxAction;
    }

    /**
     * 取出当前最好的 column：action
     * @return
     */
    public int getBestColumnAction(){
        int action = Config.getRandomNumber(0,actionNum-1);

        double maxValue = qValues[1][action].getFmq();
        int maxAction = action;
        for(int i = 0; i < qValues[1].length;i++){
            if(qValues[1][i].getFmq() > maxValue){
                maxValue = qValues[1][i].getFmq();
                maxAction = i;
            }
        }
        return maxAction;
    }


//    private int selectAction(int state){
//        // 先随机选一个，防重复选择同一个action
//        if(Math.random() < exploreRate){  // 随机探索
//            return Config.getRandomNumber(0,actionNum-1);
//        }
//        // 从第 0 个开始遍历
//        int action = 0;
//        double maxValue = qValues[state][action].getFmq();
//        for(int i = 1; i < actionNum;i++){
//            if(qValues[state][i].getFmq() > maxValue){
//                maxValue = qValues[state][i].getFmq();
//                action = i;
//            }
//        }
//        return action;
//    }

    /**
     * 使用推荐的action进行 e-探索
     * @param actionRecommended
     * @return
     */
    private int selectActionWithRecommended(int actionRecommended){
        if(Math.random() < exploreRate){  // 随机探索
            return Config.getRandomNumber(0,actionNum-1);
        }else{
            return actionRecommended;
        }
    }


    // 控制各个 agent 开始及结束的锁
    @JsonIgnore
    private CountDownLatch startGate;

    @JsonIgnore
    private CountDownLatch endGate;

    public CountDownLatch getStartGate() {
        return startGate;
    }

    public void setStartGate(CountDownLatch startGate) {
        this.startGate = startGate;
    }

    public CountDownLatch getEndGate() {
        return endGate;
    }

    public void setEndGate(CountDownLatch endGate) {
        this.endGate = endGate;
    }

    @Override
    public void run() {
        try {
            // 当前线程阻塞在起始处，确保当其他线程都启动后，再共同继续
            startGate.await();
            try{
                training();
            }finally {
                endGate.countDown();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * agent 训练
     */
    private void training() throws InterruptedException {
        Request request = null;
        Agent source = null;
        double reward = 0;
        int rowAction,columnAction,learningTimes = 0;
        long fixedDelay = 1;
        long randMod = 20;
        Random random = new Random();
        // 投票选择（请求队列中）最优 action
        int maxRowAction = 0,maxColumnAction = 0;
        boolean checkNext = false;
        while (!GlobalCache.isConverge(type,false)){
            if(Config.printLog)
                System.out.println("agent"+getId() + " 没有收敛，继续训练...");
            if(connectionLock.tryLock()){ // 拿到了自己的锁，进而继续拿对方的通信锁。保证只有一个线程能够访问
                try{
                    // 1: 先从队列中，查看那些连接请求
                    if((request = requestQueue.poll()) == null){  // 当前连接队列中还没有连接请求
                        if(Config.printLog) {
                            System.out.println("agent" + getId()+ " 请求队列为空，其它 agent 还未与自己发起通信....");
                            System.out.println("agent"+getId() + " send request to neighbors..........................................");
                        }

                        // 向邻居发起连接请求
                        sendConnectionRequestToNeighbors(false);
                        // 进入 finally: 释放自己的连接锁。
                    }else{ // 队列中有请求，则审查是否可以建立连接
                        // TODO:加权投票，统计请求队列中的 action:priority 对儿
                        maxRowAction = countActionPriorityPair(true);
                        maxColumnAction = countActionPriorityPair(false);
                        while (request != null){
                            source = request.getSource();
                            if(source.connectionLock.tryLock()){ //TODO 可能产生死锁:出现了环
                                // 获取到了锁
                                // TODO: learning...

                                // 记录统计信息
                                if(this.cooperationStatistics.get(source.getId()) == null){
                                    this.cooperationStatistics.put(source.getId(),new Statistics(0,0));
                                }

                                if(source.cooperationStatistics.get(this.getId()) == null){
                                    source.cooperationStatistics.put(this.getId(),new Statistics(0,0));
                                }

                                try{
                                    learningTimes = 0;
                                    while (learningTimes < Config.learningTimesAfterConnected){
                                        //我已经在当前连接请求中选择出了，连接对象，于是清空当前队列
                                        // TODO: 修改请求队列的策略（保证当前是固定大小），

                                        // TODO 1：  我是 row player，他是 column player ================================================================================================
//                                        rowAction = this.selectAction(0); // 自己是 row player
                                        // TODO：投票的时候，加上过滤或者统计信息，根据maxFrequency——>接受度
                                        rowAction = selectActionWithRecommended(maxRowAction); // 自己是 row player: 根据投票选择 action：
//                                        rowAction = maxRowAction; // 自己是 row player: 根据投票选择 action：
                                        // TODO: 这里应该是 伊普西龙探索！！！！！！！！！！！！！！
                                        columnAction = source.selectAction(1); // 对方是 column player
//                                        columnAction = source.selectActionWithRecommended(maxColumnAction); // 对方是 column player
                                        reward = Config.rewards[rowAction][columnAction];

                                        // 记录统计信息
                                        this.cooperationStatistics.get(source.getId()).increaseTotal();
                                        source.cooperationStatistics.get(this.getId()).increaseTotal();

                                        if(reward > 0){
                                            this.cooperationStatistics.get(source.getId()).increasePositive();
                                            source.cooperationStatistics.get(this.getId()).increasePositive();

//                                            // 更新各自权重
////                                            this.setPriority(this.getPriority() + source.getPriority() * this.cooperationStatistics.get(source.getId()).getPositiveFrequency());
//                                            this.setPriority(this.getPriority() + 1 * this.cooperationStatistics.get(source.getId()).getPositiveFrequency());
////                                            source.setPriority(source.getPriority() + this.getPriority() * source.cooperationStatistics.get(this.getId()).getPositiveFrequency());
//                                            source.setPriority(source.getPriority() + 1 * source.cooperationStatistics.get(this.getId()).getPositiveFrequency());
                                        }else{
                                            // 更新各自权重
//                                            this.setPriority(this.getPriority() - source.getPriority() * this.cooperationStatistics.get(source.getId()).getNegativeFrequency());
//                                            source.setPriority(source.getPriority() - this.getPriority() * source.cooperationStatistics.get(this.getId()).getNegativeFrequency());
                                        }

//                                        System.out.println(this.getPriority());

                                        // 1: 更新各自 Q table
                                        qValues[0][rowAction].setqValue((1 - learningRate) *  qValues[0][rowAction].getqValue() + learningRate * reward);
                                        source.qValues[1][columnAction].setqValue((1 - source.learningRate) *  source.qValues[1][columnAction].getqValue() + source.learningRate * reward);

                                        // 2: 更新总（s,a）次数
                                        qValues[0][rowAction].increaseTotalTimes();  // 总次数
                                        source.qValues[1][columnAction].increaseTotalTimes();

                                        // 3: 更新各自 maxReward under (s,a)
                                        if(reward > qValues[0][rowAction].getMaxQValue()) {
                                            qValues[0][rowAction].setMaxQValue(reward);  // maxReward
                                            qValues[0][rowAction].setMaxQTimes(1);
                                        }else if(reward == qValues[0][rowAction].getMaxQValue()){
                                            qValues[0][rowAction].increaseMaxTimes();
                                        }

                                        if(reward > source.qValues[1][columnAction].getMaxQValue()) {
                                            source.qValues[1][columnAction].setMaxQValue(reward);
                                            source.qValues[1][columnAction].setMaxQTimes(1);
                                        }else if(reward == source.qValues[1][columnAction].getMaxQValue()){
                                            source.qValues[1][columnAction].increaseMaxTimes();
                                        }

                                        // 4: 更新 max frequency under (s,a)
                                        qValues[0][rowAction].setFrequency();
                                        source.qValues[1][columnAction].setFrequency();

                                        // 5: 更新 FMQ
                                        qValues[0][rowAction].updateFMQ(Config.weightFactorForFMQ);  // 总次数
                                        source.qValues[1][columnAction].updateFMQ(Config.weightFactorForFMQ);


                                        // TODO 2：  我是 column player，他是 row player ================================================================================================
                                        // TODO：投票的时候，加上过滤或者统计信息，根据maxFrequency——>接受度
                                        columnAction = selectActionWithRecommended(maxColumnAction); // 自己是 column player: 根据投票选择 action：
//                                        columnAction = maxColumnAction; // 自己是 column player: 根据投票选择 action：
                                        // TODO: 这里应该是 伊普西龙探索！！！！！！！！！！！！！！
                                        rowAction = source.selectAction(0); // 对方是 row player
//                                        rowAction = source.selectActionWithRecommended(maxRowAction); // 对方是 row player
                                        reward = Config.rewards[rowAction][columnAction];

                                        // 记录统计信息
                                        this.cooperationStatistics.get(source.getId()).increaseTotal();
                                        source.cooperationStatistics.get(this.getId()).increaseTotal();

                                        if(reward > 0){
                                            this.cooperationStatistics.get(source.getId()).increasePositive();
                                            source.cooperationStatistics.get(this.getId()).increasePositive();

//                                            // 更新各自权重
////                                            this.setPriority(this.getPriority() + source.getPriority() * this.cooperationStatistics.get(source.getId()).getPositiveFrequency());
//                                            this.setPriority(this.getPriority() + 1 * this.cooperationStatistics.get(source.getId()).getPositiveFrequency());
////                                            source.setPriority(source.getPriority() + this.getPriority() * source.cooperationStatistics.get(this.getId()).getPositiveFrequency());
//                                            source.setPriority(source.getPriority() + 1 * source.cooperationStatistics.get(this.getId()).getPositiveFrequency());
                                        }else{
                                            // 更新各自权重
//                                            this.setPriority(this.getPriority() - source.getPriority() * this.cooperationStatistics.get(source.getId()).getNegativeFrequency());
//                                            source.setPriority(source.getPriority() - this.getPriority() * source.cooperationStatistics.get(this.getId()).getNegativeFrequency());
                                        }

                                        // 1: 更新各自 Q table
                                        qValues[1][columnAction].setqValue((1 - learningRate) *  qValues[1][columnAction].getqValue() + learningRate * reward);
                                        source.qValues[0][rowAction].setqValue((1 - source.learningRate) *  source.qValues[0][rowAction].getqValue() + source.learningRate * reward);

                                        // 2: 更新总（s,a）次数
                                        qValues[1][columnAction].increaseTotalTimes();  // 总次数
                                        source.qValues[0][rowAction].increaseTotalTimes();

                                        // 3: 更新各自 maxReward under (s,a)
                                        if(reward > qValues[1][columnAction].getMaxQValue()) {
                                            qValues[1][columnAction].setMaxQValue(reward);  // maxReward
                                            qValues[1][columnAction].setMaxQTimes(1);
                                        }else if(reward == qValues[1][columnAction].getMaxQValue()){
                                            qValues[1][columnAction].increaseMaxTimes();
                                        }

                                        if(reward > source.qValues[0][rowAction].getMaxQValue()) {
                                            source.qValues[0][rowAction].setMaxQValue(reward);
                                            source.qValues[0][rowAction].setMaxQTimes(1);
                                        }else if(reward == source.qValues[0][rowAction].getMaxQValue()){
                                            source.qValues[0][rowAction].increaseMaxTimes();
                                        }

                                        // 4: 更新 max frequency under (s,a)
                                        qValues[1][columnAction].setFrequency();
                                        source.qValues[0][rowAction].setFrequency();

                                        // 5: 更新 FMQ
                                        qValues[1][columnAction].updateFMQ(Config.weightFactorForFMQ);  // 总次数
                                        source.qValues[0][rowAction].updateFMQ(Config.weightFactorForFMQ);

                                        if(Config.printLog) {
                                            System.out.println("agent"+getId() + " update q table ....................................");
                                            System.out.println("agent"+source.getId() + " update q table ....................................");
                                        }

                                        learningTimes++;
                                    }

                                    if(Config.printLog)
                                        System.out.println("agent"+getId() + " 一次通信完成,and go on checking next...");

                                    // 更新通信连接次数
                                    connectionTimes++;
                                    source.connectionTimes++;

                                    // 更新学习率
                                    updateLearningRate();

                                    // 更新探索率
                                    updateExploreRate();

                                    // 更新各自的权重
//                                    this(source.getPriority());
//                                    source.currentWindow.updateRequestPriority(this.getPriority());

                                    // 更新各自附加权重
                                    this.updateLinkedPriority();
                                    source.updateLinkedPriority();

                                    // 发放更新通知：request
                                    sendConnectionRequestToNeighbors(false);

                                    request = requestQueue.poll();
                                    // TODO: learning finished...
                                }finally {
                                    // 释放锁
                                    source.connectionLock.unlock();
                                }
                            }else{  // 未获得锁
                                // TODO: 未能获取当前对象的锁，查看请求中,下一个对象
                                if(Config.printLog)
                                    System.out.println("agent"+getId() + "未能建立通信, check next request...");
                                request = requestQueue.poll();
                            }
                        }
                        // 已查看当前队列中所有请求
                        if(Config.printLog) {
                            System.out.println("agent" + getId()+ " 请求队列为空....");
                            System.out.println("agent"+getId() + " send request to neighbors..........................................");
                        }

                        // 自己发起连接请求；
                        sendConnectionRequestToNeighbors(false);
                    }

                    if(Config.printLog)
                        System.out.println("agent"+getId() + "已发出请求，正等待其它 agent 应答..........................................");
                    TimeUnit.NANOSECONDS.sleep(fixedDelay + random.nextLong() % randMod);
                }finally {
                    connectionLock.unlock();
                }
            }else{
                if(Config.printLog)
                    System.out.println("agent"+getId() + "自己的锁被占用；it is in communication...");
                TimeUnit.NANOSECONDS.sleep(fixedDelay + random.nextLong() % randMod);
            }
        }

    }

    public QItem[][] getqValues() {
        return qValues;
    }

    public int getConnectionTimes(){
        return connectionTimes;
    }


    /**
     * 打印 Q Table
     */
    public void printQTable(){
        if(Config.printLog) {
            System.out.println("==================================== agent"+getId()+" Q TABLE=========================================");
            System.out.println("agent"+getId()+" update q table " + connectionTimes +" times.");
            for(int i = 0; i < stateNum; i++){
                for(int j = 0; j < actionNum; j++){
                    if(j == Config.expectedAction){
                        System.out.print(" ["+qValues[i][j].getFmq() + "] ");
                    }else{
                        System.out.print("  "+qValues[i][j].getFmq() + "  ");
                    }

                }
                System.out.println();
            }
            System.out.println("   ===========================================Q TABLE===========================================");
            System.out.println();
        }
    }


    private final int RESOURCE_NUM = 2;

    // 策略，即选择某一个action的概率
    private double [][] policy;

    private double [][]defaultPolicy;

    // 统计neighbor的策略，以便衡量与某个neighbor的相似度（neighbor的策略由通信是负责传递）
    private Map<Integer,NeighborSimilar> similarityOfNeighbors = new HashMap<>();

    private void synchronizeSimilarity() {
        NeighborSimilar neighborSimilar;
        for(Request request:requestQueue){
            neighborSimilar = similarityOfNeighbors.get(request.getSource().getId());
            similarityOfNeighbors.put(request.getSource().getId(),
                    neighborSimilar == null ? new NeighborSimilar(request.getSource().getId(),request.getPolicy())
                            :neighborSimilar.updatePolicy(request.getPolicy()));
        }
    }

    /**
     * 策略，即选择某一个action的概率
     * 初始化为 1/n
     */
    private void initPolicy(){
        policy = new double[stateNum][actionNum];
        defaultPolicy = new double[stateNum][actionNum];
        for(int i = 0; i < stateNum;i++){
            for(int j = 0; j < actionNum;j++){
                policy[i][j] = ((double)1)/actionNum;
            }
        }

        for(int i = 0; i < stateNum;i++){
            for(int j = 0; j < actionNum;j++){
                defaultPolicy[i][j] = ((double)1)/actionNum;
            }
        }
    }

    /**
     * agent 更新自己的策略(选择不同action的概率)
     */
    private void updatePolicy(){
        int bestRowAction = getBestRowAction();
        int bestColumnAction = getBestColumnAction();
        // 更新 row action
        for(int i = 0; i < actionNum;i++){
            policy[0][i] = exploreRate/actionNum;
        }
        policy[0][bestRowAction] += (1-exploreRate);
        // 更新 column action
        for(int i = 0; i < actionNum;i++){
            policy[1][i] = exploreRate/actionNum;
        }
        policy[1][bestColumnAction] += (1-exploreRate);
    }

    private class NeighborSimilar{
        private int id;

        private double [][] policy;

        public NeighborSimilar(int id,double[][] policy) {
            this.id = id;
            this.policy = policy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NeighborSimilar)) return false;

            NeighborSimilar that = (NeighborSimilar) o;

            if (id != that.id) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id;
        }

        public NeighborSimilar updatePolicy(double [][]policy){
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
        if(similarityOfNeighbors.get(id) == null){
            return (cosSimilar(this.policy[0],this.defaultPolicy[0]) + cosSimilar(this.policy[1],this.defaultPolicy[1])) / 2;
        }else{
            double [][] policy = similarityOfNeighbors.get(id).policy;
            return (cosSimilar(this.policy[0],policy[0]) + cosSimilar(this.policy[1],policy[1])) / 2;
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
    private int roulette(int[] ids,int[] values){
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

    private int sum(int[] values){
        int sum = 0;
        for(double ele:values){
            sum += ele;
        }
        return sum;
    }


}