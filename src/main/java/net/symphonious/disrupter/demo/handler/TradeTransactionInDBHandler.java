package net.symphonious.disrupter.demo.handler;

import java.util.List;
import java.util.UUID;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;

import net.symphonious.disrupter.demo.vo.TradeTransaction;

public class TradeTransactionInDBHandler implements EventHandler<TradeTransaction>, WorkHandler<TradeTransaction> {

    private List<TradeTransaction> TradeTransactionList;

    public TradeTransactionInDBHandler(List<TradeTransaction> TradeTransactionList) {
        this.TradeTransactionList = TradeTransactionList;
    }

    public List<TradeTransaction> getTradeTransactionList() {
        return TradeTransactionList;
    }

    public void setTradeTransactionList(List<TradeTransaction> tradeTransactionList) {
        TradeTransactionList = tradeTransactionList;
    }

    @Override
    public void onEvent(TradeTransaction event, long sequence, boolean endOfBatch) throws Exception {
        this.onEvent(event);
    }

    @Override
    public void onEvent(TradeTransaction event) throws Exception {
        // 这里做具体的消费逻辑
        event.setId(UUID.randomUUID().toString());// 简单生成下ID
        //System.out.println(event.getId());
        this.TradeTransactionList.add(event);

    }
}
