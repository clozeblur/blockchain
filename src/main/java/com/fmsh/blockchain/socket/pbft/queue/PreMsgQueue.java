package com.fmsh.blockchain.socket.pbft.queue;

import cn.hutool.core.bean.BeanUtil;
import com.fmsh.blockchain.biz.block.Block;
import com.fmsh.blockchain.common.AppId;
import com.fmsh.blockchain.common.timer.TimerManager;
import com.fmsh.blockchain.core.event.AddBlockEvent;
import com.fmsh.blockchain.core.sqlite.SqliteManager;
import com.fmsh.blockchain.socket.pbft.VoteType;
import com.fmsh.blockchain.socket.pbft.event.MsgPrepareEvent;
import com.fmsh.blockchain.socket.pbft.msg.VoteMsg;
import com.fmsh.blockchain.socket.pbft.msg.VotePreMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * preprepare消息的存储，但凡收到请求生成Block的信息，都在这里处理
 *
 * @author wuweifeng wrote on 2018/4/23.
 */
@Component
@Slf4j
public class PreMsgQueue extends BaseMsgQueue {
    @Resource
    private SqliteManager sqliteManager;
    @Resource
    private PrepareMsgQueue prepareMsgQueue;
    @Resource
    private ApplicationEventPublisher eventPublisher;

    private ConcurrentHashMap<String, VotePreMsg> blockConcurrentHashMap = new ConcurrentHashMap<>();

    @Override
    protected void push(VoteMsg voteMsg) {
        //该队列里的是votePreMsg
        VotePreMsg votePreMsg = (VotePreMsg) voteMsg;
        String hash = votePreMsg.getHash();
        //避免收到重复消息
        if (blockConcurrentHashMap.get(hash) != null) {
            return;
        }
        //但凡是能进到该push方法的，都是通过基本校验的，但在并发情况下可能会相同number的block都进到投票队列中
        //需要对新进来的Vote信息的number进行校验，如果在比prepre阶段靠后的阶段中，已经出现了认证OK的同number的vote，则拒绝进入该队列
        if (prepareMsgQueue.otherConfirm(hash, voteMsg.getNumber())) {
            log.info("拒绝进入Prepare阶段，hash为" + hash);
            return;
        }
        // 检测脚本是否正常
        try {
			sqliteManager.tryExecute(votePreMsg.getBlock());
		} catch (Exception e) {
			if(!"00001".equals(e.getMessage())){
				// 执行异常
				return;
			}else{
				log.info("指令预校验执行成功！");
			}
		}
        
        //存入Pre集合中
        blockConcurrentHashMap.put(hash, votePreMsg);

        //加入Prepare行列，推送给所有人
        VoteMsg prepareMsg = new VoteMsg();
        BeanUtil.copyProperties(voteMsg, prepareMsg);
        prepareMsg.setVoteType(VoteType.PREPARE);
        prepareMsg.setAppId(AppId.value);
        eventPublisher.publishEvent(new MsgPrepareEvent(prepareMsg));
    }

    /**
     * 根据hash，得到内存中的Block信息
     *
     * @param hash
     *         hash
     * @return Block
     */
    public Block findByHash(String hash) {
        VotePreMsg votePreMsg = blockConcurrentHashMap.get(hash);
        if (votePreMsg != null) {
            return votePreMsg.getBlock();
        }
        return null;
    }

    /**
     * 新区块生成后，clear掉map中number比区块小的所有数据
     */
    @Order(3)
    @EventListener(AddBlockEvent.class)
    public void blockGenerated(AddBlockEvent addBlockEvent) {
        Block block = (Block) addBlockEvent.getSource();
        int number = block.getBlockHeader().getNumber();
        TimerManager.schedule(() -> {
            for (String key : blockConcurrentHashMap.keySet()) {
                if (blockConcurrentHashMap.get(key).getNumber() <= number) {
                    blockConcurrentHashMap.remove(key);
                }
            }
            return null;
        },2000);
    }
}
