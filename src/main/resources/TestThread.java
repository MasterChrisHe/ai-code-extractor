import java.util.concurrent.*;

public class TestThread{

    private Thread t = new Thread(() -> work(), "worker");

    private ThreadPoolExecutor threadPoolExecutor1 = new ThreadPoolExecutor(1, 2,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), r -> new Thread(r, "thread-abc-" + System.nanoTime()));


    public void abc(){
        Thread a=new Thread("abc");
        ThreadPoolExecutor threadPoolExecutor2 = new ThreadPoolExecutor(1, 2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), r -> new Thread(r, "my-thread-" + System.nanoTime()));
        ThreadPoolExecutor threadPoolExecutor3 = new ThreadPoolExecutor(1, 2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread("bbb");
            }
        });
    }

    public void testSetName(){
          Thread t1=new Thread();
          t1.setName("t12344");

        ExecutorService executorService = Executors.newFixedThreadPool(1);

    }

}