package tju.scs.hxt.coordination.dcop;

import tju.scs.hxt.coordination.dcop.agent.Agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by haoxiaotian on 2017/5/4 17:43.
 */
public class Analyze {

    public static Map<Integer,Map<Integer,Integer>> communicationTimes = new HashMap<Integer, Map<Integer, Integer>>();
    public static Map<Integer,Map<Integer,Integer>> rounds = new HashMap<Integer, Map<Integer, Integer>>();

    static {
        init();
    }

    public static void init(){
        for(int type = 0; type < Config.network_type;type++){
            init(type);
        }
    }

    public static void init(int netType){
        communicationTimes.put(netType,new HashMap<Integer, Integer>());
        rounds.put(netType,new HashMap<Integer, Integer>());
        for(int expId = 0; expId < Config.contrast_experiment;expId++){
            communicationTimes.get(netType).put(expId,0);
            rounds.get(netType).put(expId,0);
        }
    }

    public void printAnalysis(){
        System.out.println("communication times:" + Analyze.communicationTimes);
        System.out.println("round times:" + Analyze.rounds);
    }

    public static Map<Integer,Integer> getCommunications(int netType){
        return communicationTimes.get(netType);
    }

    public static void incCommunicationTimes(int netType,int expId){
        int old = communicationTimes.get(netType).get(expId);
        communicationTimes.get(netType).put(expId,old+1);
    }

    public static void incRoundTimes(int netType,int expId,int round){
        rounds.get(netType).put(expId,round);
    }
}
