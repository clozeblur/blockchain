package com.fmsh.blockchain.biz.transaction;

import com.fmsh.blockchain.biz.util.Base58Check;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * 交易输出
 *
 * @author wangwei
 * @date 2017/03/04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TXOutput {

    /**
     * 数值
     */
    private long value;
    /**
     * 公钥Hash
     */
    private byte[] pubKeyHash;

    /**
     * 创建交易输出
     *
     * @param value value
     * @param address address
     * @return output
     */
    public static TXOutput newTXOutput(long value, String address) {
        // 反向转化为 byte 数组
        byte[] versionedPayload = Base58Check.base58ToBytes(address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);
        return new TXOutput(value, pubKeyHash);
    }

    /**
     * 检查交易输出是否能够使用指定的公钥
     *
     * @param pubKeyHash pk hash
     * @return bool
     */
    public boolean isLockedWithKey(byte[] pubKeyHash) {
        return Arrays.equals(this.getPubKeyHash(), pubKeyHash);
    }

}
