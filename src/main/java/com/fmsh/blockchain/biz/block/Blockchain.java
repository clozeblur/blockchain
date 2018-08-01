package com.fmsh.blockchain.biz.block;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.fmsh.blockchain.biz.store.RocksDBUtils;
import com.fmsh.blockchain.biz.transaction.TXInput;
import com.fmsh.blockchain.biz.transaction.TXOutput;
import com.fmsh.blockchain.biz.transaction.Transaction;
import com.fmsh.blockchain.biz.util.Base58Check;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p> 区块链 </p>
 *
 * @author wangwei
 * @date 2018/02/02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Blockchain {

    private String lastBlockHash;

    /**
     * 从 DB 中恢复区块链数据
     *
     * @return blockchain
     */
    @SuppressWarnings("unused")
    public static Blockchain initBlockchainFromDB() {
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (lastBlockHash == null) {
            throw new RuntimeException("ERROR: Fail to init blockchain from db. ");
        }
        return new Blockchain(lastBlockHash);
    }

    /**
     * 区块链迭代器
     */
    public class BlockchainIterator {

        private String currentBlockHash;

        private BlockchainIterator(String currentBlockHash) {
            this.currentBlockHash = currentBlockHash;
        }

        /**
         * 是否有下一个区块
         *
         * @return if exist next hash
         */
        boolean hashNext() {
            if (StringUtils.isBlank(currentBlockHash)) {
                return false;
            }
            Block lastBlock = RocksDBUtils.getInstance().getBlock(currentBlockHash);
            if (lastBlock == null) {
                return false;
            }
            // 创世区块直接放行
            return StringUtils.isBlank(lastBlock.getBlockHeader().getPrevBlockHash()) || RocksDBUtils.getInstance().getBlock(lastBlock.getBlockHeader().getPrevBlockHash()) != null;
        }


        /**
         * 返回区块
         *
         * @return next block
         */
        Block next() {
            Block currentBlock = RocksDBUtils.getInstance().getBlock(currentBlockHash);
            if (currentBlock != null) {
                this.currentBlockHash = currentBlock.getBlockHeader().getPrevBlockHash();
                return currentBlock;
            }
            return null;
        }
    }

    /**
     * 获取区块链迭代器
     *
     * @return iterator
     */
    private BlockchainIterator getBlockchainIterator() {
        return new BlockchainIterator(lastBlockHash);
    }

    /**
     * 查找所有的 unspent transaction outputs
     *
     * @return map
     */
    public Map<String, TXOutput[]> findAllUTXOs() {
        Map<String, int[]> allSpentTXOs = this.getAllSpentTXOs();
        Map<String, TXOutput[]> allUTXOs = Maps.newHashMap();
        // 再次遍历所有区块中的交易输出
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();

            List<Transaction> transactions = new ArrayList<>();
            for (Instruction instruction : block.getBlockBody().getInstructions()) {
                transactions.add(instruction.getTransaction());
            }
            for (Transaction transaction : transactions) {

                String txId = Hex.encodeHexString(transaction.getTxId());

                int[] spentOutIndexArray = allSpentTXOs.get(txId);
                TXOutput[] txOutputs = transaction.getOutputs();
                for (int outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                    if (spentOutIndexArray != null && ArrayUtils.contains(spentOutIndexArray, outIndex)) {
                        continue;
                    }
                    TXOutput[] UTXOArray = allUTXOs.get(txId);
                    if (UTXOArray == null) {
                        UTXOArray = new TXOutput[]{txOutputs[outIndex]};
                    } else {
                        UTXOArray = ArrayUtils.add(UTXOArray, txOutputs[outIndex]);
                    }
                    allUTXOs.put(txId, UTXOArray);
                }
            }
        }
        return allUTXOs;
    }

    /**
     * 从交易输入中查询区块链中所有已被花费了的交易输出
     *
     * @return 交易ID以及对应的交易输出下标地址
     */
    private Map<String, int[]> getAllSpentTXOs() {
        // 定义TxId ——> spentOutIndex[]，存储交易ID与已被花费的交易输出数组索引值
        Map<String, int[]> spentTXOs = Maps.newHashMap();
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();

            List<Transaction> transactions = new ArrayList<>();
            for (Instruction instruction : block.getBlockBody().getInstructions()) {
                transactions.add(instruction.getTransaction());
            }
            for (Transaction transaction : transactions) {
                // 如果是 coinbase 交易，直接跳过，因为它不存在引用前一个区块的交易输出
                if (transaction.isCoinbase()) {
                    continue;
                }
                for (TXInput txInput : transaction.getInputs()) {
                    String inTxId = Hex.encodeHexString(txInput.getTxId());
                    int[] spentOutIndexArray = spentTXOs.get(inTxId);
                    if (spentOutIndexArray == null) {
                        spentOutIndexArray = new int[]{txInput.getTxOutputIndex()};
                    } else {
                        spentOutIndexArray = ArrayUtils.add(spentOutIndexArray, txInput.getTxOutputIndex());
                    }
                    spentTXOs.put(inTxId, spentOutIndexArray);
                }
            }
        }
        return spentTXOs;
    }

    public List<Block> findAll() {
        List<Block> blocks = new ArrayList<>();
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();
            blocks.add(block);
        }
        return blocks;
    }

    public List<Block> findBlocks(byte[] publicKey, String address) {
        byte[] versionedPayload = Base58Check.base58ToBytes(address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);
        List<Block> blocks = new ArrayList<>();
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();
            boolean isRelated = false;
            if (StrUtil.equals(block.getBlockHeader().getPublicKey(), Base64.encode(publicKey))) {
                isRelated = true;
            }
            if (isRelated) {
                blocks.add(block);
                continue;
            }

            List<Transaction> transactions = new ArrayList<>();
            for (Instruction instruction : block.getBlockBody().getInstructions()) {
                transactions.add(instruction.getTransaction());
            }

            for (Transaction transaction : transactions) {
                // 如果是 coinbase 交易，直接跳过，因为它不存在引用前一个区块的交易输出
                if (transaction.isCoinbase()) {
                    continue;
                }

                for (TXInput txInput : transaction.getInputs()) {
                    if (Arrays.equals(txInput.getPubKey(), publicKey)) {
                        isRelated = true;
                        break;
                    }
                }
                if (!isRelated) {
                    for (TXOutput txOutput : transaction.getOutputs()) {
                        if (Arrays.equals(txOutput.getPubKeyHash(), pubKeyHash)) {
                            isRelated = true;
                            break;
                        }
                    }
                }
                if (isRelated) break;
            }

            if (isRelated) {
                blocks.add(block);
            }
        }
            return blocks;
    }


    /**
     * 依据交易ID查询交易信息
     *
     * @param txId 交易ID
     * @return tx
     */
    private Transaction findTransaction(byte[] txId) {
        for (BlockchainIterator iterator = this.getBlockchainIterator(); iterator.hashNext(); ) {
            Block block = iterator.next();

            List<Transaction> transactions = new ArrayList<>();
            for (Instruction instruction : block.getBlockBody().getInstructions()) {
                transactions.add(instruction.getTransaction());
            }
            for (Transaction tx : transactions) {
                if (Arrays.equals(tx.getTxId(), txId)) {
                    return tx;
                }
            }
        }
        throw new RuntimeException("ERROR: Can not found tx by txId ! ");
    }


    /**
     * 进行交易签名
     *
     * @param tx         交易数据
     * @param privateKey 私钥
     */
    public void signTransaction(Transaction tx, BCECPrivateKey privateKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        // 先来找到这笔新的交易中，交易输入所引用的前面的多笔交易的数据
        Map<String, Transaction> prevTxMap = Maps.newHashMap();
        for (TXInput txInput : tx.getInputs()) {
            Transaction prevTx = this.findTransaction(txInput.getTxId());
            prevTxMap.put(Hex.encodeHexString(txInput.getTxId()), prevTx);
        }
        tx.sign(privateKey, prevTxMap);
    }

    /**
     * 交易签名验证
     *
     * @param tx tx
     */
    public boolean verifyTransactions(Transaction tx) {
        if (tx.isCoinbase()) {
            return true;
        }
        Map<String, Transaction> prevTx = Maps.newHashMap();
        for (TXInput txInput : tx.getInputs()) {
            Transaction transaction = this.findTransaction(txInput.getTxId());
            prevTx.put(Hex.encodeHexString(txInput.getTxId()), transaction);
        }
        try {
            return tx.verify(prevTx);
        } catch (Exception e) {
            log.error("Fail to verify transaction ! transaction invalid ! ", e);
            throw new RuntimeException("Fail to verify transaction ! transaction invalid ! ", e);
        }
    }
}