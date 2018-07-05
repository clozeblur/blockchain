package com.fmsh.blockchain.socket.distruptor;

import com.lmax.disruptor.EventHandler;
import com.mindata.blockchain.ApplicationContextProvider;
import com.mindata.blockchain.socket.distruptor.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wuweifeng wrote on 2018/4/20.
 */
public class DisruptorServerHandler implements EventHandler<BaseEvent> {
	
	private Logger logger = LoggerFactory.getLogger(DisruptorServerHandler.class);

    @Override
    public void onEvent(BaseEvent baseEvent, long sequence, boolean endOfBatch) throws Exception {
    	try {
    		ApplicationContextProvider.getBean(DisruptorServerConsumer.class).receive(baseEvent);
		} catch (Exception e) {
			logger.error("Disruptor事件执行异常",e);
		}
    }
}
