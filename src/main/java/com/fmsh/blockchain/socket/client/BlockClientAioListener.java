package com.fmsh.blockchain.socket.client;

import com.fmsh.blockchain.ApplicationContextProvider;
import com.fmsh.blockchain.core.event.NodesConnectedEvent;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.intf.ClientAioListener;
import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;

/**
 * client端对各个server连接的情况回调。</p>
 * 当某个server的心跳超时（2min）时，Aio会从group里remove掉该连接，需要在重新connect后重新加入group
 *
 * @author wuweifeng wrote on 2018/3/12.
 */
@Slf4j
public class BlockClientAioListener implements ClientAioListener {

    @Override
    public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) {
//        if (isConnected) {
//            logger.info("连接成功：server地址为-" + channelContext.getServerNode());
//            Aio.bindGroup(channelContext, Const.GROUP_NAME);
//        } else {
//            logger.info("连接失败：server地址为-" + channelContext.getServerNode());
//        }
        ApplicationContextProvider.publishEvent(new NodesConnectedEvent(channelContext));
    }

    @Override
    public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String s, boolean b) {
        log.info("连接关闭：server地址为-" + channelContext.getServerNode());
        Aio.unbindGroup(channelContext);
    }

    @Override
    public void onAfterDecoded(ChannelContext channelContext, Packet packet, int i) {

    }

    @Override
    public void onAfterReceivedBytes(ChannelContext channelContext, int i) {

    }

    @Override
    public void onAfterSent(ChannelContext channelContext, Packet packet, boolean b) {

    }

    @Override
    public void onAfterHandled(ChannelContext channelContext, Packet packet, long l) {

    }

}
