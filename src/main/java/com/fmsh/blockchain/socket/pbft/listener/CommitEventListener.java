package com.fmsh.blockchain.socket.pbft.listener;

import com.fmsh.blockchain.socket.body.VoteBody;
import com.fmsh.blockchain.socket.client.PacketSender;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import com.fmsh.blockchain.socket.packet.PacketBuilder;
import com.fmsh.blockchain.socket.packet.PacketType;
import com.fmsh.blockchain.socket.pbft.event.MsgCommitEvent;
import com.fmsh.blockchain.socket.pbft.msg.VoteMsg;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 监听block可以commit消息
 * @author wuweifeng wrote on 2018/4/25.
 */
@Component
public class CommitEventListener {
    @Resource
    private PacketSender packetSender;

    /**
     * block已经开始进入commit状态，广播消息
     *
     * @param msgCommitEvent
     *         msgCommitEvent
     */
    @EventListener
    public void msgIsCommit(MsgCommitEvent msgCommitEvent) {
        VoteMsg voteMsg = (VoteMsg) msgCommitEvent.getSource();

        //群发消息，通知所有节点，我已对该Block Prepare
        BlockPacket blockPacket = new PacketBuilder<>().setType(PacketType.PBFT_VOTE).setBody(new
                VoteBody(voteMsg)).build();

        //广播给所有人我已commit
        packetSender.sendGroup(blockPacket);
    }
}
