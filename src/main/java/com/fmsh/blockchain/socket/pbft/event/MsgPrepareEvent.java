package com.fmsh.blockchain.socket.pbft.event;

import com.fmsh.blockchain.socket.pbft.msg.VoteMsg;
import org.springframework.context.ApplicationEvent;

/**
 * 消息已被验证，进入到Prepare集合中
 * @author wuweifeng wrote on 2018/4/25.
 */
public class MsgPrepareEvent extends ApplicationEvent {
    public MsgPrepareEvent(VoteMsg source) {
        super(source);
    }
}
