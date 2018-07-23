package com.fmsh.blockchain.core.redis;

import com.fmsh.blockchain.core.bean.Leader;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/23 16:26
 * @Description:
 */
public class LeaderPersist {

    private LeaderPersist() {}

    private static Leader leader;

    public static Leader getLeader() {
        return leader;
    }

    public static void setLeader(Leader leader) {
        LeaderPersist.leader = leader;
    }

    public static String getLeaderUrl() {
        return "http://" + leader.getMember().getIp() + ":" + leader.getMember().getPort();
    }
}
