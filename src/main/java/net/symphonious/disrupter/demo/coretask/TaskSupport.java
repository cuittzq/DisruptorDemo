package net.symphonious.disrupter.demo.coretask;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TaskSupport {
    BlockingQueue<Object> supportQueue;

    public TaskSupport(int poolSize) {
        supportQueue = new ArrayBlockingQueue<Object>(poolSize);
    }

    /**
     * 等待队列有可用空间，之前一直等待。
     */
    public boolean waitNotFull() {
        try {
            while (!supportQueue.offer(new Object(), 1, TimeUnit.MILLISECONDS)) {
            }
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    /**
     * 当前线程等待子线程执行完成
     */
    public void waitAllThreadFinsh() {
        while (supportQueue.size() > 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
    }

    public void removeQueue() {
        supportQueue.poll();
    }
}
