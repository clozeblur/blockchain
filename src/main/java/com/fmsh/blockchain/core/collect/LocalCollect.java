package com.fmsh.blockchain.core.collect;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/8/13 14:17
 * @Description:
 */
public class LocalCollect {

    private volatile static LocalCollect instance;

    public static LocalCollect getInstance() {
        if (instance == null) {
            synchronized (LocalCollect.class) {
                if (instance == null) {
                    instance = new LocalCollect();
                }
            }
        }
        return instance;
    }

    private LocalCollect() {}

    private static List<TXAbbr> abbrList = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();

    public void addAbbr(TXAbbr abbr) {
        lock.lock();
        try {
            abbrList.add(abbr);
        } finally {
            lock.unlock();
        }
    }

    public List<TXAbbr> getAbbrList() {
        lock.lock();
        try {
            return new ArrayList<>(abbrList);
        } finally {
            abbrList.clear();
            lock.unlock();
        }
    }

    public long getSendAmount(String sender) {
        lock.lock();
        try {
            if (CollectionUtils.isEmpty(abbrList)) return 0L;
            return abbrList.stream().filter(abbr -> abbr.getSender().equals(sender)).mapToLong(TXAbbr::getAmount).sum();
        } finally {
            lock.unlock();
        }
    }

    public long getReceiveAmount(String receiver) {
        lock.lock();
        try {
            if (CollectionUtils.isEmpty(abbrList)) return 0L;
            return abbrList.stream().filter(abbr -> abbr.getReceiver().equals(receiver)).mapToLong(TXAbbr::getAmount).sum();
        } finally {
            lock.unlock();
        }
    }
}
