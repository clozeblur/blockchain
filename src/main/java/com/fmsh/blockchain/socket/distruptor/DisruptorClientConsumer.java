package com.fmsh.blockchain.socket.distruptor;

import cn.hutool.core.util.StrUtil;
import com.fmsh.blockchain.common.AppId;
import com.fmsh.blockchain.socket.base.AbstractBlockHandler;
import com.fmsh.blockchain.socket.body.BaseBody;
import com.fmsh.blockchain.socket.distruptor.base.BaseEvent;
import com.fmsh.blockchain.socket.distruptor.base.MessageConsumer;
import com.fmsh.blockchain.socket.handler.client.FetchBlockResponseHandler;
import com.fmsh.blockchain.socket.handler.client.NextBlockResponseHandler;
import com.fmsh.blockchain.socket.handler.client.TotalBlockInfoResponseHandler;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import com.fmsh.blockchain.socket.packet.PacketType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tio.utils.json.Json;

import java.util.HashMap;
import java.util.Map;

/**
 * 所有server发来的消息都在这里处理
 * @author wuweifeng wrote on 2018/4/20.
 */
@Component
@Slf4j
public class DisruptorClientConsumer implements MessageConsumer {
    private static Map<Byte, AbstractBlockHandler<?>> handlerMap = new HashMap<>();

    static {
        handlerMap.put(PacketType.TOTAL_BLOCK_INFO_RESPONSE, new TotalBlockInfoResponseHandler());
        handlerMap.put(PacketType.NEXT_BLOCK_INFO_RESPONSE, new NextBlockResponseHandler());
        handlerMap.put(PacketType.FETCH_BLOCK_INFO_RESPONSE, new FetchBlockResponseHandler());
    }

    @Override
    public void receive(BaseEvent baseEvent) throws Exception {
        BlockPacket blockPacket = baseEvent.getBlockPacket();
        Byte type = blockPacket.getType();
        AbstractBlockHandler<?> blockHandler = handlerMap.get(type);
        if (blockHandler == null) {
            return;
        }

        //消费消息
        BaseBody baseBody = Json.toBean(new String(blockPacket.getBody()), BaseBody.class);
        //logger.info("收到来自于<" + baseBody.getAppId() + ">针对msg<" + baseBody.getResponseMsgId() + ">的回应");

        String appId = baseBody.getAppId();
        if (StrUtil.equals(AppId.value, appId)) {
            //是本机
            //return;
        }

        blockHandler.handler(blockPacket, baseEvent.getChannelContext());
    }
}
