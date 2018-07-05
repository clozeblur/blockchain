package com.fmsh.blockchain.socket.handler.server;

import com.fmsh.blockchain.ApplicationContextProvider;
import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.core.manager.CheckerManager;
import com.fmsh.blockchain.socket.base.AbstractBlockHandler;
import com.fmsh.blockchain.socket.body.RpcBlockBody;
import com.fmsh.blockchain.socket.body.RpcCheckBlockBody;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import com.fmsh.blockchain.socket.pbft.VoteType;
import com.fmsh.blockchain.socket.pbft.msg.VotePreMsg;
import com.fmsh.blockchain.socket.pbft.queue.MsgQueueManager;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;

/**
 * 收到请求生成区块消息，进入PrePre队列
 *
 * @author wuweifeng wrote on 2018/3/12.
 */
@Slf4j
public class GenerateBlockRequestHandler extends AbstractBlockHandler<RpcBlockBody> {

    @Override
    public Class<RpcBlockBody> bodyClass() {
        return RpcBlockBody.class;
    }

    @Override
    public Object handler(BlockPacket packet, RpcBlockBody rpcBlockBody, ChannelContext channelContext) {
        Block block = rpcBlockBody.getBlock();
        log.info("收到来自于<" + rpcBlockBody.getAppId() + "><请求生成Block>消息，block信息为[" + block + "]");

        CheckerManager checkerManager = ApplicationContextProvider.getBean(CheckerManager.class);
        //对区块的基本信息进行校验，校验通过后进入pbft的Pre队列
        RpcCheckBlockBody rpcCheckBlockBody = checkerManager.check(block);
        log.info("校验结果:" + rpcCheckBlockBody.toString());
        if (rpcCheckBlockBody.getCode() == 0) {
            VotePreMsg votePreMsg = new VotePreMsg();
            votePreMsg.setBlock(block);
            votePreMsg.setVoteType(VoteType.PREPREPARE);
            votePreMsg.setNumber(block.getBlockHeader().getNumber());
            votePreMsg.setAppId(rpcBlockBody.getAppId());
            votePreMsg.setHash(block.getHash());
            votePreMsg.setAgree(true);
            //将消息推入PrePrepare队列
            ApplicationContextProvider.getBean(MsgQueueManager.class).pushMsg(votePreMsg);
        }

        return null;
    }
}
