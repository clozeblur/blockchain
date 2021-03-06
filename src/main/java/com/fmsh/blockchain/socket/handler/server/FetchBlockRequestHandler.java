package com.fmsh.blockchain.socket.handler.server;

import com.fmsh.blockchain.ApplicationContextProvider;
import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.core.manager.BlockManager;
import com.fmsh.blockchain.socket.base.AbstractBlockHandler;
import com.fmsh.blockchain.socket.body.RpcBlockBody;
import com.fmsh.blockchain.socket.body.RpcSimpleBlockBody;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import com.fmsh.blockchain.socket.packet.PacketBuilder;
import com.fmsh.blockchain.socket.packet.PacketType;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.Aio;
import org.tio.core.ChannelContext;

/**
 * 请求别人某个区块的信息
 * @author wuweifeng wrote on 2018/3/12.
 */
@Slf4j
public class FetchBlockRequestHandler extends AbstractBlockHandler<RpcSimpleBlockBody> {

    @Override
    public Class<RpcSimpleBlockBody> bodyClass() {
        return RpcSimpleBlockBody.class;
    }

    @Override
    public Object handler(BlockPacket packet, RpcSimpleBlockBody rpcBlockBody, ChannelContext channelContext) {
        log.info("收到来自于<{}><请求该Block>消息，block hash为[{}]", rpcBlockBody.getAppId(), rpcBlockBody.getHash());
        Block block = ApplicationContextProvider.getBean(BlockManager.class).getBlockByHash(rpcBlockBody.getHash());

        BlockPacket blockPacket = new PacketBuilder<>().setType(PacketType.FETCH_BLOCK_INFO_RESPONSE).setBody(new
                RpcBlockBody(block)).build();
        Aio.send(channelContext, blockPacket);

        return null;
    }
}
