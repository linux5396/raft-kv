//package com.qgailab.raftkv.task;
//
//import com.qgailab.raftkv.common.NodeStatus;
//import com.qgailab.raftkv.common.Peer;
//import com.qgailab.raftkv.common.PeerSet;
//import com.qgailab.raftkv.entity.AppendLogParam;
//import com.qgailab.raftkv.entity.AppendResult;
//import com.qgailab.raftkv.rpc.Request;
//import com.qgailab.raftkv.rpc.Response;
//import com.qgailab.raftkv.rpc.RpcClient;
//import com.qgailab.raftkv.support.RaftThreadPool;
//import org.apache.commons.logging.LogFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Map;
//
///**
// * @author linxu
// * @date 2020/1/18
// * <tip>take care of yourself.everything is no in vain.</tip>
// */
//public class HeartBeatTask implements Runnable {
//    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatTask.class);
//    private volatile int status;
//    /**
//     * 心跳间隔
//     */
//    private final long heartBeatTick;
//    /**
//     * 上一次心跳时间
//     */
//    private long preHeartBeatTime = 0;
//    /**
//     * peerSet
//     */
//    private PeerSet peerSet;
//    /**
//     * 对于每一个服务器，需要发送给他的下一个日志条目的索引值（初始化为领导人最后索引值加一）
//     */
//    private Map<Peer, Long> nextIndexs;
//    RpcClient client;
//    long currentTerm;
//    String votedFor;
//
//    public HeartBeatTask(long heartBeatTick) {
//        this.heartBeatTick = heartBeatTick;
//    }
//
//    @Override
//    public void run() {
//        if (status != NodeStatus.LEADER) {
//            return;
//        }
//
//        long current = System.currentTimeMillis();
//        if (current - preHeartBeatTime < heartBeatTick) {
//            return;
//        }
//        LOGGER.info("===========HeartBeatTask=============");
//        for (Peer peer : peerSet.getPeersWithOutSelf()) {
//            LOGGER.info("Peer {} nextIndex={}", peer.getHostAndPort(), nextIndexs.get(peer));
//        }
//
//        preHeartBeatTime = System.currentTimeMillis();
//
//        // 心跳只关心 term 和 leaderID
//        for (Peer peer : peerSet.getPeersWithOutSelf()) {
//
//            AppendLogParam param = AppendLogParam.newBuilder()
//                    .entries(null)// 心跳,空日志.
//                    .leaderId(peerSet.getSelf().getHostAndPort())
//                    .serverId(peer.getHostAndPort())
//                    .term(currentTerm)
//                    .build();
//
//            Request<AppendLogParam> request = new Request<>(
//                    Request.A_ENTRIES,
//                    param,
//                    peer.getHostAndPort());
//
//            RaftThreadPool.execute(() -> {
//                try {
//                    //通过心跳任务去获取别人的任期，从而加强一致性
//                    Response response = client.send(request);
//                    AppendResult AppendLogResult = (AppendResult) response.getResult();
//                    long term = AppendLogResult.getTerm();
//
//                    if (term > currentTerm) {
//                        LOGGER.error("self will become follower, his term : {}, but my term : {}", term, currentTerm);
//                        currentTerm = term;
//                        votedFor = "";
//                        status = NodeStatus.FOLLOWER;
//                    }
//                } catch (Exception e) {
//                    LOGGER.error("HeartBeatTask RPC Fail, request URL : {} ", request.getUrl());
//                }
//            }, false);
//        }
//    }
//}
