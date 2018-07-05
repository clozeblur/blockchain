package com.fmsh.blockchain.socket.handler.client;

import com.fmsh.blockchain.socket.base.AbstractBlockHandler;
import com.fmsh.blockchain.socket.body.RpcBlockBody;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;
import org.tio.utils.json.Json;

/**
 * 对获取所有区块信息请求的回复
 * @author wuweifeng wrote on 2018/3/12.
 */
@Slf4j
public class TotalBlockInfoResponseHandler extends AbstractBlockHandler<RpcBlockBody> {

    @Override
    public Class<RpcBlockBody> bodyClass() {
        return RpcBlockBody.class;
    }

    @Override
    public Object handler(BlockPacket packet, RpcBlockBody rpcBlockBody, ChannelContext channelContext) throws Exception {
        log.info("收到<请求生成Block的回应>消息", Json.toJson(rpcBlockBody));

        //TODO check合法性
        //TODO response

        return null;
    }
}
