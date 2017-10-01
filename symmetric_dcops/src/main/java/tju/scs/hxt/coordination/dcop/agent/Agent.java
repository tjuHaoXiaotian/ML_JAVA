package tju.scs.hxt.coordination.dcop.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tju.scs.hxt.coordination.dcop.network.Node;

/**
 * Created by haoxiaotian on 2017/5/25 20:12.
 */
public class Agent extends Node {
    // 定义game 比较规整，简单；2 player,n-action game;
    protected final int actionNum; // 每个 state 下的 action num

    @JsonIgnore
    protected double learningRate; // 学习速率

    @JsonIgnore
    protected double exploreRate; // 探索率

    protected final int type; // 所属的网络种类

    public Agent(int id, int actionNum, double exploreRate, double learningRate, int type) {
        super(id);
        this.actionNum = actionNum;
        this.exploreRate = exploreRate;
        this.learningRate = learningRate;
        this.type = type;
    }


    public int getType() {
        return type;
    }

    public int getActionNum() {
        return actionNum;
    }

    public double getExploreRate() {
        return exploreRate;
    }
}
