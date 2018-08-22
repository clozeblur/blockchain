package com.fmsh.blockchain.common;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/3 10:59
 * @Description: 常量值
 */
public interface Constants {
    /**
     * 最后一个区块hash的key，value就是最后一个区块的hash
     */
    String KEY_LAST_BLOCK = "key_last_block";
    /**
     * 第一个区块hash的key，value就是第一个区块的hash
     */
    String KEY_FIRST_BLOCK = "key_first_block";
    /**
     * 区块hash与区块本身的key value映射，key的前缀，如{key_block_xxxxxxx -> blockJson}
     */
    String KEY_BLOCK_HASH_PREFIX = "key_block_";

    String KEY_REQUEST_PREFIX = "key_request_";
    /**
     * 保存区块的hash和下一区块hash，key为hash，value为下一区块hash
     */
    String KEY_BLOCK_NEXT_PREFIX = "key_next_";
    /**
     * 每个表的权限存储key
     */
    String KEY_PERMISSION = "key_permission_";
}
