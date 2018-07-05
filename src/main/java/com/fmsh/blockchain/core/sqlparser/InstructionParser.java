package com.fmsh.blockchain.core.sqlparser;

import com.fmsh.blockchain.biz.block.InstructionBase;

/**
 * @author wuweifeng wrote on 2018/3/21.
 */
public interface InstructionParser {
    boolean parse(InstructionBase instructionBase);
}
