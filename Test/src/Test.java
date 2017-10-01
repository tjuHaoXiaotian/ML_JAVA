import java.util.ArrayList;
import java.util.List;

/**
 * Created by hxt on 17-10-1.
 */
public class Test {
    public static void main(String [] args){
        List<String> list1 = new ArrayList<String>();
        list1.add("1");
        list1.add("2");
        list1.add("3");

        List<String> list2 = list1;

        System.out.println("list1: \n"+list1);
        System.out.println("list2: \n"+list2);

        list2.clear();
        System.out.println("list1: \n"+list1);
        System.out.println("list2: \n"+list2);
    }
}
