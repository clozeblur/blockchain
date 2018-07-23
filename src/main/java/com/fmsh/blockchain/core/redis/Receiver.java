package com.fmsh.blockchain.core.redis;

import com.alibaba.fastjson.JSONObject;
import com.fmsh.blockchain.core.bean.Leader;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/23 15:03
 * @Description:
 */
@Slf4j
public class Receiver {

    @SuppressWarnings("unchecked")
    public void receiveMessage(String message) {
        try {
            Leader leader = JSONObject.parseObject(message, Leader.class);
            if (leader != null) {
                LeaderPersist.setLeader(leader);
            }
        } catch (Exception e) {
            log.warn("配置信息更新推送读取异常...message: {}", message);
        }
    }
}
