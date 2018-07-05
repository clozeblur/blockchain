package com.fmsh.blockchain.core.controller;

import com.fmsh.blockchain.common.exception.TrustSDKException;
import com.fmsh.blockchain.core.bean.BaseData;
import com.fmsh.blockchain.core.bean.ResultGenerator;
import com.fmsh.blockchain.core.service.PairKeyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author wuweifeng wrote on 2018/3/7.
 */
@RestController
@RequestMapping("/pairKey")
public class PairKeyController {
    @Resource
    private PairKeyService pairKeyService;

    /**
     * 生成公钥私钥
     */
    @GetMapping("/random")
    public BaseData generate() throws TrustSDKException {
         return ResultGenerator.genSuccessResult(pairKeyService.generate());
    }
}
