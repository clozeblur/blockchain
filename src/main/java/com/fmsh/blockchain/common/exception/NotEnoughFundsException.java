package com.fmsh.blockchain.common.exception;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/18 8:59
 * @Description:
 */
public class NotEnoughFundsException extends Exception {

    protected String msg;

    public NotEnoughFundsException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
