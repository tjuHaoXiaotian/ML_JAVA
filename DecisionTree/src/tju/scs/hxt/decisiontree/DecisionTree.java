package tju.scs.hxt.decisiontree;

import tju.scs.hxt.data.Data;
import tju.scs.hxt.data.Property;
import tju.scs.hxt.node.Logical;
import tju.scs.hxt.node.Node;
import tju.scs.hxt.util.MathUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * 改进的地方：
 * 1：group的信息熵不用每次都计算，可以缓存起来
 * Created by haoxiaotian on 2017/3/1 13:57.
 */
public class DecisionTree {

    // 每一棵决策树，对应一个 result（返回决策树的根）
    private TreeNode result;
    private int nodeId;
    private int type; // 0,1,2

    // 决策树可选属性集合
    private HashSet<Property> properties;
    private String label;

    public DecisionTree(){
        this.result = new TreeNode();
        this.nodeId = 0;
    }

    public TreeNode trainForResult(){
        // 1：从文件中读取训练数据
        List<Data> datas = readData("data2.csv");

        // 2：初始化属性类别及label
        initProperties();

        // 3：设置 action 选择策略
        setType(2);

        // 3：训练决策树
        treeGenerate(datas,this.properties,this.result,1);

        // 4：返回训练结果
        return this.result;
    }


    public double verification(){
        // 1：从文件中读入验证集数据
        List<Data> datas = readData("data3.csv");

        // 2:计算决策树正确率
        return calCorrectRate(datas);
    }

    private double calCorrectRate(List<Data> datas){
        int correctCount = 0;
        TreeNode current = this.result;
        Object value = null;
        for(Data data:datas){
            while (!current.isLeaf()){
                value = data.getProperty(current.getKey());
                for(Node node:current.getChildren()){
                    if(((TreeNode)node).getBranchValue().equals(value)){  // 沿着此分支继续
                        current = (TreeNode)node;
                        break;
                    }
                }
            }
            System.out.println(data + " is " + current.getClassifyResult());
            if(current.getClassifyResult().equals(data.getProperty(label))){
                correctCount++;
            }
            current = this.result;
        }

        return ((double)correctCount * 100 / datas.size());
    }

