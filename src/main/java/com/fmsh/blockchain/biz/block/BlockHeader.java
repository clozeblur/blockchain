package com.fmsh.blockchain.biz.block;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/5 10:23
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BlockHeader implements Serializable {
    private static final long serialVersionUID = 8354919997185217326L;
    /**
     * 版本号
     */
    private int version;
    /**
     * 前一个区块的hash值
     */
    private String prevBlockHash;
    /**
     * merkle tree根节点hash
     */
    private String merkleRootHash;
    /**
     * 区块的序号
     */
    private int number;
    /**
     * 区块创建时间(单位:秒)
     */
    private long timestamp;
    /**
     * 工作量证明计数器
     */
    private long nonce;
}
