package harry.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by zhangbin on 15/11/22.
 */
public class OptimisticLockingBlockingQueue<T> {
    private int capacity; //为对象开辟的总空间
    private int shield; //索引掩码
    private Entry[] inventorys = null; //库存
    private AtomicInteger productionIndex = null; //生产者索引
    private AtomicInteger consumptionIndex = null; //消费者索引
    private Object productionLicense = null; //生产许可证
    private Object consumptionLicense = null; //消费许可证

    public OptimisticLockingBlockingQueue(int capacityLevel) {
        if (capacityLevel > 30) {
            capacityLevel = 30;
        }
        this.capacity = 1 << capacityLevel; //比如capacityLevel==10;capacity=1024
        this.shield = Integer.MAX_VALUE >> (Integer.SIZE - 1 - capacityLevel); //初始化索引掩码
        this.inventorys = new Entry[this.capacity]; //初始化缓冲区
        for (int i = 0; i < this.capacity; i++) {
            inventorys[i] = new Entry();
        }
        this.productionIndex = new AtomicInteger(-1);
        this.consumptionIndex = new AtomicInteger(-1);
        this.productionLicense = new Object();
        this.consumptionLicense = new Object();
    }

    /**
     * 获取下一个要生产的对象
     *
     * @return 下一个[空]箱子(如果商品积压, 箱子非空)
     */
    private Entry nextProduction() {
        return inventorys[productionIndex.incrementAndGet() & shield];
    }

    /**
     * 获取下一个要消费的对象
     *
     * @return 下一个[非空]箱子(如果没有库存, 箱子为空)
     */
    private Entry nextConsumption() {
        return inventorys[consumptionIndex.incrementAndGet() & shield];
    }

    /**
     * 获得为队列开辟的总空间
     *
     * @return 队列总空间(流水线总长度)
     */
    public int capacity() {
        return capacity;
    }


    /**
     * 生产一个对象
     */
    public void put(T event) throws InterruptedException {
        Entry entry = nextProduction();
        while (!entry.applicationProductionLicense(this.productionLicense)) ;
        if (entry.event != null) { //如果商品积压,等待空箱子
            synchronized (this.productionLicense) {
                while (entry.event != null) { //等待空箱子
                    this.productionLicense.wait();
                }
            }
        }
        entry.production(event);
    }

    /**
     * 消费一个对象
     */
    public T take() throws InterruptedException {
        Entry entry = nextConsumption();
        while (!entry.applicationConsumptionLicense(this.consumptionLicense)) ;
        if (entry.event == null) { //如果没有库存,等待库存
            synchronized (this.consumptionLicense) {
                while (entry.event == null) { //等待库存
                    this.consumptionLicense.wait();
                }
            }
        }
        return (T) entry.consumption();
    }

    private class Entry<T> {
        private volatile T event = null; //实际对象
        private AtomicReference<Object> inlet = new AtomicReference<Object>(); //入口
        private AtomicReference<Object> outlet = new AtomicReference<Object>(); //出口

        /**
         * 出示生产许可证
         */
        public boolean applicationProductionLicense(Object license) {
            return inlet.compareAndSet(null, license);
        }

        /**
         * 出示消费许可证
         */
        public boolean applicationConsumptionLicense(Object license) {
            return outlet.compareAndSet(null, license);
        }

        /**
         * 生产
         */
        public void production(T event) {
            this.event = event;
            this.inlet.set(null);
            Object license = this.outlet.get();
            if (license != null) {
                synchronized (license) {
                    license.notify();
                }
            }
        }

        /**
         * 消费
         */
        public T consumption() {
            T event = this.event;
            this.event = null;
            this.outlet.set(null);
            Object license = this.inlet.get();
            if (license != null) {
                synchronized (license) {
                    license.notify();
                }
            }
            return event;
        }
    }


}

