package net.symphonious.disrupter.demo;

import java.util.Random;

import com.lmax.disruptor.EventTranslator;

import net.symphonious.disrupter.demo.vo.TradeTransaction;

public class TradeTransactionEventTranslator implements EventTranslator<TradeTransaction> {
    private Random random = new Random();

    @Override
    public void translateTo(TradeTransaction event, long sequence) {
        this.generateTradeTransaction(event);
    }

    private TradeTransaction generateTradeTransaction(TradeTransaction trade) {
        trade.setPrice(random.nextDouble() * 9999);
        return trade;
    }
}
