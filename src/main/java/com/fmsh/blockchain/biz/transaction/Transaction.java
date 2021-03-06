package com.fmsh.blockchain.biz.transaction;

import com.fmsh.blockchain.biz.block.Blockchain;
import com.fmsh.blockchain.biz.util.BtcAddressUtils;
import com.fmsh.blockchain.biz.util.SerializeUtils;
import com.fmsh.blockchain.common.exception.NotEnoughFundsException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.*;
import java.util.*;

/**
 * 交易
 *
 * @author wangwei
 * @date 2017/03/04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Transaction implements Serializable {
    private static final long serialVersionUID = -6378642650700178914L;
    /**
     * 交易的Hash
     */
    private byte[] txId;
    /**
     * 交易输入
     */
    private List<TXInput> inputs;
    /**
     * 交易输出
     */
    private List<TXOutput> outputs;
    /**
     * 创建日期
     */
    private long createTime;

    /**
     * 计算交易信息的Hash值
     *
     * @return hash
     */
    public byte[] hash() {
        // 使用序列化的方式对Transaction对象进行深度复制
        byte[] serializeBytes = SerializeUtils.serializeObject(this);
        Transaction copyTx = SerializeUtils.deserializeObject(serializeBytes, Transaction.class);
        copyTx.setTxId(new byte[]{});
        return DigestUtils.sha256(SerializeUtils.serializeObject(copyTx));
    }

    public static Transaction requestedCoinTX(String to, long amount) {
        return oneWayCoin("request from ...", to, amount);
    }

    private static Transaction oneWayCoin(String data, String to, long amount) {
        if (StringUtils.isBlank(data)) {
            data = String.format("Reward to '%s'", to);
        }
        // 创建交易输入
        TXInput txInput = new TXInput(new byte[]{}, -1, null, null);
        // 创建交易输出
        TXOutput txOutput = TXOutput.newTXOutput(amount, to);
        // 创建交易
        Transaction tx = new Transaction(null, Collections.singletonList(txInput),
                Collections.singletonList(txOutput), System.currentTimeMillis());
        // 设置交易ID
        tx.setTxId(tx.hash());
        return tx;
    }

    /**
     * 是否为 Coinbase 交易
     *
     * @return bool
     */
    public boolean isCoinbase() {
        return this.getInputs().size() == 1
                && this.getInputs().get(0).getTxId().length == 0
                && this.getInputs().get(0).getTxOutputIndex() == -1;
    }

    /**
     * 从 from 向  to 支付一定的 amount 的金额
     *
     * @param from       支付钱包地址
     * @param to         收款钱包地址
     * @param amount     交易金额
     * @param blockchain 区块链
     * @return tx
     */
    public static Transaction newUTXOTransaction(String from, String to, byte[] publicKey, BCECPrivateKey privateKey, long amount, Blockchain blockchain)
            throws DecoderException, NotEnoughFundsException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
        byte[] pubKeyHash = BtcAddressUtils.ripeMD160Hash(publicKey);

        SpendableOutputResult result = new UTXOSet(blockchain).findSpendableOutputs(pubKeyHash, amount);
        long accumulated = result.getAccumulated();
        Map<String, int[]> unspentOuts = result.getUnspentOuts();

        if (accumulated < amount) {
            log.error("ERROR: Not enough funds ! accumulated=" + accumulated + ", amount=" + amount);
            throw new NotEnoughFundsException("ERROR: Not enough funds ! accumulated=" + accumulated + ", amount=" + amount);
        }
        Iterator<Map.Entry<String, int[]>> iterator = unspentOuts.entrySet().iterator();

        List<TXInput> txInputs = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<String, int[]> entry = iterator.next();
            String txIdStr = entry.getKey();
            int[] outIds = entry.getValue();
            byte[] txId = Hex.decodeHex(txIdStr.toCharArray());
            for (int outIndex : outIds) {
                txInputs.add(new TXInput(txId, outIndex, null, publicKey));
            }
        }

        List<TXOutput> txOutputs = new ArrayList<>();
        txOutputs.add(TXOutput.newTXOutput(amount, to));
        if (accumulated > amount) {
            txOutputs.add(TXOutput.newTXOutput((accumulated - amount), from));
        }

        Transaction newTx = new Transaction(null, txInputs, txOutputs, System.currentTimeMillis());
        newTx.setTxId(newTx.hash());

        // 进行交易签名
        blockchain.signTransaction(newTx, privateKey);
        return newTx;
    }

    /**
     * 创建用于签名的交易数据副本，交易输入的 signature 和 pubKey 需要设置为null
     *
     * @return tx
     */
    private Transaction trimmedCopy() {
        TXInput[] tmpTXInputs = new TXInput[this.getInputs().size()];
        for (int i = 0; i < this.getInputs().size(); i++) {
            TXInput txInput = this.getInputs().get(i);
            tmpTXInputs[i] = new TXInput(txInput.getTxId(), txInput.getTxOutputIndex(), null, null);
        }

        TXOutput[] tmpTXOutputs = new TXOutput[this.getOutputs().size()];
        for (int i = 0; i < this.getOutputs().size(); i++) {
            TXOutput txOutput = this.getOutputs().get(i);
            tmpTXOutputs[i] = new TXOutput(txOutput.getValue(), txOutput.getPubKeyHash());
        }

        return new Transaction(this.getTxId(), Arrays.asList(tmpTXInputs), Arrays.asList(tmpTXOutputs), this.getCreateTime());
    }


    /**
     * 签名
     *
     * @param privateKey 私钥
     * @param prevTxMap  前面多笔交易集合
     */
    public void sign(BCECPrivateKey privateKey, Map<String, Transaction> prevTxMap) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // coinbase 交易信息不需要签名，因为它不存在交易输入信息
        if (this.isCoinbase()) {
            return;
        }
        // 再次验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (TXInput txInput : this.getInputs()) {
            if (prevTxMap.get(Hex.encodeHexString(txInput.getTxId())) == null) {
                throw new RuntimeException("ERROR: Previous transaction is not correct");
            }
        }

        // 创建用于签名的交易信息的副本
        Transaction txCopy = this.trimmedCopy();

        Security.addProvider(new BouncyCastleProvider());
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME);
        ecdsaSign.initSign(privateKey);

        for (int i = 0; i < txCopy.getInputs().size(); i++) {
            TXInput txInputCopy = txCopy.getInputs().get(i);
            // 获取交易输入TxID对应的交易数据
            Transaction prevTx = prevTxMap.get(Hex.encodeHexString(txInputCopy.getTxId()));
            // 获取交易输入所对应的上一笔交易中的交易输出
            TXOutput prevTxOutput = prevTx.getOutputs().get(txInputCopy.getTxOutputIndex());
            txInputCopy.setPubKey(prevTxOutput.getPubKeyHash());
            txInputCopy.setSignature(null);
            // 得到要签名的数据，即交易ID
            txCopy.setTxId(txCopy.hash());
            txInputCopy.setPubKey(null);

            // 对整个交易信息仅进行签名，即对交易ID进行签名
            ecdsaSign.update(txCopy.getTxId());
            byte[] signature = ecdsaSign.sign();
            // 将整个交易数据的签名赋值给交易输入，因为交易输入需要包含整个交易信息的签名
            // 注意是将得到的签名赋值给原交易信息中的交易输入
            this.getInputs().get(i).setSignature(signature);
        }
    }


    /**
     * 验证交易信息
     *
     * @param prevTxMap 前面多笔交易集合
     * @return bool
     */
    public boolean verify(Map<String, Transaction> prevTxMap) throws Exception {
        // coinbase 交易信息不需要签名，也就无需验证
        if (this.isCoinbase()) {
            return true;
        }

        // 再次验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (TXInput txInput : this.getInputs()) {
            if (prevTxMap.get(Hex.encodeHexString(txInput.getTxId())) == null) {
                throw new RuntimeException("ERROR: Previous transaction is not correct");
            }
        }

        // 创建用于签名验证的交易信息的副本
        Transaction txCopy = this.trimmedCopy();

        Security.addProvider(new BouncyCastleProvider());
        ECParameterSpec ecParameters = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME);

        for (int i = 0; i < this.getInputs().size(); i++) {
            TXInput txInput = this.getInputs().get(i);
            // 获取交易输入TxID对应的交易数据
            Transaction prevTx = prevTxMap.get(Hex.encodeHexString(txInput.getTxId()));
            // 获取交易输入所对应的上一笔交易中的交易输出
            TXOutput prevTxOutput = prevTx.getOutputs().get(txInput.getTxOutputIndex());

            TXInput txInputCopy = txCopy.getInputs().get(i);
            txInputCopy.setSignature(null);
            txInputCopy.setPubKey(prevTxOutput.getPubKeyHash());
            // 得到要签名的数据，即交易ID
            txCopy.setTxId(txCopy.hash());
            txInputCopy.setPubKey(null);

            // 使用椭圆曲线 x,y 点去生成公钥Key
            BigInteger x = new BigInteger(1, Arrays.copyOfRange(txInput.getPubKey(), 1, 33));
            BigInteger y = new BigInteger(1, Arrays.copyOfRange(txInput.getPubKey(), 33, 65));
            ECPoint ecPoint = ecParameters.getCurve().createPoint(x, y);

            ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, ecParameters);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(txCopy.getTxId());
            if (!ecdsaVerify.verify(txInput.getSignature())) {
                return false;
            }
        }
        return true;
    }
}
