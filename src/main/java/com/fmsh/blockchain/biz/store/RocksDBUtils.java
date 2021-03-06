package com.fmsh.blockchain.biz.store;

import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.biz.transaction.TXOutput;
import com.fmsh.blockchain.biz.util.SerializeUtils;
import com.fmsh.blockchain.common.Constants;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.List;
import java.util.Map;

/**
 * 存储工具类
 *
 * @author wangwei
 * @date 2018/02/27
 */
@Slf4j
public class RocksDBUtils {

    /**
     * 区块链数据文件
     */
    private static final String DB_FILE = "blockchain.db";
    /**
     * 常量桶key
     */
    private static final String NORMAL_BUCKET_KEY = "normal";
    /**
     * 区块桶Key
     */
    private static final String BLOCKS_BUCKET_KEY = "blocks";
    /**
     * 链状态桶Key
     */
    private static final String CHAINSTATE_BUCKET_KEY = "chainstate";

    private volatile static RocksDBUtils instance;

    public static RocksDBUtils getInstance() {
        if (instance == null) {
            synchronized (RocksDBUtils.class) {
                if (instance == null) {
                    instance = new RocksDBUtils();
                }
            }
        }
        return instance;
    }

    private RocksDB db;

    /**
     * normal buckets
     */
    private Map<String, byte[]> normalBucket;

    /**
     * block buckets
     */
    private Map<String, byte[]> blocksBucket;
    /**
     * chainstate buckets
     */
    @Getter
    private Map<String, byte[]> chainstateBucket;

    private RocksDBUtils() {
        openDB();
        initNormalBucket();
        initBlockBucket();
        initChainStateBucket();
    }

    /**
     * 打开数据库
     */
    private void openDB() {
        try {
            db = RocksDB.open(DB_FILE);
        } catch (RocksDBException e) {
            log.error("Fail to open db ! ", e);
            throw new RuntimeException("Fail to open db ! ", e);
        }
    }

    /**
     * 初始化 normal 桶
     */
    @SuppressWarnings("unchecked")
    private void initNormalBucket() {
        try {
            byte[] normalBucketKey = SerializeUtils.serialize(NORMAL_BUCKET_KEY);
            byte[] normalBucketBytes = db.get(normalBucketKey);
            if (normalBucketBytes != null) {
                normalBucket = (Map) SerializeUtils.deserialize(normalBucketBytes);
            } else {
                normalBucket = Maps.newHashMap();
                db.put(normalBucketKey, SerializeUtils.serialize(normalBucket));
            }
        } catch (RocksDBException e) {
            log.error("Fail to init normal block bucket ! ", e);
            throw new RuntimeException("Fail to init normal block bucket ! ", e);
        }
    }

    /**
     * 初始化 blocks 数据桶
     */
    @SuppressWarnings("unchecked")
    private void initBlockBucket() {
        try {
            byte[] blockBucketKey = SerializeUtils.serialize(BLOCKS_BUCKET_KEY);
            byte[] blockBucketBytes = db.get(blockBucketKey);
            if (blockBucketBytes != null) {
                blocksBucket = (Map) SerializeUtils.deserialize(blockBucketBytes);
            } else {
                blocksBucket = Maps.newHashMap();
                db.put(blockBucketKey, SerializeUtils.serialize(blocksBucket));
            }
        } catch (RocksDBException e) {
            log.error("Fail to init block bucket ! ", e);
            throw new RuntimeException("Fail to init block bucket ! ", e);
        }
    }

    /**
     * 初始化 blocks 数据桶
     */
    @SuppressWarnings("unchecked")
    private void initChainStateBucket() {
        try {
            byte[] chainstateBucketKey = SerializeUtils.serialize(CHAINSTATE_BUCKET_KEY);
            byte[] chainstateBucketBytes = db.get(chainstateBucketKey);
            if (chainstateBucketBytes != null) {
                chainstateBucket = (Map) SerializeUtils.deserialize(chainstateBucketBytes);
            } else {
                chainstateBucket = Maps.newConcurrentMap();
                db.put(chainstateBucketKey, SerializeUtils.serialize(chainstateBucket));
            }
        } catch (RocksDBException e) {
            log.error("Fail to init chainstate bucket ! ", e);
            throw new RuntimeException("Fail to init chainstate bucket ! ", e);
        }
    }

    /**
     * 保存最新一个区块的Hash值
     *
     * @param tipBlockHash tipBlockHash
     */
    public void putLastBlockHash(String tipBlockHash) {
        try {
            normalBucket.put(Constants.KEY_LAST_BLOCK, SerializeUtils.serialize(tipBlockHash));
            db.put(SerializeUtils.serialize(BLOCKS_BUCKET_KEY), SerializeUtils.serialize(blocksBucket));
        } catch (RocksDBException e) {
            log.error("Fail to put last block hash ! tipBlockHash=" + tipBlockHash, e);
            throw new RuntimeException("Fail to put last block hash ! tipBlockHash=" + tipBlockHash, e);
        }
    }

