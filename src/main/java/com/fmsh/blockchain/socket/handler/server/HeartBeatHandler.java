package com.fmsh.blockchain.socket.handler.server;

import com.fmsh.blockchain.socket.base.AbstractBlockHandler;
import com.fmsh.blockchain.socket.body.HeartBeatBody;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;

/**
 * 客户端心跳包
 * @author wuweifeng wrote on 2018/3/12.
 */
@Slf4j
public class HeartBeatHandler extends AbstractBlockHandler<HeartBeatBody> {

    @Override
    public Class<HeartBeatBody> bodyClass() {
        return HeartBeatBody.class;
    }

    @Override
    public Object handler(BlockPacket packet, HeartBeatBody heartBeatBody, ChannelContext channelContext) throws Exception {
        log.info("收到<心跳包>消息", heartBeatBody.getText());

        return null;
    }
}
