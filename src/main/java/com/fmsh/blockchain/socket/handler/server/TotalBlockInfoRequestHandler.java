package com.fmsh.blockchain.socket.handler.server;

import com.fmsh.blockchain.socket.base.AbstractBlockHandler;
import com.fmsh.blockchain.socket.body.RpcBlockBody;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;
import org.tio.utils.json.Json;

/**
 * 获取全部区块信息的请求，全网广播
 * @author wuweifeng wrote on 2018/3/12.
 */
@Slf4j
public class TotalBlockInfoRequestHandler extends AbstractBlockHandler<RpcBlockBody> {

    @Override
    public Class<RpcBlockBody> bodyClass() {
        return RpcBlockBody.class;
    }

    @Override
    public Object handler(BlockPacket packet, RpcBlockBody rpcBlockBody, ChannelContext channelContext) {
        log.info("收到<请求生成Block的回应>消息: {}", Json.toJson(rpcBlockBody.getBlock().getHash()));

        //TODO check合法性
        //TODO response

        return null;
    }
}
