public class TestThrow{



    public void test(){
        throw new IllegalAccessException("test exception");
    }

    public void test1(){
        String a="wbj";
        throw new IllegalAccessException("内容:::"+a);
    }


    public void test2(){
        Exception exception = new Exception("test 2");
        throw exception;
    }
}