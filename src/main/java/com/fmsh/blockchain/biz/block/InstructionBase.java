package com.fmsh.blockchain.biz.block;

import com.fmsh.blockchain.biz.transaction.Transaction;

import java.io.Serializable;

/**
 * blockBody内一条指令的基础属性
 * @author wuweifeng wrote on 2018/4/4.
 */
public class InstructionBase implements Serializable {
    private static final long serialVersionUID = 5214995961916840606L;
    /**
     * 指令的操作，增删改（1，-1，2）
     */
    private byte operation;
    /**
     * 操作的表名
     */
    private String table;
    /**
     * 最终要执行入库的json内容
     */
    private String oldJson;
    /**
     * 业务id，sql语句中where需要该Id
     */
    private String instructionId;
    /**
     * instruction唯一对应transaction
     */
    private Transaction transaction;

    public byte getOperation() {
        return operation;
    }

    public void setOperation(byte operation) {
        this.operation = operation;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getOldJson() {
        return oldJson;
    }

    public void setOldJson(String oldJson) {
        this.oldJson = oldJson;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public String toString() {
        return "InstructionBase{" +
                "operation=" + operation +
                ", table='" + table + '\'' +
                ", oldJson='" + oldJson + '\'' +
                ", instructionId='" + instructionId + '\'' +
                ", transaction=" + transaction +
                '}';
    }
}
