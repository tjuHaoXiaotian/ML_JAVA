package tju.scs.hxt.coordination.symmetric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by haoxiaotian on 2017/3/13 23:59.
 */
public class Config {
    public static final int contrast_experiment = 3;

    public static final int network_type = 6;

    // 是否打印log
    public static final boolean printLog = false;

    // 系统 agent 各个状态下： action 个数
    public static final int actionNum = 10;

    // 系统 agent 探索率
    public static final double exploreRate = 1;
    public static final double [][] deltaExploreRate = {
            {0.09,0.09,0.09},
            {0.09,0.09,0.09},
            {0.09,0.09,0.09},
            {0.09,0.09,0.09},
            {0.09,0.09,0.09},
            {0.09,0.09,0.09}
    };

    public static double deltaLearningRateRef;


    // 系统 agent 学习率
    public static final double learningRate = 1;
    public static final double deltaLearningRate = 0.0005;

    public static final double imitationParameter = 8.88;

    // 系统 reward 设定
    public static final double [][] rewards;

    // 预期收敛到的 action
    public static final int expectedAction;

    // a weighting factor defining the trade-off between updating using Q-values and maximum payoff information.
    public static final double weightFactorForFMQ = 0;

    public static final int resources = 1;
    public static final double communicationCost = 0.1;



    static {
        rewards = new double[actionNum][actionNum];
//        actionNum = 8;
        expectedAction = actionNum >>> 1;
        initRewards();
    }



    /**
     * 初始化系统设定的 rewards
     */
    private static void initRewards(){
        for(int j = 0; j < actionNum;j++){
            // row player
            for(int k = 0; k < actionNum;k++){
                // column player
//                if(j == k && j == expectedAction){  // actionNum / 2 为要选择的action
////                    rewards[j][k] = getRandomNumber(1,actionNum-1);   // 设置 state i 对应的元素reward为最大值 stateNum
//                    rewards[j][k] = actionNum;   // 设置 state i 对应的元素reward为最大值 stateNum
//                }else
                if (j == k){
                    rewards[j][k] = 1;
//                    rewards[j][k] =  getRandomNumber(1,actionNum-1);  // 设置为 1 ~ stateNum-1
                }else{
                    rewards[j][k] = -1;  // 非同一个action，reward为 -1
                }
            }
        }
    }



    /**
     * 产生随机数 取值 [begin,end]
     * except: numbers in excepts
     * @param begin
     * @param end
     * @return
     */
    public static int getRandomNumber(int begin,int end,List<Integer> excepts){
        int result = getRandomNumber(begin,end);
        while (excepts.contains(result)){
            result = getRandomNumber(begin,end);
        }
        return result;
    }

    /**
     * 产生随机数 取值 [begin,end]
     * @param begin
     * @param end
     * @return
     */
    public static int getRandomNumber(int begin,int end){
        int length = end+1 - begin;
        int delta = (int)Math.floor(Math.random() * length);
        return begin + delta;
    }

    public static void main(String [] args){
        Map<Integer,Integer> count = new HashMap<Integer, Integer>();
        int num;
        for(int i = 0; i < 100; i++){
            num = getRandomNumber(0,10);

            count.put(num,count.containsKey(num)?count.get(num)+1:1);
        }

//        for(Integer i:count.keySet()){
//            System.out.println(i + ":" + count.get(i));
//        }
        printRewardTable();

    }

    /**
     * 打印系统设定的 rewards
     */
    public static void printRewardTable(){
        // state i
        for(int j = 0; j < actionNum;j++){
            // row player
            for(int k = 0; k < actionNum;k++){
                if(j == k && k == expectedAction){
                    System.out.print(" ["+rewards[j][k] + "] ");
                }else{
                    System.out.print("  "+rewards[j][k] + "  ");
                }
            }
            System.out.println();
        }
    }

}
