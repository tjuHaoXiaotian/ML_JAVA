package tju.scs.hxt.coordination.agent;

import tju.scs.hxt.coordination.web.GlobalCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hxt on 17-4-7.
 */
public class StopThread extends Thread{
    private int type;

    public StopThread(int type) {
        super();
        this.type = type;
    }

    // 目前所有 agent 的最有action统计信息
    private Map<Integer,Integer> rowActionSelection = new HashMap<Integer, Integer>();
    private Map<Integer,Integer> columnActionSelection = new HashMap<Integer, Integer>();

    private int convergeTimes = 0;

    /**
     * 判断agent是否收敛：选择统一action的agent数量 > 98%
     * @return
     */
    private boolean isConverge(){
        rowActionSelection.clear();
        columnActionSelection.clear();
        int bestRow,bestColumn,maxRow = 0,maxColumn = 0;
        for(Agent agent:GlobalCache.getAgents(type)){
            bestRow = agent.getBestRowAction();
            bestColumn = agent.getBestColumnAction();
            rowActionSelection.put(bestRow,rowActionSelection.get(bestRow) == null?1:rowActionSelection.get(bestRow)+1);
            if(rowActionSelection.get(bestRow) > maxRow){
                maxRow = rowActionSelection.get(bestRow);
            }
            columnActionSelection.put(bestColumn,columnActionSelection.get(bestColumn) == null?1:columnActionSelection.get(bestColumn)+1);
            if(columnActionSelection.get(bestColumn) > maxColumn){
                maxColumn = columnActionSelection.get(bestColumn);
            }
        }
        return ((double)maxRow) / GlobalCache.getAgents(type).size() > 0.98 && ((double)maxColumn) / GlobalCache.getAgents(type).size() > 0.98;
    }

    @Override
    public void run(){
        // 当目标网络未收敛，即自己工作没有完成
        while (!GlobalCache.isConverge(type,false)){
            if(isConverge()){
                convergeTimes++;
                System.out.println("type:" + type + " converged " + convergeTimes + " times.");

                if(convergeTimes >= 3){
                    GlobalCache.setConverge(type);
                }
            }

            try {
                // 睡眠 5 s
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
