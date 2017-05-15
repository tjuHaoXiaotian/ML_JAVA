package tju.scs.hxt.coordination.dcop;

import tju.scs.hxt.coordination.dcop.agent.Agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by haoxiaotian on 2017/5/4 17:43.
 */
public class Analyze {

    public static Map<String,Integer> communicationTimes = new HashMap<String, Integer>();
    public static Map<String,Integer> rounds = new HashMap<String, Integer>();

    static {
        for(int type = 0; type < Config.network_type;type++){
            for(int expId = 0; expId < Config.contrast_experiment;expId++){
                communicationTimes.put("network:"+type+"-"+expId,0);
                rounds.put("network:"+type+"-"+expId,0);
            }
        }
    }
    public void printAnalysis(){
        System.out.println("communication times:" + Analyze.communicationTimes);
        System.out.println("round times:" + Analyze.rounds);
    }
}
