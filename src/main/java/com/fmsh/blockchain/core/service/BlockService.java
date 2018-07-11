package com.fmsh.blockchain.core.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.biz.block.BlockBody;
import com.fmsh.blockchain.biz.block.BlockHeader;
import com.fmsh.blockchain.biz.block.Instruction;
import com.fmsh.blockchain.biz.transaction.MerkleTree;
import com.fmsh.blockchain.biz.util.Sha256;
import com.fmsh.blockchain.common.CommonUtil;
import com.fmsh.blockchain.common.exception.TrustSDKException;
import com.fmsh.blockchain.core.body.BlockRequestBody;
import com.fmsh.blockchain.core.manager.BlockManager;
import com.fmsh.blockchain.socket.body.RpcBlockBody;
import com.fmsh.blockchain.socket.client.PacketSender;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import com.fmsh.blockchain.socket.packet.PacketBuilder;
import com.fmsh.blockchain.socket.packet.PacketType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/5 10:30
 * @Description:
 */
@Service
public class BlockService {

    @Resource
    private InstructionService instructionService;
    @Value("${version}")
    private int version;
    @Resource
    private PacketSender packetSender;
    @Resource
    private BlockManager blockManager;

    /**
     * 校验指令集是否合法
     *
     * @param blockRequestBody
     *         指令集
     * @return 是否合法，为null则校验通过，其他则失败并返回原因
     */
    public String check(BlockRequestBody blockRequestBody) throws TrustSDKException {
        //TODO 此处可能需要校验publicKey的合法性
        if (blockRequestBody == null || blockRequestBody.getBlockBody() == null || StrUtil.isEmpty(blockRequestBody
                .getPublicKey())) {
            return "请求参数缺失";
        }
        List<Instruction> instructions = blockRequestBody.getBlockBody().getInstructions();
        if (CollectionUtil.isEmpty(instructions)) {
            return "指令信息不能为空";
        }

        for (Instruction instruction : instructions) {
            if (!StrUtil.equals(blockRequestBody.getPublicKey(), instruction.getPublicKey())) {
                return "指令内公钥和传来的公钥不匹配";
            }
//            if (!instructionService.checkSign(instruction)) {
//                return "签名校验不通过";
//            }
//            if (!instructionService.checkHash(instruction)) {
//                return "Hash校验不通过";
//            }
        }

        return null;
    }

    /**
     * 添加新的区块
     * @param blockRequestBody blockRequestBody
     * @return Block
     */
    public Block addBlock(BlockRequestBody blockRequestBody) {
        BlockBody blockBody = blockRequestBody.getBlockBody();
        List<Instruction> instructions = blockBody.getInstructions();
        List<String> hashList = instructions.stream().map(Instruction::getHash).collect(Collectors
                .toList());

        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setHashList(hashList);

        //计算所有指令的hashRoot
        blockHeader.setMerkleRootHash(Arrays.toString(new Block(null, null, blockBody).hashTransaction()));
        blockHeader.setPublicKey(blockRequestBody.getPublicKey());
        blockHeader.setTimestamp(CommonUtil.getNow());
        blockHeader.setVersion(version);
        blockHeader.setNumber(blockManager.getLastBlockNumber() + 1);
        blockHeader.setPrevBlockHash(blockManager.getLastBlockHash());
        Block block = new Block();
        block.setBlockBody(blockBody);
        block.setBlockHeader(blockHeader);
        block.setHash(Sha256.sha256(blockHeader.toString() + blockBody.toString()));

        BlockPacket blockPacket = new PacketBuilder<>().setType(PacketType.GENERATE_BLOCK_REQUEST).setBody(new
                RpcBlockBody(block)).build();

        //广播给其他人做验证
        packetSender.sendGroup(blockPacket);

        return block;
    }
}
