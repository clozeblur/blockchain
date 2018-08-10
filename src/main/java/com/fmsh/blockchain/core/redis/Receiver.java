package com.fmsh.blockchain.core.redis;

import com.alibaba.fastjson.JSONObject;
import com.fmsh.blockchain.common.CommonUtil;
import com.fmsh.blockchain.core.bean.Leader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/23 15:03
 * @Description:
 */
@Slf4j
public class Receiver {

    @Value("${block.port}")
    private String blockPort;

    public void receiveMessage(String message) {
        try {
            log.info("received message: {}", message);
            Leader leader = JSONObject.parseObject(message, Leader.class);
            if (leader != null) {
                log.info("current leader is : {}", leader.getMember());
                LeaderPersist.setLeader(leader);
                LeaderPersist.setIfLeader(ifLeader(leader));
                log.info("current is leader : {}", LeaderPersist.getIfLeader());
            }
        } catch (Exception e) {
            log.warn("配置信息更新推送读取异常...message: {}", message);
        }
    }

    private boolean ifLeader(Leader leader) {
        String localIp = CommonUtil.getLocalIp();
        Integer localPort = Integer.valueOf(blockPort);
        if (localIp == null) {
            log.error("can not find local ip address");
            return true;
        }
        return (localIp.equals(leader.getMember().getIp()) && localPort.equals(leader.getMember().getPort()));
    }
}
