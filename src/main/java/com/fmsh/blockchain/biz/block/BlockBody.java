package com.fmsh.blockchain.biz.block;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
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
public class BlockBody implements Serializable {
    private static final long serialVersionUID = -5571983054431440954L;
    /**
     * 交易信息
     */
    private List<Instruction> instructions;
}
