package com.fmsh.blockchain.socket.client;

import com.fmsh.blockchain.common.AppId;
import com.fmsh.blockchain.common.CommonUtil;
import com.fmsh.blockchain.common.Const;
import com.fmsh.blockchain.core.bean.Member;
import com.fmsh.blockchain.core.bean.MemberData;
import com.fmsh.blockchain.core.event.NodesConnectedEvent;
import com.fmsh.blockchain.socket.packet.BlockPacket;
import com.fmsh.blockchain.socket.packet.NextBlockPacketBuilder;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.tio.client.AioClient;
import org.tio.client.ClientGroupContext;
import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.utils.lock.SetWithLock;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static com.fmsh.blockchain.common.Const.GROUP_NAME;

/**
 * @author wuweifeng wrote on 2018/3/18.
 */
@Component
@Slf4j
public class ClientStarter {
    @Resource
    private ClientGroupContext clientGroupContext;
    @Resource
    private PacketSender packetSender;
    @Resource
    private RestTemplate restTemplate;
    @Value("${managerUrl}")
    private String managerUrl;
    @Value("${appId}")
    private String appId;
    @Value("${name}")
    private String name;
    @Value("${singeNode:false}")
    private Boolean singeNode;

    @Value("${block.port}")
    private Integer port;

    private Set<Node> nodes = new HashSet<>();
    
    // 节点连接状态
    private Map<String,Integer> nodesStatus = Maps.newConcurrentMap();
    private volatile boolean isNodesReady = false; // 节点是否已准备好

    /**
     * 从麦达区块链管理端获取已登记的各服务器ip
     * 隔5分钟去获取一次
     */
    @Scheduled(fixedRate = 300000)
    public void fetchOtherServer() {
        String localIp = CommonUtil.getLocalIp();
        log.info("本机IP：{}",localIp);
        try {
            MemberData memberData = null;
            try {
                //如果连不上服务器，就不让启动
                memberData = restTemplate.getForEntity(managerUrl + "member/memberData?name=" + name + "&appId=" + AppId
                                .value +
                                "&ip=" +
                                localIp,
                        MemberData.class).getBody();
            } catch (Exception e) {
                log.error("连接不上manager节点");
            }
            if (memberData == null) return;
            //合法的客户端
            if (memberData.getCode() == 0) {
                List<Member> memberList = memberData.getMembers();
                log.info("共有" + memberList.size() + "个成员需要连接：" + memberList.toString());

                nodes.clear();
                for (Member member : memberList) {
                    Node node = new Node(member.getIp(), member.getPort());
                    nodes.add(node);
                }
                try {
                    //开始尝试绑定到对方开启的server
                    bindServerGroup(nodes);
                } catch (Exception e) {
                    log.error("绑定节点异常: {}", e);
                }
            } else {
                log.error("不是合法有效的已注册的客户端");
                System.exit(0);
            }
        } catch (Exception e) {
            log.error("请先启动blockchain_manager服务，并配置appId等属性，只有合法联盟链成员才能启动该服务");
//            System.exit(0);
        }

    }

    /**
     * 每30秒群发一次消息，和别人对比最新的Block
     */
    @Scheduled(fixedRate = 30000)
    public void heartBeat() {
    	if(!isNodesReady)return;
        log.info("---------开始心跳包--------");
        BlockPacket blockPacket = NextBlockPacketBuilder.build();
        packetSender.sendGroup(blockPacket);
    }

    public void onNodesReady() {
        log.info("开始群发信息获取next Block");
        //在这里发请求，去获取group别人的新区块
        BlockPacket nextBlockPacket = NextBlockPacketBuilder.build();
        packetSender.sendGroup(nextBlockPacket);
    }

    /**
     * client在此绑定多个服务器，多个服务器为一个group，将来发消息时发给一个group。
     * 此处连接的server的ip需要和服务器端保持一致，服务器删了，这边也要踢出Group
     */
    private void bindServerGroup(Set<Node> serverNodes) {
        //当前已经连接的
        SetWithLock<ChannelContext> setWithLock = Aio.getAllChannelContexts(clientGroupContext);
        Lock lock2 = setWithLock.getLock().readLock();
        lock2.lock();
        try {
            Set<ChannelContext> set = setWithLock.getObj();
            //已连接的节点集合
            Set<Node> connectedNodes = set.stream().map(ChannelContext::getServerNode).collect(Collectors.toSet());

            //连接新增的，删掉已在管理端不存在的
            for (Node node : serverNodes) {
                if (!connectedNodes.contains(node)) {
                    connect(node);
                }
            }
            //删掉已经不存在
            for (ChannelContext channelContext : set) {
                Node node = channelContext.getServerNode();
                if (!serverNodes.contains(node)) {
                    Aio.remove(channelContext, "主动关闭" + node.getIp());
                }

            }
        } finally {
            lock2.unlock();
        }

    }

    private void connect(Node serverNode) {
        try {
            AioClient aioClient = new AioClient(clientGroupContext);
            log.info("开始绑定" + ":" + serverNode.toString());
            aioClient.asynConnect(serverNode);
        } catch (Exception e) {
            log.info("异常");
        }
    }
    
    @EventListener(NodesConnectedEvent.class)
    public void onConnected(NodesConnectedEvent connectedEvent){
    	ChannelContext channelContext = connectedEvent.getSource();
    	Node node = channelContext.getServerNode();
    	if (channelContext.isClosed()) {
            log.info("连接" + node.toString() + "失败");
            nodesStatus.put(node.getIp(), -1);
            return;
        }else{
        	log.info("连接" + node.toString() + "成功");
        	nodesStatus.put(node.getIp(), 1);
        	//绑group是将要连接的各个服务器节点做为一个group
        	Aio.bindGroup(channelContext, GROUP_NAME);

        	int csize = Aio.getAllChannelContexts(clientGroupContext).size();
        	if(csize >= pbftAgreeCount()){
        		synchronized (nodesStatus) {
        			if(!isNodesReady){
        				isNodesReady = true;
        				onNodesReady();
        			}
				}
        	}
        }
    }

    public int halfGroupSize() {
        SetWithLock<ChannelContext> setWithLock = clientGroupContext.groups.clients(clientGroupContext, GROUP_NAME);
        return setWithLock.getObj().size() / 2;
    }

    /**
     * pbft算法中拜占庭节点数量f，总节点数3f+1
     *
     * @return f
     */
    public int pbftSize() {
        //Group内共有多少个节点
        int total = nodes.size();
        int pbft = (total - 1) / 3;
        if (pbft <= 0) {
            pbft = 1;
        }
        //如果要单节点测试，此处返回值改为0
        if(singeNode) return 0;
        return pbft;
    }

    public int pbftAgreeCount() {
        return pbftSize() * 2 + 1;
    }
}
