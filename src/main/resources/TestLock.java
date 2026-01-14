import com.github.javaparser.ast.stmt.SynchronizedStmt;

import java.util.concurrent.locks.ReentrantLock;

public class TestLock{


    public  void test(){
        synchronized(Object.class){
            String a= new String("abc");
            System.out.println(a);
            ReentrantLock lock = new ReentrantLock();
            lock.lock();
            lock.unlock();
        }
    }

    public synchronized void test2(){
        String a= new String("abc");
        if(a.equals("abc")){

        }
    }
}