    private List<Data> readData(String s) {

        List<Data> datas = new ArrayList<Data>();
        try {
            URL url = this.getClass().getClassLoader().getResource(s);
            System.out.println(url.getPath());
            InputStream inputStream = null;
            BufferedReader bufferedReader = null;
            try {
                inputStream = url.openConnection().getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                boolean firstLine = true;
                String [] inputs = null,keys = null;
                Data data = null;
                while ((line = bufferedReader.readLine()) != null){
                    System.out.println(line);
                    if(firstLine){
                        keys = line.split(",");
                        firstLine = false;
                    }else{
                        inputs = line.split(",");
                        data = new Data();
                        for(int i = 0; i < keys.length;i++){
//                            if(i == keys.length - 1){
//                                data.addProperty(keys[i], inputs[i].equals("是"));
//                            }else{
//                                data.addProperty(keys[i],inputs[i]);
//                            }
                            if(i == 0){
                                data.setId(Integer.valueOf(inputs[i]));
                            }else{
                                data.addProperty(keys[i],inputs[i]);
                            }
                        }
                        datas.add(data);
                    }
                }

                System.out.println(datas);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if(bufferedReader != null){
                    bufferedReader.close();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return datas;
    }

    private void initProperties(){
        this.label = "好瓜";
        this.properties = new HashSet<Property>();
        Property p1 = new Property("色泽",new HashSet<Object>(Arrays.asList("青绿","乌黑","浅白")));
        Property p2 = new Property("根蒂",new HashSet<Object>(Arrays.asList("蜷缩","稍蜷","硬挺")));
        Property p3 = new Property("敲声",new HashSet<Object>(Arrays.asList("浊响","沉闷","清脆")));
        Property p4 = new Property("纹理",new HashSet<Object>(Arrays.asList("清晰","稍糊","模糊")));
        Property p5 = new Property("脐部",new HashSet<Object>(Arrays.asList("凹陷","稍凹","平坦")));
        Property p6 = new Property("触感",new HashSet<Object>(Arrays.asList("硬滑","软粘")));
        Property label = new Property(this.label,new HashSet<Object>(Arrays.asList("是","否")));

        this.properties.add(p1);
        this.properties.add(p2);
        this.properties.add(p3);
        this.properties.add(p4);
        this.properties.add(p5);
        this.properties.add(p6);
        this.properties.add(label);
    }

    /**
     * 生成决策树
     * @param datas  数据集
     * @param actions  可选的动作集合
     * @param node   parent 节点（也可能是以叶节点收尾）
     * @param height  （节点的深度）
     */
    private void treeGenerate(List<Data> datas,Set<Property> actions,TreeNode node,int height){

        // 1：如果当前所有数据都已经是属于同一个类了，则无需再继续分类，将节点标记为该类
        SameResult sameLabelResult = withSameLabel(datas);
        if(sameLabelResult.getResult()){
            node.setLeaf(true);
            node.setClassifyResult(sameLabelResult.getType());
            node.setHeight(height);
            node.setId(nodeId++);
            return;//TODO 产生叶节点
        }

        // 2：如果当前可选的action集合为空，或者所有数据在action集合上取值相同，则无需再继续分类，将节点标记为出现最多的分类
        if(actions.isEmpty() || withSameProperties(datas,actions)){
            node.setLeaf(true);
            node.setClassifyResult(sameLabelResult.getType());
            node.setHeight(height);
            node.setId(nodeId++);
            return;//TODO 产生叶节点
        }

        Property action = selectBestPartitionProperty(datas,actions,type);
        Set<Property> subActions = getSubActions(actions,action);

        //TODO 产生分支节点
        node.setLeaf(false);  // node 是分支节点
        node.setKey(action.getKey());  // 设置node判断key
        node.setLogic(Logical.EQUAL);  // 设置node 判断逻辑为 ==
        node.setHeight(height);
        node.setId(nodeId++);
        for(Object branch:action.getValues()){  // 对 property 的每一个取值
            // 生成一个分支
            TreeNode child = new TreeNode();
            child.setBranchValue(branch);  // 设置父节点 key 对应 value
            node.addChild(child);  // node 添加分支节点
            child.setParent(node.getId());

            // 找出 取值为 branch 的 样本子集
            List<Data> subData = new ArrayList<Data>();
            for(Data data:datas){
                if(data.getProperty(action.getKey()).equals(branch)){
                    subData.add(data);
                }
            }

            // 3： 如果样本子集为空，标记为叶节点，类别标记为 datas 中样本最多的类
            if(subData.isEmpty()){
                child.setLeaf(true);
                child.setClassifyResult(sameLabelResult.getType());
                child.setHeight(height+1);
                child.setId(nodeId++);

                //TODO 产生叶节点
            }else{
                treeGenerate(subData, subActions,child,height+1);
            }
        }

    }

    /**
     * 选择最优划分属性
     * @param datas
     * @param actions
     * @param type 0：信息熵增益，1：信息熵增益与增益率，2：基尼系数
     * @return
     */
    private Property selectBestPartitionProperty(List<Data> datas, Set<Property> actions,int type) {
        switch (type){
            case 0:  // 信息熵增益
                return selectByInformationGain(datas,actions);
            case 1:  // 启发式（根据信息熵增益与信息熵增益率的折中）
                return selectByInformationGainAndRatio(datas,actions);
            case 2:  // 基尼指数
                return selectByGini(datas,actions);
            default: // 信息熵增益
                return selectByInformationGain(datas,actions);
        }
    }

    /**
     * 根据信息熵增益选择最优属性
     * @param datas
     * @param actions
     * @return
     */
    private Property selectByInformationGain(List<Data> datas, Set<Property> actions){
        System.out.println("new branch:-----------------------------------------------");
        double entD = getEntropy(datas);

        double maxGain = 0,gain = 0,sumSubEntropy = 0;
        Property maxProperty = null;
        // 在 actions 中选出信息熵增益最大的 action
        for(Property action:actions){
            if(!action.getKey().equals(label)){
                sumSubEntropy = 0;
                List<ArrayList<Data>> groups = getSubGroups(datas,action);
                for(ArrayList<Data> group:groups){
                    if(group.size() > 0){
                        sumSubEntropy += getEntropy(group) * group.size() / datas.size();
                    }
                }
                gain = entD - sumSubEntropy;
                System.out.println(action.getKey() + "信息熵增益: " + gain);
                if(maxGain < gain){
                    maxGain = gain;
                    maxProperty = action;
                }
            }
        }
        System.out.println("select :" + maxProperty.getKey() + "\n");
        return maxProperty;
    }

    /**
     * 启发式（根据信息熵增益与信息熵增益率的折中）
     * 先从候选划分属性中，找出信息增益高于平均水平的属性，再从中选择增益率最高的
     * @param datas
     * @param actions
     * @return
     */
    private Property selectByInformationGainAndRatio(List<Data> datas,Set<Property> actions){
        return selectByInformationGainRatio(datas,selectBetterActions(datas,actions));
    }


    /**
     * 根据基尼指数
     * 基尼指数越小，纯度越高
     * @param datas
     * @param actions
     * @return
     */
    private Property selectByGini(List<Data> datas,Set<Property> actions){
        System.out.println("new branch:-----------------------------------------------");

        double minGini = Double.MAX_VALUE,sumGini = 0;
        Property maxProperty = null;
        // 在 actions 中选出信息熵增益最大的 action
        for(Property action:actions){
            if(!action.getKey().equals(label)){
                sumGini = 0;
                List<ArrayList<Data>> groups = getSubGroups(datas,action);
                for(ArrayList<Data> group:groups){
                    if(group.size() > 0) {
                        sumGini += getGini(group) * group.size() / datas.size();
                    }
                }

                System.out.println(action.getKey() + "基尼系数: "+sumGini);
                if(minGini > sumGini){
                    minGini = sumGini;
                    maxProperty = action;
                }
            }
        }
        System.out.println("select :" + maxProperty.getKey() + ",基尼系数："+minGini + "\n");
        return maxProperty;
    }


    /**
     * 计算信息增益率在平均值以上的 actions
     * @param datas
     * @param actions
     * @return
     */
    private Set<Property> selectBetterActions(List<Data> datas, Set<Property> actions){
        Set<Property> betterActions = new HashSet<Property>();
        double entD = getEntropy(datas);
        // 存储计算结果： action ——> gain
        Map<Property,Double> calResult = new HashMap<Property, Double>(actions.size() - 1);
        double gain = 0,sumSubEntropy = 0,totalGain = 0,meanGain = 0;
        // 在 actions 中选出信息熵增益最大的 action
        for(Property action:actions){
            if(!action.getKey().equals(label)){
                sumSubEntropy = 0;
                List<ArrayList<Data>> groups = getSubGroups(datas,action);
                for(ArrayList<Data> group:groups){
                    if(group.size() > 0) {  // group 不为空
                        sumSubEntropy += getEntropy(group) * group.size() / datas.size();
                    }
                }
                gain = entD - sumSubEntropy;
                calResult.put(action,gain);
                totalGain += gain;
            }
        }
        meanGain = totalGain / calResult.size();

        for(Property key:calResult.keySet()){
            if(calResult.get(key) >= meanGain){
                betterActions.add(key);
            }
        }

        return betterActions;
    }

    /**
     * 选择信息增益率最高的action
     * @param datas
     * @param actions
     * @return
     */
    private Property selectByInformationGainRatio(List<Data> datas,Set<Property> actions){
        System.out.println("new branch:-----------------------------------------------");
        double entD = getEntropy(datas);

        double maxGainRatio = Double.MIN_VALUE,gain = 0,sumSubEntropy = 0,IV = 0,gain_ratio = 0,ratio = 0;
        Property maxProperty = null;
        // 在 action 中选择增益率最高的 action
        for(Property action:actions){
            if(!action.getKey().equals(label)){
                sumSubEntropy = 0;
                IV = 0;

                List<ArrayList<Data>> groups = getSubGroups(datas,action);
                for(ArrayList<Data> group:groups){
                    if(group.size() > 0) {  // group 不为空
                        sumSubEntropy += getEntropy(group) * group.size() / datas.size();
                        ratio = (double) group.size() / datas.size();
                        IV -= (ratio) * MathUtil.log(ratio, 2);
                    }
                }

                gain = entD - sumSubEntropy;
                gain_ratio = gain / IV;
                System.out.println(gain_ratio);
                if(maxGainRatio < gain_ratio){
                    maxGainRatio = gain_ratio;
                    maxProperty = action;
                }
            }
        }
        System.out.println("select :" + maxProperty.getKey() + "\n");
        return maxProperty;
    }

    /**
     * 计算某个数据集的基尼系数(衡量一个数据集的纯度)
     * @param datas
     * @return
     */
    private double getGini(List<Data> datas){
        double sumProbability = 0;
        // 统计标签列，各个取值有几种，并且计算每一种取值的个数
        HashMap<Object,Integer> labelsCount = new HashMap<Object, Integer>();
        for(Data data:datas){
            Object type = data.getProperty(label);
            labelsCount.put(type, labelsCount.get(type) == null ? 1 : labelsCount.get(type)+1);
        }

        for(Object key:labelsCount.keySet()){
            sumProbability += Math.pow((double) labelsCount.get(key) / datas.size(), 2);
        }
        return 1 - sumProbability;
    }

    /**
     * 计算某个数据集的信息熵(衡量一个数据集的纯度)
     * @param datas
     * @return
     */
    private double getEntropy(List<Data> datas){
        int total = datas.size();
        double entD = 0;

        // 统计标签列，各个取值有几种，并且计算每一种取值的个数
        HashMap<Object,Integer> labelsCount = new HashMap<Object, Integer>();
        for(Data data:datas){
            Object type = data.getProperty(label);
            labelsCount.put(type, labelsCount.get(type) == null ? 1 : labelsCount.get(type)+1);
        }

        for(Object key:labelsCount.keySet()){
            entD -= (((double)labelsCount.get(key)) / total) * MathUtil.log(((double)labelsCount.get(key)) / total,2);
        }
        System.out.println(datas + "信息熵" + entD);
        return entD;
    }

    /**
     * 根据某个属性（不同取值）划分数据集
     * @param datas
     * @param action
     * @return
     */
    private List<ArrayList<Data>> getSubGroups(List<Data> datas, Property action) {
        List<ArrayList<Data>> groups = new ArrayList<ArrayList<Data>>();
        ArrayList<Data> subData;
        for(Object branch:action.getValues()) {  // 对 property 的每一个取值

            // 找出 取值为 branch 的 样本子集
            subData = new ArrayList<Data>();
            for (Data data : datas) {
                if (data.getProperty(action.getKey()).equals(branch)) {
                    subData.add(data);
                }
            }
            groups.add(subData);
        }
        return groups;
    }

    /**
     * 返回 actions 中去掉 preAction 后的子集
     * @param actions
     * @param preAction
     * @return
     */
    private Set<Property> getSubActions(Set<Property> actions,Property preAction){
        Set<Property> subAction = new HashSet<Property>();
        for(Property action:actions){
            if(!action.equals(preAction)){
                subAction.add(action);
            }
        }
        return subAction;
    }

    class SameResult{
        boolean result;
        Object type;

        public boolean getResult() {
            return result;
        }

        public void setResult(boolean result) {
            this.result = result;
        }

        public Object getType() {
            return type;
        }

        public void setType(Object type) {
            this.type = type;
        }
    }

    /**
     * 判断剩余的数据是否已经属于同一类
     * @param datas
     * @return
     */
    private SameResult withSameLabel(List<Data> datas){
        // 统计标签列，各个取值有几种，并且计算每一种取值的个数
        HashMap<Object,Integer> labelsCount = new HashMap<Object, Integer>();
        for(Data data:datas){
            Object type = data.getProperty(label);
            labelsCount.put(type, labelsCount.get(type) == null ? 1 : labelsCount.get(type)+1);
        }
        SameResult sameLabelResult = new SameResult();
        if(labelsCount.size() == 1){  // 数据datas 标签列只有一种类型
            sameLabelResult.setResult(true);
            sameLabelResult.setType(labelsCount.keySet().toArray()[0]);  // 返回结果设置为对应的类型
        }else{
            sameLabelResult.setResult(false);   // 不是只有一个类型
            Integer maxValue = 0;               // 将type 设置为样本数量最多的类
            for(Object key:labelsCount.keySet()){
                if(labelsCount.get(key) > maxValue){
                    maxValue = labelsCount.get(key);
                    sameLabelResult.setType(key);
                }
            }
        }

        return sameLabelResult;
    }


    /**
     * 判断剩余的数据是否在可选属性集上有相同的取值
     * @param datas
     * @param actions
     * @return
     */
    private boolean withSameProperties(List<Data> datas,Set<Property> actions){
        Map<Object,Integer> temp = new HashMap<Object, Integer>();  // 统计每一列各属性值出现的次数
        Object tempKey;
        for(Property property:actions){  //
            temp.clear();
            for(Data data:datas){
                tempKey = data.getProperty(property.getKey());
                temp.put(tempKey,temp.get(tempKey) == null ? 1 : temp.get(tempKey) + 1);
            }
            if(temp.size() != 1){   // 某一列属性不是只有一种
                return false;
            }
        }
        return true;
    }

    public void setType(int type) {
        this.type = type;
    }


    public TreeNode getResult() {
        return result;
    }

    public void setResult(TreeNode result) {
        this.result = result;
    }
}
