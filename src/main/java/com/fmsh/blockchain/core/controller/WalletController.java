package com.fmsh.blockchain.core.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.fmsh.blockchain.biz.block.*;
import com.fmsh.blockchain.biz.transaction.TXInput;
import com.fmsh.blockchain.biz.transaction.TXOutput;
import com.fmsh.blockchain.biz.transaction.Transaction;
import com.fmsh.blockchain.common.CommonUtil;
import com.fmsh.blockchain.core.body.BlockRequestBody;
import com.fmsh.blockchain.core.body.InstructionBody;
import com.fmsh.blockchain.core.manager.BlockManager;
import com.fmsh.blockchain.core.queue.RequestQueue;
import com.fmsh.blockchain.core.redis.LeaderPersist;
import com.fmsh.blockchain.core.service.BlockService;
import com.fmsh.blockchain.core.service.InstructionService;
import com.fmsh.blockchain.core.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
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
    private WalletService walletService;

    @Resource
    private BlockManager blockManager;

    @PostMapping("/checkBalance")
    public String checkBalance(@RequestBody Map<String, Object> map) {
//        if (!LeaderPersist.getIfLeader()) {
//            return restTemplate.postForEntity(LeaderPersist.getLeaderUrl() + "/wallet/checkBalance", map, String.class).getBody();
//        }
        String address = String.valueOf(map.get("address"));
        long balance = walletService.getBalance(address);
        return String.valueOf(balance);
    }

    @PostMapping("/requestCoin")
    @ResponseBody
    public String requestCoin(@RequestBody Map<String, Object> map) {
        if (!LeaderPersist.getIfLeader()) {
            return restTemplate.postForEntity(LeaderPersist.getLeaderUrl() + "/wallet/requestCoin", map, String.class).getBody();
        }
        return walletService.requestCoin(map);
    }

    @PostMapping("/doSend")
    @ResponseBody
    public String doSend(@RequestBody Map<String, Object> map) {
        if (!LeaderPersist.getIfLeader()) {
            return restTemplate.postForEntity(LeaderPersist.getLeaderUrl() + "/wallet/doSend", map, String.class).getBody();
        }
        return walletService.sendCoin(map);
    }

    @PostMapping("/queryRelatedBlocks")
    @ResponseBody
    public String queryRelatedBlocks(@RequestBody Map<String, Object> map) {
        return walletService.queryRelatedBlocks(map);
    }

    @GetMapping("/queryAllBlocks")
    public String queryAllBlocks() {
        return walletService.queryAllBlocks();
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
        if (!LeaderPersist.getIfLeader()) {
            return restTemplate.getForEntity(LeaderPersist.getLeaderUrl() + "/wallet/createBlockchain", Block.class).getBody();
        }
        String firstBlockHash = blockManager.getFirstBlockHash();
        if (!StringUtils.isBlank(firstBlockHash)) {
            throw new RuntimeException("区块链无法再次创造");
        }

        // 创建交易输入
        TXInput txInput = new TXInput(new byte[]{}, -1, null, null);
        // 创建交易输出
        TXOutput txOutput = new TXOutput(0, null);
        // 创建交易
        Transaction tx = new Transaction(null, Collections.singletonList(txInput),
                Collections.singletonList(txOutput), System.currentTimeMillis());
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
}
