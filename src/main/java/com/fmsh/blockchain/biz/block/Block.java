package com.fmsh.blockchain.biz.block;

import com.fmsh.blockchain.biz.transaction.MerkleTree;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * 区块
 *
 * @author wangwei
 * @date 2018/02/02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Block implements Serializable {

    private static final long serialVersionUID = 3306914466108386966L;
    /**
     * 区块hash值
     */
    private String hash;
    /**
     * 区块头
     */
    private BlockHeader blockHeader;
    /**
     * 区块体
     */
    private BlockBody blockBody;

    /**
     * 对区块中的交易信息进行Hash计算
     *
     * @return bytes
     */
    public byte[] hashTransaction() {
        byte[][] txIdArrays = new byte[this.getBlockBody().getInstructions().size()][];
        for (int i = 0; i < this.getBlockBody().getInstructions().size(); i++) {
            txIdArrays[i] = this.getBlockBody().getInstructions().get(i).getTransaction().hash();
        }
        return new MerkleTree(txIdArrays).getRoot().getHash();
    }
}
