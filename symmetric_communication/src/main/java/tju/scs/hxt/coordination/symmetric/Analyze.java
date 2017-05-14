package tju.scs.hxt.coordination.symmetric;

import tju.scs.hxt.coordination.symmetric.agent.Agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by haoxiaotian on 2017/5/4 17:43.
 */
public class Analyze {

    public static Map<String,Integer> times = new HashMap<>();

    public static int communicationTimes = 0;
    public static int connectionTimes = 0;

    public void printAnalysis(){
        System.out.println(Analyze.times);
        System.out.println("communicationTimes:" + Analyze.communicationTimes);
        System.out.println("connectionTimes:" + Analyze.connectionTimes);
    }
}
