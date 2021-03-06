package com.fmsh.blockchain.core.service;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSONObject;
import com.fmsh.blockchain.biz.block.*;
import com.fmsh.blockchain.biz.store.RocksDBUtils;
import com.fmsh.blockchain.biz.transaction.TXOutput;
import com.fmsh.blockchain.biz.transaction.Transaction;
import com.fmsh.blockchain.biz.transaction.UTXOSet;
import com.fmsh.blockchain.biz.util.Base58Check;
import com.fmsh.blockchain.common.exception.NotEnoughFundsException;
import com.fmsh.blockchain.core.bean.UserData;
import com.fmsh.blockchain.core.body.BlockRequestBody;
import com.fmsh.blockchain.core.body.InstructionBody;
import com.fmsh.blockchain.core.collect.LocalCollect;
import com.fmsh.blockchain.core.collect.TXAbbr;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/8/9 10:33
 * @Description:
 */
@Service
@Slf4j
public class WalletService {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private BlockService blockService;

    @Resource
    private InstructionService instructionService;

    public String generateRequestCoinTransaction(Map<String, Object> map) {
        String username = String.valueOf(map.get("username"));

        Long amount = Long.valueOf(String.valueOf(map.get("amount")));

        if (amount < 1 || amount > (Long.MAX_VALUE - 1) / 2) {
            log.error("ERROR: amount invalid ! amount=" + amount);
            return "ERROR: amount invalid ! amount=" + amount;
        }

        String address = getAddress(username);

        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(address);
        } catch (Exception e) {
            log.error("ERROR: user address invalid ! address=" + address, e);
            return "ERROR: user address invalid ! address=" + address;
        }

        long balance = getBalance(address);
        long receiveAmount = LocalCollect.getInstance().getReceiveAmount(username);

        if (balance + amount + receiveAmount > (Long.MAX_VALUE - 1) / 2) {
            log.error("申请coin后的余额不能超过2的62次方");
            return "申请coin后的余额不能超过2的62次方";
        }

