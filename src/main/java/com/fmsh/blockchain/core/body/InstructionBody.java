package com.fmsh.blockchain.core.body;

/**
 * @author wuweifeng wrote on 2018/3/7.
 */
public class InstructionBody {
    /**
     * 指令的操作，增删改
     */
    private byte operation;
    /**
     * 操作的表名
     */
    private String table;
    /**
     * 具体内容
     */
    private String json;

    @Override
    public String toString() {
        return "InstructionBody{" +
                "operation=" + operation +
                ", table='" + table + '\'' +
                ", json='" + json + '\'' +
                '}';
    }

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

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
