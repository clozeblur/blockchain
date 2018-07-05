package com.fmsh.blockchain.core.manager;

import cn.hutool.core.util.StrUtil;
import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.biz.store.RocksDBUtils;
import org.springframework.stereotype.Service;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/5 10:01
 * @Description:
 */
@Service
public class BlockManager {

    /**
     * 查找第一个区块
     *
     * @return 第一个Block
     */
    public Block getFirstBlock() {
        String firstBlockHash = RocksDBUtils.getInstance().getFirstBlockHash();
        if (StrUtil.isEmpty(firstBlockHash)) {
            return null;
        }
        return getBlockByHash(firstBlockHash);
    }

    /**
     * 获取最后一个区块信息
     *
     * @return 最后一个区块
     */
    public Block getLastBlock() {
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (StrUtil.isEmpty(lastBlockHash)) {
            return null;
        }
        return getBlockByHash(lastBlockHash);
    }

    /**
     * 获取最后一个区块的hash
     *
     * @return hash
     */
    public String getLastBlockHash() {
        Block block = getLastBlock();
        if (block != null) {
            return block.getHash();
        }
        return null;
    }

    /**
     * 获取最后一个block的number
     *
     * @return number
     */
    public int getLastBlockNumber() {
        Block block = getLastBlock();
        if (block != null) {
            return block.getBlockHeader().getNumber();
        }
        return 0;
    }

    /**
     * 获取某一个block的下一个Block
     *
     * @param block block
     * @return block
     */
    public Block getNextBlock(Block block) {
        if (block == null) {
            return getFirstBlock();
        }
        String nextHash = RocksDBUtils.getInstance().getNextBlockHash(block.getHash());
        if (nextHash == null) {
            return null;
        }
        return getBlockByHash(nextHash);
    }

    public Block getNextBlockByHash(String hash) {
        if (hash == null) {
            return getFirstBlock();
        }
        String nextHash = RocksDBUtils.getInstance().getNextBlockHash(hash);
        if (nextHash == null) {
            return null;
        }
        return getBlockByHash(nextHash);
    }

    public Block getBlockByHash(String hash) {
        return RocksDBUtils.getInstance().getBlock(hash);
    }
}
