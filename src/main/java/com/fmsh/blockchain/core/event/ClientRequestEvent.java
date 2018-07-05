package com.fmsh.blockchain.core.event;

import com.fmsh.blockchain.socket.packet.BlockPacket;
import org.springframework.context.ApplicationEvent;

/**
 * 客户端对外发请求时会触发该Event
 * @author wuweifeng wrote on 2018/3/17.
 */
public class ClientRequestEvent extends ApplicationEvent {
    public ClientRequestEvent(BlockPacket blockPacket) {
        super(blockPacket);
    }
}
