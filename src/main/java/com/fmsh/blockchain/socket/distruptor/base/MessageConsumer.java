package com.fmsh.blockchain.socket.distruptor.base;

/**
 * @author wuweifeng wrote on 2018/4/20.
 */
public interface MessageConsumer {
    void receive(BaseEvent baseEvent) throws Exception;
}
