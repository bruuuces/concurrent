package harry.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by zhangbin on 15/11/23.
 */
public class TestOptimisticLockingBlockingQueue {
    OptimisticLockingBlockingQueue<Integer> queue = new OptimisticLockingBlockingQueue(21);
//    BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(2097152);

    @org.junit.Test
    public void testTake() throws Exception {
        System.out.println(1 << 21);

        Runnable offer = new Runnable() {
            public void run() {
                try {
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < 1 << 21; i++) {
                        queue.put(1000000000 + i);
                    }
                    long end = System.currentTimeMillis();

                    System.out.println("offer:" + (end - start));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Runnable take = new Runnable() {
            public void run() {
                try {
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < 1 << 23; i++) {
                        queue.take();
                    }
                    long end = System.currentTimeMillis();

                    System.out.println("take:" + (end - start));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(take).start();
        for (int i=0;i<4;i++) {
            new Thread(offer).start();
        }


        Thread.sleep(10000);
    }
}
