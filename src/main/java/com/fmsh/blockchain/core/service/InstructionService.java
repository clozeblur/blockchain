package com.fmsh.blockchain.core.service;

import cn.hutool.core.bean.BeanUtil;
import com.fmsh.blockchain.biz.block.Instruction;
import com.fmsh.blockchain.biz.block.InstructionReverse;
import com.fmsh.blockchain.biz.block.Operation;
import com.fmsh.blockchain.common.CommonUtil;
import com.fmsh.blockchain.core.body.InstructionBody;
import org.springframework.stereotype.Service;

/**
 * 一条指令的service
 *
 * @author wuweifeng wrote on 2018/3/7.
 */
@Service
public class InstructionService {

    /**
     * 根据传来的body构建一条指令
     *
     * @param instructionBody
     *         body
     * @return Instruction
     */
    public Instruction build(InstructionBody instructionBody) {
        Instruction instruction = new Instruction();
        BeanUtil.copyProperties(instructionBody, instruction);
        if (Operation.ADD == instruction.getOperation()) {
            instruction.setInstructionId(CommonUtil.generateUuid());
        }
        return instruction;
    }

    /**
     * 根据一个指令，计算它的回滚时的指令。<p>
     * 如add table1 {id:xxx, name:"123"}，那么回滚时就是delete table1 {id:xxx}
     * 如delete table2 id2 oldJson:{id:xxx, name:"123"}，那么回滚时就是add table2 {id:xxx, name:"123"}。
     * 如update table3 id3 json:{id:xxx, name:"123"} oldJson:{id:xxx, name:"456"}
     * 注意，更新和删除时，原来的json都得有，不然没法回滚
     *
     * @param instruction
     *         instruction
     * @return 回滚指令
     */
    public InstructionReverse buildReverse(Instruction instruction) {
        InstructionReverse instructionReverse = new InstructionReverse();
        BeanUtil.copyProperties(instruction, instructionReverse);

        if (Operation.ADD == instruction.getOperation()) {
            instructionReverse.setOperation(Operation.DELETE);
        } else if (Operation.DELETE == instruction.getOperation()) {
            instructionReverse.setOperation(Operation.ADD);
        }

        return instructionReverse;
    }
}
