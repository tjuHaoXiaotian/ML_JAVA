package cn.edu.tju.scs.hxt.tree;

/**
 * Created by haoxiaotian on 2017/3/8 16:15.
 */
import tju.scs.hxt.decisiontree.DecisionTree;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Panel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/**
 * @author John
 *
 */
public class TestDrawTree extends JFrame{

    public TestDrawTree(){
        super("Test Draw Tree");
        initComponents();
    }

    public static void main(String[] args){
        TestDrawTree frame = new TestDrawTree();

        frame.setSize(2000, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



    }

    public void initComponents(){
        DecisionTree decisionTree = new DecisionTree();

//        System.out.println();
        decisionTree.trainForResult();
        System.out.println("正确率："+decisionTree.verification() + "%");
        /*
         * 创建一个用于绘制树的面板并将树传入,使用相对对齐方式
         */
        TreePanel panel1 = new TreePanel(TreePanel.CHILD_ALIGN_RELATIVE);
//        TreePanel panel1 = new TreePanel(TreePanel.CHILD_ALIGN_ABSOLUTE);
        panel1.setTree(decisionTree.getResult());

        /*
         * 创建一个用于绘制树的面板并将树传入,使用绝对对齐方式
         */
        TreePanel panel2 = new TreePanel(TreePanel.CHILD_ALIGN_ABSOLUTE);
        panel2.setTree(decisionTree.getResult());
        panel2.setBackground(Color.BLACK);
        panel2.setGridColor(Color.WHITE);
        panel2.setLinkLineColor(Color.WHITE);
        panel2.setStringColor(Color.BLACK);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridLayout(1,1));
        contentPane.add(panel1);
//        contentPane.add(panel2);

        add(contentPane,BorderLayout.CENTER);
    }
}
