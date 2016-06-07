package net.symphonious.disrupter.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.WorkerPool;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;

import net.symphonious.disrupter.demo.coretask.TaskManager;
import net.symphonious.disrupter.demo.coretask.ThreadTest;
import net.symphonious.disrupter.demo.handler.TradeTransactionInDBHandler;
import net.symphonious.disrupter.demo.handler.TradeTransactionJMSNotifyHandler;
import net.symphonious.disrupter.demo.vo.HotelDetailInfoVO;
import net.symphonious.disrupter.demo.vo.People;
import net.symphonious.disrupter.demo.vo.TradeTransaction;

public class demomain {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        demo2();

        //demo3();
        //TestTask(16, 128);
        //lamdaTest();
    }

    public static void demo1() throws InterruptedException {
        int BUFFER_SIZE = 1024;
        int THREAD_NUMBERS = 4;
        /*
         * createSingleProducer创建一个单生产者的RingBuffer，
         * 第一个参数叫EventFactory，从名字上理解就是“事件工厂”，其实它的职责就是产生数据填充RingBuffer的区块。
         * 第二个参数是RingBuffer的大小，它必须是2的指数倍 目的是为了将求模运算转为&运算提高效率
         * 第三个参数是RingBuffer的生产都在没有可用区块的时候(可能是消费者（或者说是事件处理器） 太慢了)的等待策略
         */
        final RingBuffer<TradeTransaction> ringBuffer = RingBuffer.createSingleProducer(new EventFactory<TradeTransaction>() {
            @Override
            public TradeTransaction newInstance() {
                return new TradeTransaction();
            }
        }, BUFFER_SIZE, new YieldingWaitStrategy());

        List<TradeTransaction> TradeTransactionList = new ArrayList<TradeTransaction>();
        long beginTime = System.currentTimeMillis();

        // 创建线程池
        ExecutorService executors = Executors.newFixedThreadPool(THREAD_NUMBERS);
        // 创建SequenceBarrier
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        // 创建消息处理器
        BatchEventProcessor<TradeTransaction> transProcessor = new BatchEventProcessor<TradeTransaction>(ringBuffer, sequenceBarrier,
            new TradeTransactionInDBHandler(TradeTransactionList));

        // 这一部的目的是让RingBuffer根据消费者的状态 如果只有一个消费者的情况可以省略
        ringBuffer.addGatingSequences(transProcessor.getSequence());

        // 把消息处理器提交到线程池
        executors.submit(transProcessor);
        // 如果存大多个消费者 那重复执行上面3行代码 把TradeTransactionInDBHandler换成其它消费者类

        Future<?> future = executors.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                long seq;
                for (int i = 0; i < 1000; i++) {
                    seq = ringBuffer.next();// 占个坑 --ringBuffer一个可用区块

                    ringBuffer.get(seq).setPrice(Math.random() * 9999);// 给这个区块放入
                                                                       // 数据
                                                                       // 如果此处不理解，想想RingBuffer的结构图

                    ringBuffer.publish(seq);// 发布这个区块的数据使handler(consumer)可见
                }
                return null;
            }
        });
        try {
            future.get();
        } catch (InterruptedException e) {

        } catch (ExecutionException e) {

        }
        // 等待生产者结束
        executors.isTerminated();
        transProcessor.halt();// 通知事件(或者说消息)处理器 可以结束了（并不是马上结束!!!）
        executors.shutdown();// 终止线程
        System.out.println("ringbuffer总耗时:" + (System.currentTimeMillis() - beginTime));
    }

    public static void demo2() {
        int BUFFER_SIZE = 1024 * 1024;
        int THREAD_NUMBERS = 16;
        long beginTime = System.currentTimeMillis();
        List<TradeTransaction> TradeTransactionList = new ArrayList<TradeTransaction>();
        EventFactory<TradeTransaction> eventFactory = new EventFactory<TradeTransaction>() {
            public TradeTransaction newInstance() {
                return new TradeTransaction();
            }
        };
        RingBuffer<TradeTransaction> ringBuffer = RingBuffer.createSingleProducer(eventFactory, BUFFER_SIZE);
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUMBERS);

        WorkHandler<TradeTransaction> workHandlers = new TradeTransactionInDBHandler(TradeTransactionList);

        WorkerPool<TradeTransaction> workerPool = new WorkerPool<TradeTransaction>(ringBuffer, sequenceBarrier, new IgnoreExceptionHandler(), workHandlers);

        workerPool.start(executor);

        for (int i = 0; i < 10000000; i++) {
            long seq = ringBuffer.next();
            ringBuffer.get(seq).setPrice(Math.random() * 9999);
            ringBuffer.publish(seq);
        }
        workerPool.drainAndHalt();
        workerPool.halt();
        executor.shutdown();

        System.out.println("执行结束");
        System.out.println("总耗时:" + (System.currentTimeMillis() - beginTime));
    }

    public static void demo3() {
        long beginTime = System.currentTimeMillis();

        int bufferSize = 1024;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        // 这个构造函数参数，相信你在了解上面2个demo之后就看下就明白了，不解释了~
        Disruptor<TradeTransaction> disruptor = new Disruptor<TradeTransaction>(new EventFactory<TradeTransaction>() {
            @Override
            public TradeTransaction newInstance() {
                return new TradeTransaction();
            }
        }, bufferSize, executor, ProducerType.SINGLE, new BusySpinWaitStrategy());
        List<TradeTransaction> TradeTransactionList = new ArrayList<TradeTransaction>();
        // 使用disruptor创建消费者组C1,C2
        TradeTransactionVasConsumer tradeTransactionVasConsumer = new TradeTransactionVasConsumer();
        TradeTransactionInDBHandler tradeTransactionInDBHandler = new TradeTransactionInDBHandler(TradeTransactionList);

        // 事件处理组
        EventHandlerGroup<TradeTransaction> handlerGroup = disruptor.handleEventsWith(tradeTransactionVasConsumer, tradeTransactionInDBHandler);

        TradeTransactionJMSNotifyHandler jmsConsumer = new TradeTransactionJMSNotifyHandler();

        // 声明在C1,C2完事之后执行JMS消息发送操作 也就是流程走到C3
        handlerGroup.then(jmsConsumer);
        handlerGroup.handleEventsWith(jmsConsumer);
        disruptor.start();// 启动
        CountDownLatch latch = new CountDownLatch(1);
        // 生产者准备
        executor.submit(new TradeTransactionPublisher(latch, disruptor));

        try {
            // 等待生产者完事.
            latch.await();
        } catch (InterruptedException e) {
            // logger.error("", e);
        }

        disruptor.shutdown();
        executor.shutdown();
        System.out.println(TradeTransactionList.size());
        System.out.println("总耗时:" + (System.currentTimeMillis() - beginTime));
    }

    /**
     * 去重与排序
     * 
     * @param hotelDetailInfoList
     * @param isdesc
     *            是否是降序
     */
    public static void HotelDetailInfosortandDuplicate(List<HotelDetailInfoVO> hotelDetailInfoList, final boolean isdesc) {

        Map<String, HotelDetailInfoVO> hotelDetailInfoVODic = new HashMap<String, HotelDetailInfoVO>();
        List<HotelDetailInfoVO> resultList = new ArrayList<HotelDetailInfoVO>();
        // 去除价格高的重复数据
        for (HotelDetailInfoVO hotelDetailInfoVO : hotelDetailInfoList) {

            if (hotelDetailInfoVODic.containsKey(hotelDetailInfoVO.getResourceId().toString())) {
                if (hotelDetailInfoVODic.get(hotelDetailInfoVO.getResourceId().toString()).getLowestPrice().compareTo(hotelDetailInfoVO.getLowestPrice()) >= 0) {
                    hotelDetailInfoVODic.put(hotelDetailInfoVO.getResourceId().toString(), hotelDetailInfoVO);
                }
            } else {
                hotelDetailInfoVODic.put(hotelDetailInfoVO.getResourceId().toString(), hotelDetailInfoVO);
            }
        }

        // 将Map Key 转化为List
        List<HotelDetailInfoVO> mapValuesList = new ArrayList<HotelDetailInfoVO>(hotelDetailInfoVODic.values());
        // 排序
        Collections.sort(mapValuesList, new Comparator<HotelDetailInfoVO>() {
            public int compare(HotelDetailInfoVO o1, HotelDetailInfoVO o2) {
                return isdesc ? o2.getLowestPrice().compareTo(o1.getLowestPrice()) : o1.getLowestPrice().compareTo(o2.getLowestPrice());
            }
        });

        for (HotelDetailInfoVO item : mapValuesList) {
            System.out.println(item.getResourceId() + "---" + item.getLowestPrice());
        }
    }

    /**
     * 
     */
    public static void ListTest() {
        List<Integer> test = new ArrayList<Integer>();
        //init list  
        for (int i = 0; i < 5; i++) {
            test.add(i); //auto boxing  
        }
        //display the list  
        System.out.print("the orginal list: ");
        for (int i = 0; i < test.size(); i++) {
            System.out.print(test.get(i) + " ");
        }
        System.out.println();

        List<Integer> sub = test.subList(1, 3);

        System.out.print("the orginal list after sublist modified: ");
        for (int i = 0; i < test.size(); i++) {
            System.out.print(test.get(i) + " ");
        }
        System.out.println();

    }

    public static void TestTask(int coreThread, int threadpool) {

        long beginTime = System.currentTimeMillis();
        try {
            List<TradeTransaction> tradeTransactionList = new ArrayList<TradeTransaction>();
            //System.out.println(String.format("核心线程数%d,最大线程数%d", coreThread, threadpool));
            TaskManager manager = TaskManager.getInstance(coreThread, threadpool);
            TradeTransaction tradeTransaction = null;
            ThreadTest threadTest = null;
            for (int i = 0; i < 10000000; i++) {
                tradeTransaction = new TradeTransaction();
                tradeTransaction.setPrice(Math.random() * 9999);
                threadTest = new ThreadTest(tradeTransactionList, tradeTransaction);
                manager.doTask(threadTest);
            }

            //            System.out.println("PoolActiveSize:" + manager.getPoolActiveSize());
            //            System.out.println("PoolSize:" + manager.getPoolSize());
            //            System.out.println("QueueSize:" + manager.getQueueSize());
            //等待所有子线程执行完毕
            manager.waitAllThreadFinsh();

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("总耗时:" + (System.currentTimeMillis() - beginTime));
    }

    public static void lamdaTest() {
        List<People> peoples = new ArrayList<People>();
        for (int i = 0; i < 10000; i++) {
            Integer gerad = new Random().nextInt(100);
            peoples.add(new People(String.format("牛顿%d", i), gerad));
        }

        long beginTime = System.currentTimeMillis();
        Predicate<People> predicate = p -> p.getAge() > 70;
        peoples.stream().filter(predicate).forEach(p -> {
            System.out.println(p.getName());
        });

        System.out.println("总耗时:" + (System.currentTimeMillis() - beginTime));
        //stream（）方法得到一个流
        //        peoples.stream().forEach(p -> {
        //            System.out.println(p.getName());
        //        });
    }

}
