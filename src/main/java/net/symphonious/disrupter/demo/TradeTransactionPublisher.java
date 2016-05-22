package net.symphonious.disrupter.demo;

import java.util.concurrent.CountDownLatch;

import com.lmax.disruptor.dsl.Disruptor;

import net.symphonious.disrupter.demo.vo.TradeTransaction;

/**
 * 生产者
 * @author tzq24955
 * @version $Id: TradeTransactionPublisher.java, v 0.1 2016年5月20日 下午8:17:40 tzq24955 Exp $
 */
public class TradeTransactionPublisher implements Runnable {
    Disruptor<TradeTransaction> disruptor;
    private CountDownLatch      latch;
    private static int          LOOP = 10;//模拟一千万次交易的发生  

    public TradeTransactionPublisher(CountDownLatch latch, Disruptor<TradeTransaction> disruptor) {
        this.disruptor = disruptor;
        this.latch = latch;
    }

    @Override
    public void run() {
        TradeTransactionEventTranslator tradeTransloator = new TradeTransactionEventTranslator();
        for (int i = 0; i < LOOP; i++) {
            disruptor.publishEvent(tradeTransloator);
        }
        latch.countDown();
    }

}
