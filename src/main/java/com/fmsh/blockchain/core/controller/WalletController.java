package com.fmsh.blockchain.core.controller;

import com.fmsh.blockchain.biz.block.*;
import com.fmsh.blockchain.core.manager.BlockManager;
import com.fmsh.blockchain.core.redis.LeaderPersist;
import com.fmsh.blockchain.core.service.WalletService;
import lombok.extern.slf4j.Slf4j;
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

//        return walletService.requestCoin(map);
        return walletService.generateRequestCoinTransaction(map);
    }

    @PostMapping("/doSend")
    @ResponseBody
    public String doSend(@RequestBody Map<String, Object> map) {
        if (!LeaderPersist.getIfLeader()) {
            return restTemplate.postForEntity(LeaderPersist.getLeaderUrl() + "/wallet/doSend", map, String.class).getBody();
        }
//        return walletService.sendCoin(map);
        return walletService.generateSendCoinTransaction(map);
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
}
