package com.fmsh.blockchain.socket.handler.server;

import com.fmsh.blockchain.ApplicationContextProvider;
import com.fmsh.blockchain.socket.base.AbstractBlockHandler;
import com.fmsh.blockchain.socket.body.VoteBody;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import com.fmsh.blockchain.socket.pbft.msg.VoteMsg;
import com.fmsh.blockchain.socket.pbft.queue.MsgQueueManager;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;

/**
 * pbft投票处理
 *
 * @author wuweifeng wrote on 2018/3/12.
 */
@Slf4j
public class PbftVoteHandler extends AbstractBlockHandler<VoteBody> {

    @Override
    public Class<VoteBody> bodyClass() {
        return VoteBody.class;
    }

    @Override
    public Object handler(BlockPacket packet, VoteBody voteBody, ChannelContext channelContext) {
        VoteMsg voteMsg = voteBody.getVoteMsg();
        log.info("收到来自于<" + voteMsg.getAppId() + "><投票>消息，投票信息为[" + voteMsg + "]");

        ApplicationContextProvider.getBean(MsgQueueManager.class).pushMsg(voteMsg);
        return null;
    }
}
