package com.fmsh.blockchain.socket.handler.server;

import com.fmsh.blockchain.ApplicationContextProvider;
import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.common.timer.TimerManager;
import com.fmsh.blockchain.core.manager.BlockManager;
import com.fmsh.blockchain.socket.base.AbstractBlockHandler;
import com.fmsh.blockchain.socket.body.RpcSimpleBlockBody;
import com.fmsh.blockchain.socket.client.PacketSender;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import com.fmsh.blockchain.socket.packet.NextBlockPacketBuilder;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;

/**
 * 已生成了新区块的全网广播
 * @author wuweifeng wrote on 2018/3/12.
 */
@Slf4j
public class GenerateCompleteRequestHandler extends AbstractBlockHandler<RpcSimpleBlockBody> {

    @Override
    public Class<RpcSimpleBlockBody> bodyClass() {
        return RpcSimpleBlockBody.class;
    }

    @Override
    public Object handler(BlockPacket packet, RpcSimpleBlockBody rpcBlockBody, ChannelContext channelContext) {
        log.info("收到来自于<" + rpcBlockBody.getAppId() + "><生成了新的Block>消息，block hash为[" + rpcBlockBody.getHash() +
                "]");

        //延迟2秒校验一下本地是否有该区块，如果没有，则发请求去获取新Block
        //延迟的目的是可能刚好自己也马上就要生成同样的Block了，就可以省一次请求
        TimerManager.schedule(() -> {
            Block block = ApplicationContextProvider.getBean(BlockManager.class).getBlockByHash(rpcBlockBody
                    .getHash());
            //本地有了
            if (block == null) {
                log.info("开始去获取别人的新区块");
                //在这里发请求，去获取group别人的新区块
                BlockPacket nextBlockPacket = NextBlockPacketBuilder.build();
                ApplicationContextProvider.getBean(PacketSender.class).sendGroup(nextBlockPacket);
            }
            return null;
        },2000);

        return null;
    }
}
