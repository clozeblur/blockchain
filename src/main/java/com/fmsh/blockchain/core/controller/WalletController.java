package com.fmsh.blockchain.core.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.fmsh.blockchain.biz.block.*;
import com.fmsh.blockchain.biz.store.RocksDBUtils;
import com.fmsh.blockchain.biz.transaction.TXOutput;
import com.fmsh.blockchain.biz.transaction.Transaction;
import com.fmsh.blockchain.biz.transaction.UTXOSet;
import com.fmsh.blockchain.biz.util.Base58Check;
import com.fmsh.blockchain.biz.wallet.PairKeyPersist;
import com.fmsh.blockchain.biz.wallet.Wallet;
import com.fmsh.blockchain.biz.wallet.WalletUtils;
import com.fmsh.blockchain.common.CommonUtil;
import com.fmsh.blockchain.core.bean.BaseData;
import com.fmsh.blockchain.core.bean.UserData;
import com.fmsh.blockchain.core.body.BlockRequestBody;
import com.fmsh.blockchain.core.body.InstructionBody;
import com.fmsh.blockchain.core.manager.BlockManager;
import com.fmsh.blockchain.core.service.BlockService;
import com.fmsh.blockchain.core.service.InstructionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    @GetMapping("/clean")
    public void cleanWallet() {
        PairKeyPersist.setWalletMap(new HashMap<>());
    }

    @GetMapping("/init")
    @ResponseBody
    public String register(String username) throws Exception {
        if (StringUtils.isEmpty(username)) {
            return "username输入为空";
        }
        Wallet wallet = WalletUtils.getInstance().createWallet();
        String address = wallet.getAddress();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("username", username);
        map.add("address", address);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        BaseData base = restTemplate.postForObject(managerUrl + "user/register", request, BaseData.class);
        if (base.getCode() == 0) {
            Map<String, String> walletMap = new HashMap<>();
            walletMap.put("username", username);
            walletMap.put("address", address);
            PairKeyPersist.setWalletMap(walletMap);
            return "成功注册,您的地址为: " + PairKeyPersist.getWalletMap().get("address");
        }
        return "内存错误";
    }

    @GetMapping("/createBlockchain")
    @ResponseBody
    public String createBlockchain() throws Exception {
        if (PairKeyPersist.getWalletMap().get("username") == null) {
            return "您还没有初始化钱包";
        }

        Blockchain blockchain = blockchain(PairKeyPersist.getWalletMap().get("address"));
        return blockchain.toString();
    }

    @GetMapping("/getBalance")
    @ResponseBody
    public String getBalance(@RequestParam(required = false, value = "address") String addr) throws Exception {
//        if (PairKeyPersist.getWalletMap().get("username") == null) {
//            return "您还没有初始化钱包";
//        }
        String address = addr == null ? PairKeyPersist.getWalletMap().get("address") : addr;

        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(address);
        } catch (Exception e) {
            log.error("ERROR: invalid wallet address", e);
            throw new RuntimeException("ERROR: invalid wallet address", e);
        }

        // 得到公钥Hash值
        byte[] versionedPayload = Base58Check.base58ToBytes(address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);


        Blockchain blockchain = blockchain(address);
        UTXOSet utxoSet = new UTXOSet(blockchain);

        TXOutput[] txOutputs = utxoSet.findUTXOs(pubKeyHash);
        int balance = 0;
        if (txOutputs != null && txOutputs.length > 0) {
            for (TXOutput txOutput : txOutputs) {
                balance += txOutput.getValue();
            }
        }
        return "address: " + address + "  balance: " + balance;
    }

    @GetMapping("/send")
    @ResponseBody
    public String send(String sender, String receiver, int amount) throws Exception {
        UserData receiverData = restTemplate.getForEntity(managerUrl + "user/getUser?username=" + receiver, UserData.class).getBody();
        String to = receiverData.getUser().getAddress();

        UserData senderData = restTemplate.getForEntity(managerUrl + "user/getUser?username=" + sender, UserData.class).getBody();
        String from = senderData.getUser().getAddress();

        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(from);
        } catch (Exception e) {
            log.error("ERROR: sender address invalid ! address=" + from, e);
            throw new RuntimeException("ERROR: sender address invalid ! address=" + from, e);
        }
        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(to);
        } catch (Exception e) {
            log.error("ERROR: receiver address invalid ! address=" + to, e);
            throw new RuntimeException("ERROR: receiver address invalid ! address=" + to, e);
        }
        if (amount < 1) {
            log.error("ERROR: amount invalid ! amount=" + amount);
            throw new RuntimeException("ERROR: amount invalid ! amount=" + amount);
        }

        Blockchain blockchain = blockchain(from);

        // 新交易
        Transaction transaction = Transaction.newUTXOTransaction(from, to, amount, blockchain);

        Block block = newBlock(transaction, "from:" + from + ",to:" + to, from);
        return "发送成功";
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

    @GetMapping("/requestCoin")
    @ResponseBody
    public String requestCoin(@RequestParam("address") String address, @RequestParam("amount") Integer amount) throws Exception {
        Blockchain blockchain = blockchain(address);
        // 新交易
        Transaction transaction = Transaction.requestedCoinTX(address, amount);

        Block block = newBlock(transaction, "from:央行申请" + ",to:" + address, address);
        new UTXOSet(blockchain).update(block);

        RocksDBUtils.getInstance().putLastBlockHash(block.getHash());
        RocksDBUtils.getInstance().putBlock(block);
        return getBalance(address);
    }

    private Blockchain blockchain(String address) throws Exception {
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            // 创建 coinBase 交易
            String genesisCoinbaseData = "创世区块";
            Transaction coinbaseTX = Transaction.newCoinbaseTX(address, genesisCoinbaseData);

            Block genesisBlock = newBlock(coinbaseTX, genesisCoinbaseData, address);
        }
        return new Blockchain(lastBlockHash);
    }

    private Block newBlock(Transaction transaction, String data, String address) throws Exception {
        InstructionBody instructionBody = new InstructionBody();
        instructionBody.setOperation(Operation.ADD);
        instructionBody.setTable("message");
        instructionBody.setJson("{\"content\":\"" + data + "\"}");

        Wallet wallet = WalletUtils.getInstance().getWallet(address);
        instructionBody.setPublicKey(new String(wallet.getPublicKey()));
        instructionBody.setPrivateKey(wallet.getPrivateKey().toString());
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
