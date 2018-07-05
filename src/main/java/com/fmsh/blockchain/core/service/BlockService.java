package com.fmsh.blockchain.core.service;

import com.fmsh.blockchain.common.exception.TrustSDKException;
import com.fmsh.blockchain.core.body.BlockRequestBody;
import org.springframework.stereotype.Service;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/5 10:30
 * @Description:
 */
@Service
public class BlockService {

    /**
     * 校验指令集是否合法
     *
     * @param blockRequestBody
     *         指令集
     * @return 是否合法，为null则校验通过，其他则失败并返回原因
     */
    public String check(BlockRequestBody blockRequestBody) throws TrustSDKException {
        return null;
    }
}
