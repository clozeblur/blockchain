package com.fmsh.blockchain.core.body;

import com.fmsh.blockchain.biz.block.BlockBody;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/5 10:21
 * @Description: 生成Block时传参
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BlockRequestBody {
    private BlockBody blockBody;
}
