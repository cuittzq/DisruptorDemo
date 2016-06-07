package net.symphonious.disrupter.demo.coretask;

import java.util.List;
import java.util.UUID;

import net.symphonious.disrupter.demo.vo.TradeTransaction;

public class ThreadTest implements Runnable {

    private static Object          lockobj = new Object();

    private List<TradeTransaction> tradeTransactionList;

    private TradeTransaction       tradeTransaction;

    public ThreadTest(List<TradeTransaction> tradeTransactionList, TradeTransaction event) {
        this.tradeTransactionList = tradeTransactionList;
        this.tradeTransaction = event;
    }

    @Override
    public void run() {
        synchronized (lockobj) {
            this.tradeTransaction.setId(UUID.randomUUID().toString());// 简单生成下ID
            // System.out.println(this.tradeTransaction.getId());
            this.tradeTransactionList.add(this.tradeTransaction);
        }
    }
}
