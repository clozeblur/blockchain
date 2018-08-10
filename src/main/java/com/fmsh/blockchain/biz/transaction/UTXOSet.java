package com.fmsh.blockchain.biz.transaction;

import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.biz.block.Blockchain;
import com.fmsh.blockchain.biz.block.Instruction;
import com.fmsh.blockchain.biz.store.RocksDBUtils;
import com.fmsh.blockchain.biz.util.SerializeUtils;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 未被花费的交易输出池
 *
 * @author wangwei
 * @date 2018/03/31
 */
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class UTXOSet {

    private Blockchain blockchain;

    /**
     * 寻找能够花费的交易
     *
     * @param pubKeyHash 钱包公钥Hash
     * @param amount     花费金额
     */
    public SpendableOutputResult findSpendableOutputs(byte[] pubKeyHash, long amount) {
        Map<String, int[]> unspentOuts = Maps.newHashMap();
        long accumulated = 0;
        Map<String, byte[]> chainstateBucket = RocksDBUtils.getInstance().getChainstateBucket();
        for (Map.Entry<String, byte[]> entry : chainstateBucket.entrySet()) {
            String txId = entry.getKey();
            List<TXOutput> txOutputs = SerializeUtils.deserializeList(entry.getValue(), TXOutput.class);

            for (int outId = 0; outId < txOutputs.size(); outId++) {
                TXOutput txOutput = txOutputs.get(outId);
                if (txOutput.isLockedWithKey(pubKeyHash) && accumulated < amount) {
                    accumulated += txOutput.getValue();

                    int[] outIds = unspentOuts.get(txId);
                    if (outIds == null) {
                        outIds = new int[]{outId};
                    } else {
                        outIds = ArrayUtils.add(outIds, outId);
                    }
                    unspentOuts.put(txId, outIds);
                    if (accumulated >= amount) {
                        break;
                    }
                }
            }
        }
        return new SpendableOutputResult(accumulated, unspentOuts);
    }


    /**
     * 查找钱包地址对应的所有UTXO
     *
     * @param pubKeyHash 钱包公钥Hash
     * @return outputs
     */
    public List<TXOutput> findUTXOs(byte[] pubKeyHash) {
        List<TXOutput> utxos = new ArrayList<>();
        Map<String, byte[]> chainstateBucket = RocksDBUtils.getInstance().getChainstateBucket();
        if (chainstateBucket.isEmpty()) {
            return utxos;
        }
        for (byte[] value : chainstateBucket.values()) {
            List<TXOutput> txOutputs = SerializeUtils.deserializeList(value, TXOutput.class);
            for (TXOutput txOutput : txOutputs) {
                if (txOutput.isLockedWithKey(pubKeyHash)) {
                    utxos.add(txOutput);
                }
            }
        }
        return utxos;
    }


    /**
     * 重建 UTXO 池索引
     */
    @Synchronized
    public void reIndex() {
        log.info("Start to reIndex UTXO set !");
        RocksDBUtils.getInstance().cleanChainStateBucket();
        Map<String, List<TXOutput>> allUTXOs = blockchain.findAllUTXOs();
        for (Map.Entry<String, List<TXOutput>> entry : allUTXOs.entrySet()) {
            RocksDBUtils.getInstance().putUTXOs(entry.getKey(), entry.getValue());
        }
        log.info("ReIndex UTXO set finished ! ");
    }

    /**
     * 更新UTXO池
     * <p>
     * 当一个新的区块产生时，需要去做两件事情：
     * 1）从UTXO池中移除花费掉了的交易输出；
     * 2）保存新的未花费交易输出；
     *
     * @param tipBlock 最新的区块
     */
    @Synchronized
    public void update(Block tipBlock) {
        if (tipBlock == null) {
            log.error("Fail to update UTXO set ! tipBlock is null !");
            throw new RuntimeException("Fail to update UTXO set ! ");
        }

        List<Transaction> transactions = new ArrayList<>();
        for (Instruction instruction : tipBlock.getBlockBody().getInstructions()) {
            transactions.add(instruction.getTransaction());
        }
        for (Transaction transaction : transactions) {

            // 根据交易输入排查出剩余未被使用的交易输出
            if (!transaction.isCoinbase()) {
                for (TXInput txInput : transaction.getInputs()) {
                    // 余下未被使用的交易输出
                    List<TXOutput> remainderUTXOs = new ArrayList<>();
                    String txId = Hex.encodeHexString(txInput.getTxId());
                    List<TXOutput> txOutputs = RocksDBUtils.getInstance().getUTXOs(txId);

                    if (txOutputs == null) {
                        continue;
                    }

                    for (int outIndex = 0; outIndex < txOutputs.size(); outIndex++) {
                        if (outIndex != txInput.getTxOutputIndex()) {
                            remainderUTXOs.add(txOutputs.get(outIndex));
                        }
                    }

                    // 没有剩余则删除，否则更新
                    if (remainderUTXOs.size() == 0) {
                        RocksDBUtils.getInstance().deleteUTXOs(txId);
                    } else {
                        RocksDBUtils.getInstance().putUTXOs(txId, remainderUTXOs);
                    }
                }
            }

            // 新的交易输出保存到DB中
            List<TXOutput> txOutputs = transaction.getOutputs();
            String txId = Hex.encodeHexString(transaction.getTxId());
            RocksDBUtils.getInstance().putUTXOs(txId, txOutputs);
        }

    }


}
