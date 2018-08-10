package com.fmsh.blockchain.core.redis;

import com.fmsh.blockchain.core.bean.Leader;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/23 16:26
 * @Description:
 */
public class LeaderPersist {

    private LeaderPersist() {}

    private static volatile Leader leader;

    private static volatile Boolean ifLeader = false;

    public static Leader getLeader() {
        return leader;
    }

    public static void setLeader(Leader leader) {
        LeaderPersist.leader = leader;
    }

    public static Boolean getIfLeader() {
        return ifLeader;
    }

    public static void setIfLeader(Boolean ifLeader) {
        LeaderPersist.ifLeader = ifLeader;
    }

    public static String getLeaderUrl() {
        return "http://" + leader.getMember().getIp() + ":" + serverPort(leader.getMember().getPort());
    }

    // default 8084
    private static Integer serverPort(Integer port) {
        if (port == 11000) return 8081;
        if (port == 12000) return 8082;
        if (port == 13000) return 8083;
        else return 8084;
    }
}