    public String getFirstBlockHash() {
        byte[] firstBlockHashBytes = normalBucket.get(Constants.KEY_FIRST_BLOCK);
        return firstBlockHashBytes == null ? null : (String) SerializeUtils.deserialize(firstBlockHashBytes);
    }

    /**
     * 查询最新一个区块的Hash值
     *
     * @return last block hash
     */
    public String getLastBlockHash() {
        byte[] lastBlockHashBytes = normalBucket.get(Constants.KEY_LAST_BLOCK);
        if (lastBlockHashBytes != null) {
            return (String) SerializeUtils.deserialize(lastBlockHashBytes);
        }
        return "";
    }

    /**
     * 查询某一个block的下一个block
     *
     * @param hash block.hash
     * @return hash
     */
    public String getNextBlockHash(String hash) {
        if (StringUtils.isBlank(hash)) return getFirstBlockHash();
        byte[] nextBlockHash = normalBucket.get(Constants.KEY_BLOCK_NEXT_PREFIX + hash);
        return nextBlockHash == null ? null : (String) SerializeUtils.deserialize(nextBlockHash);
    }

    /**
     * 保存常规数据
     *
     * @param key key
     * @param value value
     */
    public void normalPut(String key, String value) {
        try {
            normalBucket.put(key, SerializeUtils.serialize(value));
            db.put(SerializeUtils.serialize(NORMAL_BUCKET_KEY), SerializeUtils.serialize(normalBucket));
        } catch (RocksDBException e) {
            log.error("Fail to put normal data ! key=" + key + " & value=" + value, e);
            throw new RuntimeException("Fail to put normal data ! key=" + key + " & value=" + value, e);
        }
    }

    /**
     * 保存区块
     *
     * @param block block
     */
    public void putBlock(Block block) {
        try {
            blocksBucket.put(block.getHash(), SerializeUtils.serializeObject(block));
            db.put(SerializeUtils.serialize(BLOCKS_BUCKET_KEY), SerializeUtils.serialize(blocksBucket));
        } catch (RocksDBException e) {
            log.error("Fail to put block ! block=" + block.toString(), e);
            throw new RuntimeException("Fail to put block ! block=" + block.toString(), e);
        }
    }

    /**
     * 查询区块
     *
     * @param blockHash block hash
     * @return block
     */
    public Block getBlock(String blockHash) {
        byte[] blockBytes = blocksBucket.get(blockHash);
        if (blockBytes != null) {
            return SerializeUtils.deserializeObject(blockBytes, Block.class);
        }
        log.warn("Fail to get block ! blockHash=" + blockHash);
        return null;
    }


    /**
     * 清空chainstate bucket
     */
    public void cleanChainStateBucket() {
        try {
            chainstateBucket.clear();
        } catch (Exception e) {
            log.error("Fail to clear chainstate bucket ! ", e);
            throw new RuntimeException("Fail to clear chainstate bucket ! ", e);
        }
    }

    /**
     * 保存UTXO数据
     *
     * @param key   交易ID
     * @param utxos UTXOs
     */
    public void putUTXOs(String key, List<TXOutput> utxos) {
        try {
            chainstateBucket.put(key, SerializeUtils.serializeList(utxos, TXOutput.class));
            db.put(SerializeUtils.serialize(CHAINSTATE_BUCKET_KEY), SerializeUtils.serialize(chainstateBucket));
        } catch (Exception e) {
            log.error("Fail to put UTXOs into chainstate bucket ! key=" + key, e);
            throw new RuntimeException("Fail to put UTXOs into chainstate bucket ! key=" + key, e);
        }
    }


    /**
     * 查询UTXO数据
     *
     * @param key 交易ID
     */
    public List<TXOutput> getUTXOs(String key) {
        byte[] utxosByte = chainstateBucket.get(key);
        if (utxosByte != null) {
            return SerializeUtils.deserializeList(utxosByte, TXOutput.class);
        }
        return null;
    }


    /**
     * 删除 UTXO 数据
     *
     * @param key 交易ID
     */
    public void deleteUTXOs(String key) {
        try {
            chainstateBucket.remove(key);
            db.put(SerializeUtils.serialize(CHAINSTATE_BUCKET_KEY), SerializeUtils.serialize(chainstateBucket));
        } catch (Exception e) {
            log.error("Fail to delete UTXOs by key ! key=" + key, e);
            throw new RuntimeException("Fail to delete UTXOs by key ! key=" + key, e);
        }
    }

    /**
     * 关闭数据库
     */
    @SuppressWarnings("unused")
    public void closeDB() {
        try {
            db.close();
        } catch (Exception e) {
            log.error("Fail to close db ! ", e);
            throw new RuntimeException("Fail to close db ! ", e);
        }
    }
}
