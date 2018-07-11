package com.fmsh.blockchain.biz.block;

import com.fmsh.blockchain.biz.transaction.MerkleTree;
import com.fmsh.blockchain.biz.transaction.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
public class Block {

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
     * <p> 创建创世区块 </p>
     *
     * @param coinbase
     * @return
     */
    public static Block newGenesisBlock(Transaction coinbase) {
        return Block.newBlock("", new Transaction[]{coinbase});
    }

    /**
     * <p> 创建新区块 </p>
     *
     * @param previousHash
     * @param transactions
     * @return
     */
    public static Block newBlock(String previousHash, Transaction[] transactions) {
        BlockHeader head = new BlockHeader();
        head.setPrevBlockHash(previousHash);

        List<Instruction> instructions = new ArrayList<>();
        for (Transaction transaction : transactions) {
            Instruction instruction = new Instruction();
            instruction.setTransaction(transaction);
            instructions.add(instruction);
        }
        BlockBody body = new BlockBody();
        body.setInstructions(instructions);

        return new Block("", head, body);
    }

    /**
     * 对区块中的交易信息进行Hash计算
     *
     * @return
     */
    public byte[] hashTransaction() {
        byte[][] txIdArrays = new byte[this.getBlockBody().getInstructions().size()][];
        for (int i = 0; i < this.getBlockBody().getInstructions().size(); i++) {
            txIdArrays[i] = this.getBlockBody().getInstructions().get(i).getTransaction().hash();
        }
        return new MerkleTree(txIdArrays).getRoot().getHash();
    }
}
