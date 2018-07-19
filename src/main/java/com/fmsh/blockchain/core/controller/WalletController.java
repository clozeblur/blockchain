package com.fmsh.blockchain.core.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
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
import com.fmsh.blockchain.core.manager.BlockManager;
import com.fmsh.blockchain.core.service.BlockService;
import com.fmsh.blockchain.core.service.InstructionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@RestController
@RequestMapping("/wallet")
@Slf4j
public class WalletController {

    @Value("${managerUrl}")
    private String managerUrl;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private BlockService blockService;

    @Resource
    private InstructionService instructionService;

    @Resource
    private BlockManager blockManager;

    @PostMapping("/checkBalance")
    public String checkBalance(@RequestBody Map<String, Object> map) {
        String address = String.valueOf(map.get("address"));
        Blockchain blockchain = blockchain();
        UTXOSet utxoSet = new UTXOSet(blockchain);

        byte[] versionedPayload = Base58Check.base58ToBytes(address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);

        TXOutput[] txOutputs = utxoSet.findUTXOs(pubKeyHash);
        int balance = 0;
        if (txOutputs != null && txOutputs.length > 0) {
            for (TXOutput txOutput : txOutputs) {
                balance += txOutput.getValue();
            }
        }
        return String.valueOf(balance);
    }

    @PostMapping("/doSend")
    @ResponseBody
    public String doSend(@RequestBody Map<String, Object> map) {
        String sender = String.valueOf(map.get("sender"));
        String receiver = String.valueOf(map.get("receiver"));

        byte[] pk = Base64.decode(String.valueOf(map.get("pk")), Charset.defaultCharset());

        byte[] skBytes = Base64.decode(String.valueOf(map.get("sk")), Charset.defaultCharset());

        BCECPrivateKey sk = (BCECPrivateKey) bytesToSk(skBytes);
        int amount = Integer.valueOf(String.valueOf(map.get("amount")));

        String from = getAddress(sender);

        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(from);
        } catch (Exception e) {
            log.error("ERROR: sender address invalid ! address=" + from, e);
            throw new RuntimeException("ERROR: sender address invalid ! address=" + from, e);
        }

        String to = getAddress(receiver);
        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(to);
        } catch (Exception e) {
            log.error("ERROR: receiver address invalid ! address=" + to, e);
            throw new RuntimeException("ERROR: receiver address invalid ! address=" + to, e);
        }

        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        Blockchain blockchain = new Blockchain(lastBlockHash);

        Transaction transaction = null;
        try {
            transaction = Transaction.newUTXOTransaction(from, to, pk, sk, amount, blockchain);
        } catch (NotEnoughFundsException e) {
            return e.getMsg();
        } catch (DecoderException | NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException e) {
            return e.getMessage();
        }

        Block block = newBlock(transaction, "from:" + from + "|sender:" + sender + "发送了" + amount + "到 to:" + to + "|receiver:" + receiver, pk, sk);
        return block.getHash();
    }

    @GetMapping("/getLastBlockHash")
    @ResponseBody
    public String getLastBlockHash() {
        return blockManager.getLastBlockHash();
    }

    @GetMapping("/getFirstBlockHash")
    public String getFirstBlockHash() {
        return blockManager.getFirstBlockHash();
    }

    @GetMapping("/getLastBlock")
    @ResponseBody
    public Block getLastBlock() {
        return blockManager.getLastBlock();
    }

    @GetMapping("/getFirstBlock")
    @ResponseBody
    public Block getFirstBlock() {
        return blockManager.getFirstBlock();
    }

    @PostMapping("/requestCoin")
    @ResponseBody
    public String requestCoin(@RequestBody Map<String, Object> map) throws Exception {
        String username = String.valueOf(map.get("username"));

        byte[] pk = Base64.decode(String.valueOf(map.get("pk")), Charset.defaultCharset());

        byte[] skBytes = Base64.decode(String.valueOf(map.get("sk")), Charset.defaultCharset());

        BCECPrivateKey sk = (BCECPrivateKey) bytesToSk(skBytes);

        Integer amount = Integer.valueOf(String.valueOf(map.get("amount")));

        String address = getAddress(username);

        Blockchain blockchain = blockchain();
        // 新交易
        Transaction transaction = Transaction.requestedCoinTX(address, amount);

        assert sk != null;
        Block block = newBlock(transaction, "from:央行申请" + ",to:" + address, pk, sk);
        new UTXOSet(blockchain).update(block);

        RocksDBUtils.getInstance().putLastBlockHash(block.getHash());
        RocksDBUtils.getInstance().putBlock(block);

        return block.getHash();
    }

    private PrivateKey bytesToSk(byte[] bytes) {
        try {
            // 注册 BC Provider
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
            PKCS8EncodedKeySpec pKCS8EncodedKeySpec =new PKCS8EncodedKeySpec(bytes);
            return keyFactory.generatePrivate(pKCS8EncodedKeySpec);
            //Log.d("get",filename+"　;　"+privateKey.toString() );
        } catch (Exception e) {
            log.error("还原密钥异常");
            throw new RuntimeException("还原密钥异常");
        }
    }

    private String getAddress(String username) {
        UserData receiverData = restTemplate.getForEntity(managerUrl + "user/getUser?username=" + username, UserData.class).getBody();
        return receiverData.getUser().getAddress();
    }

    @PostMapping("/generateBlock")
    public Block generateBlock(@RequestBody Map<String, Object> map) {
        InstructionBody instructionBody = JSONObject.parseObject(JSONObject.toJSONString(map.get("instructionBody")), InstructionBody.class);
        Transaction transaction = JSONObject.parseObject(JSONObject.toJSONString(map.get("transaction")), Transaction.class);

        Instruction instruction = instructionService.build(instructionBody);
        instruction.setTransaction(transaction);

        BlockRequestBody blockRequestBody = new BlockRequestBody();
        blockRequestBody.setPublicKey(instructionBody.getPublicKey());
        BlockBody blockBody = new BlockBody();
        blockBody.setInstructions(CollectionUtil.newArrayList(instruction));

        blockRequestBody.setBlockBody(blockBody);

        return blockService.addBlock(blockRequestBody);
    }

    private Blockchain blockchain() {
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        return new Blockchain(lastBlockHash);
    }

    private Block newBlock(Transaction transaction, String data, byte[] publicKey, BCECPrivateKey privateKey) {
        InstructionBody instructionBody = new InstructionBody();
        instructionBody.setOperation(Operation.ADD);
        instructionBody.setTable("message");
        instructionBody.setJson("{\"content\":\"" + data + "\"}");
        instructionBody.setPublicKey(Base64.encode(publicKey, Charset.defaultCharset()));
        instructionBody.setPrivateKey(Base64.encode(privateKey.getEncoded(), Charset.defaultCharset()));
        Instruction instruction = instructionService.build(instructionBody);
        instruction.setTransaction(transaction);

        BlockRequestBody blockRequestBody = new BlockRequestBody();
        blockRequestBody.setPublicKey(instructionBody.getPublicKey());
        BlockBody blockBody = new BlockBody();
        blockBody.setInstructions(CollectionUtil.newArrayList(instruction));

        blockRequestBody.setBlockBody(blockBody);

        return blockService.addBlock(blockRequestBody);
    }
}
