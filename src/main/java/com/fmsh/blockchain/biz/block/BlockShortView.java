package com.fmsh.blockchain.biz.block;

import com.fmsh.blockchain.biz.transaction.TXInput;
import com.fmsh.blockchain.biz.transaction.TXOutput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/8/3 9:09
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BlockShortView {

    private String hash;

    /**
     * 生成该区块的公钥
     */
    private String publicKey;
    /**
     * 区块的序号
     */
    private int number;

    /**
     * 区块创建时间(单位:秒)
     */
    private long timestamp;

    private String json;

    /**
     * 交易输入
     */
    private TXInput[] inputs;
    /**
     * 交易输出
     */
    private TXOutput[] outputs;
}
