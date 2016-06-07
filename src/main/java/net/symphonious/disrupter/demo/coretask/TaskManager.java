package net.symphonious.disrupter.demo.coretask;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class TaskManager {
    private static TaskManager instance;

    private ThreadPoolExecutor pool;

    private TaskSupport        taskSupport;

    private TaskManager(int poolSize, int maxpool) {
        if (poolSize == 0) {
            poolSize = Integer.parseInt(System.getenv("NUMBER_OF_PROCESSORS"));
        }
        taskSupport = new TaskSupport(maxpool);
        pool = new ThreadPoolExecutor(poolSize, maxpool, 1000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
     * 外部执行入口
     * @param task
     */
    public void doTask(final Runnable task) {
        if (taskSupport.waitNotFull()) {
            pool.execute(new Runnable() {
                public void run() {
                    try {
                        task.run();
                    } finally {
                        taskSupport.removeQueue();
                    }
                }
            });
        }
    }

    public void waitAllThreadFinsh() {
        taskSupport.waitAllThreadFinsh();
    }

    public static synchronized TaskManager getInstance(int cpuCount, int maxpool) {
        if (instance == null) {
            instance = new TaskManager(cpuCount, maxpool);
        }
        return instance;
    }

    public Integer getPoolActiveSize() {
        if (pool != null) {
            return pool.getActiveCount();
        }

        return 0;
    }

    public Integer getPoolSize() {
        if (pool != null) {
            return pool.getPoolSize();
        }

        return 0;
    }

    public Integer getQueueSize() {
        if (taskSupport != null) {
            return taskSupport.supportQueue.size();
        }

        return 0;
    }
}
