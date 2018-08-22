package com.fmsh.blockchain.core.collect;

import com.fmsh.blockchain.core.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/8/13 13:42
 * @Description:
 */
@Component
@Slf4j
public class TXSchedule {

    @Resource
    private WalletService walletService;

    @Scheduled(fixedDelay = 5000)
    public void blockSchedule() {
        log.info("##########################################schedule start##########################################");
        List<TXAbbr> abbrList = LocalCollect.getInstance().getAbbrList();
        log.info("list size: {}", abbrList.size());
        if (!CollectionUtils.isEmpty(abbrList)) {
            walletService.newBlock(abbrList);
        }
        log.info("##########################################schedule   end##########################################");
    }
}
