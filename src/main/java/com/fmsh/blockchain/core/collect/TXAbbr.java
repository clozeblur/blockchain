package com.fmsh.blockchain.core.collect;

import com.fmsh.blockchain.biz.transaction.Transaction;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/8/13 14:15
 * @Description:
 */
public class TXAbbr {

    public TXAbbr(Transaction tx, String sender, String receiver, Long amount) {
        this.tx = tx;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
    }

    private Transaction tx;

    private String sender;

    private String receiver;

    private Long amount;

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "TXAbbr{" +
                "tx=" + tx +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", amount=" + amount +
                '}';
    }
}
