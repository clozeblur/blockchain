package com.fmsh.blockchain.core.manager;

import com.fmsh.blockchain.ApplicationContextProvider;
import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.biz.block.Blockchain;
import com.fmsh.blockchain.biz.store.RocksDBUtils;
import com.fmsh.blockchain.biz.transaction.UTXOSet;
import com.fmsh.blockchain.common.Constants;
import com.fmsh.blockchain.core.event.AddBlockEvent;
import com.fmsh.blockchain.core.event.DbSyncEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * block的本地存储
 * @author wuweifeng wrote on 2018/4/25.
 */
@Service
@Slf4j
public class BlockGenerator {

    @Resource
    private CheckerManager checkerManager;

    /**
     * 数据库里添加一个新的区块
     *
     * @param addBlockEvent
     *         addBlockEvent
     */
    @Order(1)
    @EventListener(AddBlockEvent.class)
    public synchronized void addBlock(AddBlockEvent addBlockEvent) {
        log.info("开始生成本地block");
        Block block = (Block) addBlockEvent.getSource();
        String hash = block.getHash();
        log.info("当前block的hash为: " + hash);
        //如果已经存在了，说明已经更新过该Block了
        if (RocksDBUtils.getInstance().getBlock(hash) != null) {
            return;
        }
        //校验区块
        if (checkerManager.check(block).getCode() != 0) {
            return;
        }

        Blockchain blockchain = new Blockchain(hash);
        UTXOSet utxoSet = new UTXOSet(blockchain);

        //如果没有上一区块，说明该块就是创世块
        if (block.getBlockHeader().getPrevBlockHash() == null) {
            log.info("该block判断为创世块");
            RocksDBUtils.getInstance().normalPut(Constants.KEY_FIRST_BLOCK, hash);

            utxoSet.reIndex();
        } else {
            log.info("保存映射 key=" + Constants.KEY_BLOCK_NEXT_PREFIX + block.getBlockHeader().getPrevBlockHash() + "  value=" + hash);
            //保存上一区块对该区块的key value映射
            RocksDBUtils.getInstance().normalPut(Constants.KEY_BLOCK_NEXT_PREFIX + block.getBlockHeader().getPrevBlockHash(), hash);
        }
        //存入rocksDB
        RocksDBUtils.getInstance().putBlock(block);
        //设置最后一个block的key value
        RocksDBUtils.getInstance().normalPut(Constants.KEY_LAST_BLOCK, hash);

        utxoSet.update(block);

        log.info("本地已生成新的Block");

        //同步到sqlite
        sqliteSync();
    }

    /**
     * sqlite根据block信息，执行sql
     */
    @Async
    public void sqliteSync() {
        //开始同步到sqlite
        ApplicationContextProvider.publishEvent(new DbSyncEvent(""));
    }
}
