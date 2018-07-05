package com.fmsh.blockchain.biz.check;

import cn.hutool.core.util.StrUtil;
import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.biz.util.Sha256;
import com.fmsh.blockchain.common.exception.TrustSDKException;
import com.fmsh.blockchain.core.body.BlockRequestBody;
import com.fmsh.blockchain.core.manager.BlockManager;
import com.fmsh.blockchain.core.service.BlockService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/5 9:59
 * @Description:
 */
@Component
public class BlockChecker {

    @Resource
    private BlockManager blockManager;
    @Resource
    private BlockService blockService;

    /**
     * 比较目标区块和自己本地的区块num大小
     * @param block
     * 被比较的区块
     * @return
     * 本地与目标区块的差值
     */
    public int checkNum(Block block) {
        Block localBlock = blockManager.getLastBlock();
        int localNum = 0;
        if (localBlock != null) {
            localNum = localBlock.getBlockHeader().getNumber();
        }
        //本地区块+1等于新来的区块时才同意
        if (localNum + 1 == block.getBlockHeader().getNumber()) {
            //同意生成区块
            return 0;
        }
        //拒绝
        return -1;
    }

    /**
     * 校验hash，包括prevHash、内部hash（merkle tree root hash）
     * @param block
     * block
     * @return
     * 大于0合法
     */
    public int checkHash(Block block) {
        Block localLast = blockManager.getLastBlock();
        //创世块可以，或者新块的prev等于本地的last hash也可以
        if (localLast == null && block.getBlockHeader().getPrevBlockHash() == null) {
            return 0;
        }
        if (localLast != null && StrUtil.equals(localLast.getHash(), block.getBlockHeader().getPrevBlockHash())) {
            return 0;
        }
        return -1;
    }

    /**
     * 校验生成时间
     * @param block  block
     * @return block
     */
    public int checkTime(Block block) {
        Block localBlock = blockManager.getLastBlock();
        //新区块的生成时间比本地的还小
        if (localBlock != null && block.getBlockHeader().getTimestamp() <= localBlock.getBlockHeader().getTimestamp()) {
            //拒绝
            return -1;
        }
        return 0;
    }

    /**
     * 校验签名
     * @param block  block
     * @return block
     */
    public int checkSign(Block block) {
        if(!checkBlockHashSign(block)) {
            return -1;
        }
        return 0;
    }

    /**
     * 校验block，包括签名、hash、关联关系
     * @param block
     * @return
     */
    public String checkBlock(Block block) {
        if(!checkBlockHashSign(block)) return block.getHash();

        String preHash = block.getBlockHeader().getPrevBlockHash();
        if(preHash == null) return null;

        Block preBlock = blockManager.getBlockByHash(preHash);
        if(preBlock == null) return block.getHash();

        int localNum = preBlock.getBlockHeader().getNumber();
        //当前区块+1等于下一个区块时才同意
        if (localNum + 1 != block.getBlockHeader().getNumber()) {
            return block.getHash();
        }
        if(block.getBlockHeader().getTimestamp() <= preBlock.getBlockHeader().getTimestamp()) {
            return block.getHash();
        }


        return null;
    }

    /**
     * 检测区块签名及hash是否符合
     * @param block
     * @return
     */
    private boolean checkBlockHashSign(Block block) {
        BlockRequestBody blockRequestBody = new BlockRequestBody();
        blockRequestBody.setBlockBody(block.getBlockBody());
        blockRequestBody.setPublicKey(block.getBlockHeader().getPublicKey());
        try {
            if(blockService.check(blockRequestBody) != null) return false;
        } catch (TrustSDKException e) {
            return false;
        }

        String hash = Sha256.sha256(block.getBlockHeader().toString() + block.getBlockBody().toString());
        if(!StrUtil.equals(block.getHash(),hash)) return false;

        return true;
    }
}
