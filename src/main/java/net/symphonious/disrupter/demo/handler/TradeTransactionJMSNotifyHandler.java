package net.symphonious.disrupter.demo.handler;

import com.lmax.disruptor.EventHandler;

import net.symphonious.disrupter.demo.vo.TradeTransaction;

public class TradeTransactionJMSNotifyHandler implements EventHandler<TradeTransaction> {

    @Override
    public void onEvent(TradeTransaction event, long sequence, boolean endOfBatch) throws Exception {
        //do send jms message  
        System.out.println("JMS");
    }
}