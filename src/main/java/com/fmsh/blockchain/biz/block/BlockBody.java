package com.fmsh.blockchain.biz.block;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/5 10:23
 * @Description: 区块体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BlockBody {
    /**
     * 交易信息
     */
    private List<Instruction> instructions;
}
