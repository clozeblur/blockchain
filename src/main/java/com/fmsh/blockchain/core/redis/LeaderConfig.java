package com.fmsh.blockchain.core.redis;

import com.fmsh.blockchain.core.bean.Leader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/24 11:24
 * @Description:
 */
@Component
public class LeaderConfig {

    @Resource
    private RestTemplate restTemplate;

    @Value("${managerUrl}")
    private String managerUrl;

    @PostConstruct
    public void init() {
        Leader leader = restTemplate.getForEntity(managerUrl + "member/getLeader", Leader.class).getBody();
        LeaderPersist.setLeader(leader);
    }
}