        Transaction tx = Transaction.requestedCoinTX(address, amount);
        TXAbbr txAbbr = new TXAbbr(tx, "", username, amount);
        LocalCollect.getInstance().addAbbr(txAbbr);
        return "success";
    }

    public String generateSendCoinTransaction(Map<String, Object> map) {
        String sender = String.valueOf(map.get("sender"));
        String receiver = String.valueOf(map.get("receiver"));

        byte[] pk = Base64.decode(String.valueOf(map.get("pk")), Charset.defaultCharset());

        byte[] skBytes = Base64.decode(String.valueOf(map.get("sk")), Charset.defaultCharset());

        BCECPrivateKey sk = (BCECPrivateKey) bytesToSk(skBytes);
        Long amount = Long.valueOf(String.valueOf(map.get("amount")));

        if (amount < 1 || amount > (Long.MAX_VALUE - 1) / 2) {
            log.error("ERROR: amount invalid ! amount=" + amount);
            return "ERROR: amount invalid ! amount=" + amount;
        }

        String from = getAddress(sender);

        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(from);
        } catch (Exception e) {
            log.error("ERROR: sender address invalid ! address=" + from, e);
            return "ERROR: sender address invalid ! address=" + from;
        }

        String to = getAddress(receiver);
        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(to);
        } catch (Exception e) {
            log.error("ERROR: receiver address invalid ! address=" + to, e);
            return "ERROR: receiver address invalid ! address=" + to;
        }

        long senderBalance = getBalance(from);
        long receiverBalance = getBalance(to);

        long sendAmount = LocalCollect.getInstance().getSendAmount(sender);
        long receiveAmount = LocalCollect.getInstance().getReceiveAmount(receiver);
        if (senderBalance - amount - sendAmount < 1 || receiverBalance + amount + receiveAmount > (Long.MAX_VALUE - 1) / 2) {
            log.error("发送方余额不足或接收方余额越界(超过2的62次方) senderBalance: {}, receiverBalance: {}, sendAmount: {}, receiveAmount: {}, amount: {}", senderBalance, receiverBalance, sendAmount, receiveAmount, amount);
        }

        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        Blockchain blockchain = new Blockchain(lastBlockHash);

        Transaction transaction;
        try {
            transaction = Transaction.newUTXOTransaction(from, to, pk, sk, amount, blockchain);
            if (!blockchain.verifyTransactions(transaction)) {
                log.error("invalid sign");
                return "invalid sign";
            }

            TXAbbr txAbbr = new TXAbbr(transaction, sender, receiver, amount);
            LocalCollect.getInstance().addAbbr(txAbbr);
            return "success";
        } catch (NotEnoughFundsException e) {
            log.error(e.getMsg());
            return "no enough fund";
        } catch (DecoderException | NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException e) {
            log.error(e.getMessage());
            return "signature error";
        }
    }

    public long getBalance(String address) {
        Blockchain blockchain = blockchain();
        UTXOSet utxoSet = new UTXOSet(blockchain);

        byte[] versionedPayload = Base58Check.base58ToBytes(address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);
        List<TXOutput> txOutputs = utxoSet.findUTXOs(pubKeyHash);

        long balance = 0;
        if (txOutputs != null && txOutputs.size() > 0) {
            for (TXOutput txOutput : txOutputs) {
                balance += txOutput.getValue();
            }
        }
        return balance;
    }

    public String queryRelatedBlocks(Map<String, Object> map) {
        String username = String.valueOf(map.get("username"));
        String address = getAddress(username);
        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(address);
        } catch (Exception e) {
            log.error("ERROR: user address invalid ! address=" + address, e);
            return "ERROR: user address invalid ! address=" + address;
        }

        byte[] pk = Base64.decode(String.valueOf(map.get("pk")), Charset.defaultCharset());
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        Blockchain blockchain = new Blockchain(lastBlockHash);

        List<Block> blocks = blockchain.findBlocks(pk, address);
        if (CollectionUtils.isEmpty(blocks)) return "EMPTY BLOCKS";

        return JSONObject.toJSONString(blocks);
    }

    public String queryAllBlocks() {
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        Blockchain blockchain = new Blockchain(lastBlockHash);
        return JSONObject.toJSONString(blockchain.findAll());
    }

    private PrivateKey bytesToSk(byte[] bytes) {
        try {
            // 注册 BC Provider
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
            PKCS8EncodedKeySpec pKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(bytes);
            return keyFactory.generatePrivate(pKCS8EncodedKeySpec);
        } catch (Exception e) {
            log.error("还原密钥异常");
            throw new RuntimeException("还原密钥异常");
        }
    }

    private String getAddress(String username) {
        UserData receiverData = restTemplate.getForEntity("http://192.168.95.131:8888/" + "user/getUser?username=" + username, UserData.class).getBody();
        return receiverData.getUser().getAddress();
    }

    private Blockchain blockchain() {
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        return new Blockchain(lastBlockHash);
    }

    public Block newBlock(List<TXAbbr> abbrList) {
        List<Instruction> instructions = new ArrayList<>();
        for (TXAbbr abbr : abbrList) {
            String data = abbr.getSender().equals("")
                    ? "from:央行申请 | amount=" + abbr.getAmount() + ",to:" + abbr.getReceiver()
                    : "sender:" + abbr.getSender() + " |发送了" + abbr.getAmount() + "到|receiver:" + abbr.getReceiver();
            InstructionBody instructionBody = new InstructionBody();
            instructionBody.setOperation(Operation.ADD);
            instructionBody.setTable("message");
            instructionBody.setJson("{\"content\":\"" + data + "\"}");
            Instruction instruction = instructionService.build(instructionBody);
            instruction.setTransaction(abbr.getTx());
            instructions.add(instruction);
        }

        BlockRequestBody blockRequestBody = new BlockRequestBody();
        BlockBody blockBody = new BlockBody();
        blockBody.setInstructions(instructions);

        blockRequestBody.setBlockBody(blockBody);
        log.info("##################################################################################");
        log.info("confirm start, current timestamp is {}", System.currentTimeMillis());
        return blockService.addBlock(blockRequestBody);
    }
}
