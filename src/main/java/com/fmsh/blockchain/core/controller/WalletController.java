package com.fmsh.blockchain.core.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import com.fmsh.blockchain.biz.block.*;
import com.fmsh.blockchain.biz.store.RocksDBUtils;
import com.fmsh.blockchain.biz.transaction.TXInput;
import com.fmsh.blockchain.biz.transaction.TXOutput;
import com.fmsh.blockchain.biz.transaction.Transaction;
import com.fmsh.blockchain.biz.transaction.UTXOSet;
import com.fmsh.blockchain.biz.util.Base58Check;
import com.fmsh.blockchain.common.CommonUtil;
import com.fmsh.blockchain.common.exception.NotEnoughFundsException;
import com.fmsh.blockchain.core.bean.UserData;
import com.fmsh.blockchain.core.body.BlockRequestBody;
import com.fmsh.blockchain.core.body.InstructionBody;
import com.fmsh.blockchain.core.manager.BlockManager;
import com.fmsh.blockchain.core.redis.LeaderPersist;
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

    @Value("${block.port}")
    private String blockPort;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private BlockService blockService;

    @Resource
    private InstructionService instructionService;

    @Resource
    private BlockManager blockManager;

    private boolean ifNotLeader() {
        String localIp = CommonUtil.getLocalIp();
        Integer localPort = Integer.valueOf(blockPort);
        if (localIp == null) {
            log.error("can not find local ip address");
            return true;
        }
        return !(localIp.equals(LeaderPersist.getLeader().getMember().getIp()) && localPort.equals(LeaderPersist.getLeader().getMember().getPort()));
    }

    @PostMapping("/checkBalance")
    public String checkBalance(@RequestBody Map<String, Object> map) {
        if (ifNotLeader()) {
            return restTemplate.postForEntity(LeaderPersist.getLeaderUrl() + "/wallet/checkBalance", map, String.class).getBody();
        }

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

    @PostMapping("/requestCoin")
    @ResponseBody
    public String requestCoin(@RequestBody Map<String, Object> map) {
        if (ifNotLeader()) {
            return restTemplate.postForEntity(LeaderPersist.getLeaderUrl() + "/wallet/requestCoin", map, String.class).getBody();
        }

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

    @PostMapping("/doSend")
    @ResponseBody
    public String doSend(@RequestBody Map<String, Object> map) {
        if (ifNotLeader()) {
            return restTemplate.postForEntity(LeaderPersist.getLeaderUrl() + "/wallet/doSend", map, String.class).getBody();
        }

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

        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        Blockchain blockchain = new Blockchain(lastBlockHash);

        Transaction transaction;
        try {
            transaction = Transaction.newUTXOTransaction(from, to, pk, sk, amount, blockchain);
            if (!blockchain.verifyTransactions(transaction)) {
                return "invalid sign";
            }
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

    @GetMapping("/createBlockchain")
    @ResponseBody
    public Block createBlockchain() {
        if (ifNotLeader()) {
            return restTemplate.getForEntity(LeaderPersist.getLeaderUrl() + "/wallet/createBlockchain", Block.class).getBody();
        }
        // 创建交易输入
        TXInput txInput = new TXInput(new byte[]{}, -1, null, "origin".getBytes());
        // 创建交易输出
        TXOutput txOutput = new TXOutput(0, new byte[] {});
        // 创建交易
        Transaction tx = new Transaction(null, new TXInput[]{txInput},
                new TXOutput[]{txOutput}, System.currentTimeMillis());
        // 设置交易ID
        tx.setTxId(tx.hash());

        InstructionBody instructionBody = new InstructionBody();
        instructionBody.setOperation(Operation.ADD);
        instructionBody.setTable("message");
        instructionBody.setJson("{\"content\":\"" + "创世区块" + "\"}");
        instructionBody.setPublicKey(null);
        instructionBody.setPrivateKey(null);

        Instruction instruction = instructionService.build(instructionBody);
        instruction.setTransaction(tx);

        BlockRequestBody blockRequestBody = new BlockRequestBody();
        blockRequestBody.setPublicKey(instructionBody.getPublicKey());
        BlockBody blockBody = new BlockBody();
        blockBody.setInstructions(CollectionUtil.newArrayList(instruction));

        blockRequestBody.setBlockBody(blockBody);

        return blockService.addBlock(blockRequestBody);
    }

    private PrivateKey bytesToSk(byte[] bytes) {
        try {
            // 注册 BC Provider
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
            PKCS8EncodedKeySpec pKCS8EncodedKeySpec =new PKCS8EncodedKeySpec(bytes);
            return keyFactory.generatePrivate(pKCS8EncodedKeySpec);
        } catch (Exception e) {
            log.error("还原密钥异常");
            throw new RuntimeException("还原密钥异常");
        }
    }

    private String getAddress(String username) {
        UserData receiverData = restTemplate.getForEntity(managerUrl + "user/getUser?username=" + username, UserData.class).getBody();
        return receiverData.getUser().getAddress();
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
