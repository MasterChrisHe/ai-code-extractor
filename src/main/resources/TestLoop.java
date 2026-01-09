import java.util.List;

public class TestLoop {


    public void test(String a,String b,T node) {
        for (int i = 0; i < 10; i++) {
            String s = new String("123");
            System.out.println("123");
        }

        List<String> list = List.of("1", "2", "3");
        for (String s : list) {
            System.out.println(s);
        }

        while (true) {
            System.out.println("while");
            System.out.println("true");
        }

        do {
            System.out.println("do");
            System.out.println("while true");
        }while (true);
    }

}