package com.fmsh.blockchain.socket.distruptor;

import com.fmsh.blockchain.ApplicationContextProvider;
import com.fmsh.blockchain.socket.distruptor.base.BaseEvent;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuweifeng wrote on 2018/4/20.
 */
@Slf4j
public class DisruptorServerHandler implements EventHandler<BaseEvent> {

    @Override
    public void onEvent(BaseEvent baseEvent, long sequence, boolean endOfBatch) throws Exception {
    	try {
    		ApplicationContextProvider.getBean(DisruptorServerConsumer.class).receive(baseEvent);
		} catch (Exception e) {
			log.error("Disruptor事件执行异常",e);
		}
    }
}
