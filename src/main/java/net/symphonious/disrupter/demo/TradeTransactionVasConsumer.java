package net.symphonious.disrupter.demo;

import com.lmax.disruptor.EventHandler;

import net.symphonious.disrupter.demo.vo.TradeTransaction;

public class TradeTransactionVasConsumer implements EventHandler<TradeTransaction> {

    @Override
    public void onEvent(TradeTransaction event, long sequence, boolean endOfBatch) throws Exception {
        //do something....  
        System.out.println("Consumer");
    }

}
