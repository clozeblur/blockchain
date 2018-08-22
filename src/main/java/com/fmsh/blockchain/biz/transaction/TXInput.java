package com.fmsh.blockchain.biz.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 交易输入
 *
 * @author wangwei
 * @date 2017/03/04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TXInput implements Serializable {

    private static final long serialVersionUID = -2665010317915030686L;
    /**
     * 交易Id的hash值
     */
    private byte[] txId;
    /**
     * 交易输出索引
     */
    private int txOutputIndex;
    /**
     * 签名
     */
    private byte[] signature;
    /**
     * 公钥
     */
    private byte[] pubKey;
}
