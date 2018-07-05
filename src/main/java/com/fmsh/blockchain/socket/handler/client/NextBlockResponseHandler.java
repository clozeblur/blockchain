package com.fmsh.blockchain.socket.handler.client;

import com.fmsh.blockchain.ApplicationContextProvider;
import com.fmsh.blockchain.socket.base.AbstractBlockHandler;
import com.fmsh.blockchain.socket.body.BlockHash;
import com.fmsh.blockchain.socket.body.RpcNextBlockBody;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import com.fmsh.blockchain.socket.pbft.queue.NextBlockQueue;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;

/**
 * 对方根据我们传的hash，给我们返回的next block
 *
 * @author wuweifeng wrote on 2018/3/16.
 */
@Slf4j
public class NextBlockResponseHandler extends AbstractBlockHandler<RpcNextBlockBody> {

    @Override
    public Class<RpcNextBlockBody> bodyClass() {
        return RpcNextBlockBody.class;
    }

    @Override
    public Object handler(BlockPacket packet, RpcNextBlockBody rpcBlockBody, ChannelContext channelContext) {
        log.info("收到来自于<" + rpcBlockBody.getAppId() + ">的回复，下一个Block hash为：" + rpcBlockBody.getHash());

        String hash = rpcBlockBody.getHash();
        //如果为null，说明对方根据我们传过去的hash，找不到next block。说明要么已经是最新了，要么对方的block比自己的少
        if (hash == null) {
            log.info("和<" + rpcBlockBody.getAppId() + ">相比，本地已是最新块了");
        } else {
            BlockHash blockHash = new BlockHash(hash, rpcBlockBody.getPrevHash(), rpcBlockBody.getAppId());
            //此处进行搜集next block的hash，相同的hash过2f+1时可以确认
            ApplicationContextProvider.getBean(NextBlockQueue.class).push(blockHash);
        }

        return null;
    }
}
