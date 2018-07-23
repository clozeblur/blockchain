package com.fmsh.blockchain.core.bean;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/23 10:55
 * @Description:
 */
public class Leader implements Serializable {

    private static final long serialVersionUID = -3894764726332439527L;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Member member;

    private Long timestamp;

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public String getTimestamp() {
        return sdf.format(new Date(timestamp));
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Leader{" +
                "member=" + member +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
