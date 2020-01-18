package com.qgailab.raftkv.impl;

import com.qgailab.raftkv.*;
import com.qgailab.raftkv.client.ClientKVAck;
import com.qgailab.raftkv.client.ClientKVReq;

import com.qgailab.raftkv.common.NodeInfo;
import com.qgailab.raftkv.common.NodeStatus;
import com.qgailab.raftkv.common.Peer;
import com.qgailab.raftkv.common.PeerSet;
import com.qgailab.raftkv.dynamic.memship.ClusterMembershipChanges;
import com.qgailab.raftkv.dynamic.memship.Result;
import com.qgailab.raftkv.entity.*;
import com.qgailab.raftkv.exception.RaftRemotingException;
import com.qgailab.raftkv.rpc.*;
import com.qgailab.raftkv.support.LongConvert;
import com.qgailab.raftkv.support.RaftThreadPool;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 抽象机器节点, 初始为 follower, 角色随时变化.
 *
 * @author linxu
 */
@Data
public class DefaultNode<T> implements Node<T>, LifeCycle, ClusterMembershipChanges {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNode.class);

    /**
     * 选举时间间隔基数 15S
     */
    public volatile long electionTime = 15 * 1000;
    /**
     * 上一次选举时间
     */
    public volatile long preElectionTime = 0;

    /**
     * 上次一心跳时间戳
     */
    public volatile long preHeartBeatTime = 0;
    /**
     * 心跳间隔基数 5S
     */
    public final long heartBeatTick = 5 * 1000;


    private HeartBeatTask heartBeatTask = new HeartBeatTask();
    private ElectionTask electionTask = new ElectionTask();
    private ReplicationFailQueueConsumer replicationFailQueueConsumer = new ReplicationFailQueueConsumer();

    private LinkedBlockingQueue<ReplicationFailModel> replicationFailQueue = new LinkedBlockingQueue<>(2048);


    /**
     * 节点当前状态
     *
     * @see NodeStatus
     */
    public volatile int status = NodeStatus.FOLLOWER;

    public PeerSet peerSet;



    /* ============ 所有服务器上持久存在的 ============= */

    /**
     * 服务器最后一次知道的任期号（初始化为 0，持续递增）
     */
    volatile long currentTerm = 0;
    /**
     * 在当前获得选票的候选人的 Id
     */
    volatile String votedFor;
    /**
     * 日志条目集；每一个条目包含一个用户状态机执行的指令，和收到时的任期号
     */
    LogModule logModule;



    /* ============ 所有服务器上经常变的 ============= */

    /**
     * 已知的最大的已经被提交的日志条目的索引值
     */
    volatile long commitIndex;

    /**
     * 最后被应用到状态机的日志条目索引值（初始化为 0，持续递增)
     */
    volatile long lastApplied = 0;

    /* ========== 在领导人里经常改变的(选举后重新初始化) ================== */

    /**
     * 对于每一个服务器，需要发送给他的下一个日志条目的索引值（初始化为领导人最后索引值加一）
     */
    Map<Peer, Long> nextIndexs;

    /**
     * 对于每一个服务器，已经复制给他的日志的最高索引值
     */
    Map<Peer, Long> matchIndexs;



    /* ============================== */

    public volatile boolean started;

    public NodeInfo config;

    public static RpcServer RPC_SERVER;

    public RpcClient rpcClient = new DefaultRpcClient();

    public StateMachine stateMachine;

    /* ============================== */

    /**
     * 一致性模块实现
     */
    Consensus consensus;
    /**
     * 成员变化模块的实现
     */
    ClusterMembershipChanges delegate;


    /* ============================== */

    private DefaultNode() {
    }

    public static DefaultNode getInstance() {
        return DefaultNodeLazyHolder.INSTANCE;
    }


    private static class DefaultNodeLazyHolder {

        private static final DefaultNode INSTANCE = new DefaultNode();
    }

    //TODO 配置参数采用配置文件注入
    @Override
    public void init() throws Throwable {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            RPC_SERVER.start();

            consensus = new DefaultConsensus(this);
            delegate = new ClusterMembershipChangesImpl(this);
            //延时启动心跳
            RaftThreadPool.scheduleWithFixedDelay(heartBeatTask, 500);
            //第一次启动的选举任务延时为6S
            RaftThreadPool.scheduleAtFixedRate(electionTask, 6000, 500);
            //复制
            RaftThreadPool.execute(replicationFailQueueConsumer);

            LogEntry logEntry = logModule.getLast();
            if (logEntry != null) {
                currentTerm = logEntry.getTerm();
            }

            started = true;

            LOGGER.info("start success, selfId : {} ", peerSet.getSelf());
        }
    }

    @Override
    public void loadNodeInfo(NodeInfo config) {
        this.config = config;
        stateMachine = DefaultStateMachine.getInstance();
        logModule = DefaultLogModule.getInstance();
        peerSet = PeerSet.getInstance();
        //TODO
        for (String s : config.getPeerAddress()) {
            Peer peer = new Peer(s);
            if (s.equals("localhost:" + config.getSelfPort())) {
                peer = new Peer("localhost:" + config.getSelfPort());
                peerSet.setSelf(peer);
            }
            peerSet.addPeer(peer);
        }
        //启动RPC服务
        RPC_SERVER = new DefaultRpcServer(config.selfPort, this);
    }

    /**
     * 处理投票的请求
     *
     * @param param 投票参数
     * @return 投票结果
     */
    @Override
    public VoteResult handlerRequestVote(RequestVoteParam param) {
        LOGGER.warn("handlerRequestVote will be invoke, param info : {}", param);
        return consensus.requestVote(param);
    }

    /**
     * 处理日志追加请求
     */
    @Override
    public AppendResult handlerAppendEntries(AppendLogParam param) {
        if (param.getEntries() != null) {
            LOGGER.warn("node receive node {} append log entry,  content = {}", param.getLeaderId(), param.getEntries());
        }
        return consensus.appendEntries(param);
    }

    /**
     * 重定向请求给leader
     *
     * @param request 客户端请求
     * @return 响应
     */
    @SuppressWarnings("unchecked")
    @Override
    public ClientKVAck redirect(ClientKVReq request) {
        Request<ClientKVReq> r = Request.newBuilder().
                obj(request).url(peerSet.getLeader().getHostAndPort()).cmd(Request.CLIENT_REQ).build();
        Response response = rpcClient.send(r);
        return (ClientKVAck) response.getResult();
    }

    /**
     * 客户端的每一个请求都包含一条被复制状态机执行的指令。
     * 领导人把这条指令作为一条新的日志条目附加到日志中去，然后并行的发起附加条目 RPCs 给其他的服务器，让他们复制这条日志条目。
     * 当这条日志条目被安全的复制（下面会介绍），领导人会应用这条日志条目到它的状态机中然后把执行的结果返回给客户端。
     * 如果跟随者崩溃或者运行缓慢，再或者网络丢包，
     * 领导人会不断的重复尝试附加日志条目 RPCs （尽管已经回复了客户端）直到所有的跟随者都最终存储了所有的日志条目。
     *
     */
    @Override
    public synchronized ClientKVAck handlerClientRequest(ClientKVReq request) {

        LOGGER.warn("handlerClientRequest handler {} operation,  and key : [{}], value : [{}]",
                ClientKVReq.Type.value(request.getType()), request.getKey(), request.getValue());
        //NodeStatus不是leader，进行重定向
        if (status != NodeStatus.LEADER) {
            LOGGER.warn("I not am leader , only invoke redirect method, leader address : {}, my address : {}",
                    peerSet.getLeader(), peerSet.getSelf().getHostAndPort());
            //call redirect
            return redirect(request);
        }
        //get请求
        if (request.getType() == ClientKVReq.GET) {
            LogEntry logEntry = stateMachine.get(request.getKey());
            if (logEntry != null) {
                return new ClientKVAck(logEntry.getCommand());
            }
            return new ClientKVAck(null);
        }
        //TODO del
        if (request.getType() == ClientKVReq.DEL) {
            stateMachine.delString(request.getKey());
        }
        //TODO here add leader solve proposal.
        if (request.getType() == ClientKVReq.PROPOSE) {
            //empty impl
        }
        //put below
        //build entry
        LogEntry logEntry = LogEntry.newBuilder()
                .command(Command.newBuilder().
                        key(request.getKey()).
                        value(request.getValue()).
                        build())
                .term(currentTerm)
                .build();

        // 预提交到本地日志
        //here write ahead log(my local log)
        //如果后面发生日志复制达不到一半的条件，则执行撤销
        logModule.write(logEntry);
        LOGGER.info("write logModule success, logEntry info : {}, log index : {}", logEntry, logEntry.getIndex());
        //下面采用原子量进行复制的统计
        final AtomicInteger success = new AtomicInteger(0);
        //异步
        List<Future<Boolean>> futureList = new CopyOnWriteArrayList<>();

        int count = 0;
        //  复制到其他机器
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            // TODO check self and RaftThreadPool
            count++;
            // 并行发起 RPC 复制.
            futureList.add(replication(peer, logEntry));
        }

        CountDownLatch latch = new CountDownLatch(futureList.size());
        List<Boolean> resultList = new CopyOnWriteArrayList<>();

        getRPCAppendResult(futureList, latch, resultList);

        try {
            latch.await(4000, MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //成功复制的数量
        for (Boolean aBoolean : resultList) {
            if (aBoolean) {
                success.incrementAndGet();
            }
        }
        // 如果存在一个满足N > commitIndex的 N，并且大多数的matchIndex[i] ≥ N成立，
        // 并且log[N].term == currentTerm成立，那么令 commitIndex 等于这个 N （5.3 和 5.4 节）
        List<Long> matchIndexList = new ArrayList<>(matchIndexs.values());
        // 小于 2, 没有意义
        int median = 0;
        if (matchIndexList.size() >= 2) {
            Collections.sort(matchIndexList);
            median = matchIndexList.size() >> 1;
        }
        Long N = matchIndexList.get(median);
        if (N > commitIndex) {
            LogEntry entry = logModule.read(N);
            if (entry != null && entry.getTerm() == currentTerm) {
                commitIndex = N;
            }
        }
        //  响应客户端(成功一半)
        if (success.get() >= (count >> 1)) {
            // 更新
            commitIndex = logEntry.getIndex();
            //  应用到状态机
            getStateMachine().apply(logEntry);
            lastApplied = commitIndex;

            LOGGER.info("success apply local state machine,  logEntry info : {}", logEntry);
            // 返回成功.
            return ClientKVAck.ok();
        } else {
            // 回滚已经提交的日志.
            logModule.removeOnStartIndex(logEntry.getIndex());
            LOGGER.warn("fail apply local state  machine,  logEntry info : {}", logEntry);
            // TODO 不应用到状态机,但已经记录到日志中.由定时任务从重试队列取出,然后重复尝试,当达到条件时,应用到状态机.
            // 这里应该返回错误, 因为没有成功复制过半机器.
            return ClientKVAck.fail();
        }
    }

    private void getRPCAppendResult(List<Future<Boolean>> futureList, CountDownLatch latch, List<Boolean> resultList) {
        for (Future<Boolean> future : futureList) {
            RaftThreadPool.execute(() -> {
                try {
                    resultList.add(future.get(3000, MILLISECONDS));
                } catch (CancellationException | TimeoutException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    resultList.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }
    }


    /**
     * 异步复制到其他机器
     */
    public Future<Boolean> replication(Peer peer, LogEntry entry) {

        return RaftThreadPool.submit(() -> {
            long start = System.currentTimeMillis(), end = start;
            // 20 秒重试时间
            while (end - start < 20 * 1000L) {
                AppendLogParam AppendLogParam = new AppendLogParam();
                AppendLogParam.setTerm(currentTerm);
                AppendLogParam.setServerId(peer.getHostAndPort());
                AppendLogParam.setLeaderId(peerSet.getSelf().getHostAndPort());
                AppendLogParam.setLeaderCommit(commitIndex);

                // 以我这边为准, 这个行为通常是成为 leader 后,首次进行 RPC 才有意义.
                Long nextIndex = nextIndexs.get(peer);
                LinkedList<LogEntry> logEntries = new LinkedList<>();
                if (entry.getIndex() >= nextIndex) {
                    for (long i = nextIndex; i <= entry.getIndex(); i++) {
                        LogEntry l = logModule.read(i);
                        if (l != null) {
                            logEntries.add(l);
                        }
                    }
                } else {
                    logEntries.add(entry);
                }
                // 最小的那个日志.
                LogEntry preLog = getPreLog(logEntries.getFirst());
                AppendLogParam.setPreLogTerm(preLog.getTerm());
                AppendLogParam.setPrevLogIndex(preLog.getIndex());
                AppendLogParam.setEntries(logEntries.toArray(new LogEntry[0]));
                //构建RPC
                Request request = Request.newBuilder()
                        .cmd(Request.A_ENTRIES)
                        .obj(AppendLogParam)
                        .url(peer.getHostAndPort())
                        .build();

                try {
                    Response response = getRpcClient().send(request);
                    if (response == null) {
                        return false;
                    }
                    AppendResult result = (AppendResult) response.getResult();
                    if (result != null && result.isSuccess()) {
                        LOGGER.info("append follower entry success , follower=[{}], entry=[{}]", peer, AppendLogParam.getEntries());
                        // update 这两个追踪值
                        nextIndexs.put(peer, entry.getIndex() + 1);
                        matchIndexs.put(peer, entry.getIndex());
                        return true;
                    } else if (result != null) {
                        // 对方比我大
                        if (result.getTerm() > currentTerm) {
                            LOGGER.warn("follower [{}] term [{}]  more than self, and my term = [{}], so, I will become follower",
                                    peer, result.getTerm(), currentTerm);
                            currentTerm = result.getTerm();
                            // 对方的任期比我大，则我成为跟随者。
                            status = NodeStatus.FOLLOWER;
                            return false;
                        } // 没我大, 却失败了,说明 index 不对.或者 term 不对.
                        else {
                            // 递减
                            if (nextIndex == 0) {
                                nextIndex = 1L;
                            }
                            nextIndexs.put(peer, nextIndex - 1);
                            LOGGER.warn("follower {} nextIndex not match, will reduce nextIndex and retry RPC append, nextIndex : [{}]", peer.getHostAndPort(),
                                    nextIndex);
                            // 重来, 直到成功.
                        }
                    }

                    end = System.currentTimeMillis();

                } catch (Exception e) {
                    LOGGER.warn(e.getMessage(), e);
                    // TODO 要不要放队列重试
//                        ReplicationFailModel model =  ReplicationFailModel.newBuilder()
//                            .callable(this)
//                            .logEntry(entry)
//                            .peer(peer)
//                            .offerTime(System.currentTimeMillis())
//                            .build();
//                        replicationFailQueue.offer(model);
                    return false;
                }
            }
            // 超时了,没办法了
            return false;
        });

    }

    private LogEntry getPreLog(LogEntry logEntry) {
        LogEntry entry = logModule.read(logEntry.getIndex() - 1);

        if (entry == null) {
            LOGGER.warn("get perLog is null , parameter logEntry : {}", logEntry);
            entry = LogEntry.newBuilder().index(0L).term(0).command(null).build();
        }
        return entry;
    }


    class ReplicationFailQueueConsumer implements Runnable {

        /**
         * 一分钟
         */
        long intervalTime = 1000 * 60;

        @Override
        public void run() {
            for (; ; ) {

                try {
                    ReplicationFailModel model = replicationFailQueue.take();
                    if (status != NodeStatus.LEADER) {
                        // 应该清空?
                        replicationFailQueue.clear();
                        continue;
                    }
                    LOGGER.warn("replication Fail Queue Consumer take a task, will be retry replication, content detail : [{}]", model.logEntry);
                    long offerTime = model.offerTime;
                    if (System.currentTimeMillis() - offerTime > intervalTime) {
                        LOGGER.warn("replication Fail event Queue maybe full or handler slow");
                    }

                    Callable callable = model.callable;
                    Future<Boolean> future = RaftThreadPool.submit(callable);
                    Boolean r = future.get(3000, MILLISECONDS);
                    // 重试成功.
                    if (r) {
                        // 可能有资格应用到状态机.
                        tryApplyStateMachine(model);
                    }

                } catch (InterruptedException e) {
                    // ignore
                } catch (ExecutionException | TimeoutException e) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
    }

    private void tryApplyStateMachine(ReplicationFailModel model) {

        String success = stateMachine.getString(model.successKey);
        stateMachine.setString(model.successKey, String.valueOf(Integer.valueOf(success) + 1));

        String count = stateMachine.getString(model.countKey);

        if (Integer.valueOf(success) >= Integer.valueOf(count) / 2) {
            stateMachine.apply(model.logEntry);
            stateMachine.delString(model.countKey, model.successKey);
        }
    }


    @Override
    public void destroy() throws Throwable {
        RPC_SERVER.stop();
    }


    /**
     * this is the core of the election.
     * 1. 在转变成候选人后就立即开始选举过程
     * 自增当前的任期号（currentTerm）
     * 给自己投票
     * 重置选举超时计时器
     * 发送请求投票的 RPC 给其他所有服务器
     * 2. 如果接收到大多数服务器的选票，那么就变成领导人
     * 3. 如果接收到来自新的领导人的附加日志 RPC，转变成跟随者
     * 4. 如果选举过程超时，再次发起一轮选举
     */
    class ElectionTask implements Runnable {

        @Override
        public void run() {

            if (status == NodeStatus.LEADER) {
                return;
            }

            long current = System.currentTimeMillis();
            // 基于 RAFT 的随机时间,解决冲突.
            electionTime = electionTime + ThreadLocalRandom.current().nextInt(50);
            if (current - preElectionTime < electionTime) {
                return;
            }
            status = NodeStatus.CANDIDATE;
            LOGGER.error("node {} will become CANDIDATE and start election leader, current term : [{}], LastEntry : [{}]",
                    peerSet.getSelf(), currentTerm, logModule.getLast());

            preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150;

            currentTerm = currentTerm + 1;
            // 推荐自己.
            votedFor = peerSet.getSelf().getHostAndPort();
            /**
             * 获取除了自己的其他人的列表
             */
            List<Peer> peers = peerSet.getPeersWithOutSelf();

            ArrayList<Future> futureArrayList = new ArrayList<>();

            LOGGER.info("peerList size : {}, peer list content : {}", peers.size(), peers);

            // 发送请求
            for (Peer peer : peers) {

                futureArrayList.add(RaftThreadPool.submit(() -> {
                    long lastTerm = 0L;
                    LogEntry last = logModule.getLast();
                    if (last != null) {
                        lastTerm = last.getTerm();
                    }
                    /**
                     * 构造自身的投票参数
                     * TODO 这里对于多个伙伴，就会重复创建多次，可以外提一层。
                     */
                    RequestVoteParam param = RequestVoteParam.newBuilder().
                            term(currentTerm).
                            candidateId(peerSet.getSelf().getHostAndPort()).
                            lastLogIndex(LongConvert.convert(logModule.getLastIndex())).
                            lastLogTerm(lastTerm).
                            build();
                    /**
                     * 构造投票请求
                     */
                    Request request = Request.newBuilder()
                            .cmd(Request.R_VOTE)
                            .obj(param)
                            .url(peer.getHostAndPort())
                            .build();

                    try {
                        @SuppressWarnings("unchecked")
                        Response<VoteResult> response = getRpcClient().send(request);
                        return response;

                    } catch (RaftRemotingException e) {
                        //TODO 这些地方可以添加对于节点剔除的判断
                        LOGGER.error("ElectionTask RPC Fail , URL : " + request.getUrl());
                        return null;
                    }
                }));
            }

            AtomicInteger success2 = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(futureArrayList.size());

            LOGGER.info("futureArrayList.size() : {}", futureArrayList.size());
            // 等待结果.
            for (Future future : futureArrayList) {
                RaftThreadPool.submit(() -> {
                    try {

                        @SuppressWarnings("unchecked")
                        Response<VoteResult> response = (Response<VoteResult>) future.get(3000, MILLISECONDS);
                        if (response == null) {
                            return -1;
                        }
                        boolean isVoteGranted = response.getResult().isVoteGranted();
                        /**
                         * 成功被投票，则增加
                         * 否则更新任期
                         */
                        if (isVoteGranted) {
                            success2.incrementAndGet();
                        } else {
                            // 更新自己的任期.
                            long resTerm = response.getResult().getTerm();
                            if (resTerm >= currentTerm) {
                                currentTerm = resTerm;
                            }
                        }
                        return 0;
                    } catch (Exception e) {
                        LOGGER.error("future.get exception , e : ", e);
                        return -1;
                    } finally {
                        //减少投票期待
                        latch.countDown();
                    }
                });
            }

            try {
                // 稍等片刻
                latch.await(3500, MILLISECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("InterruptedException By Master election Task");
            }

            int success = success2.get();
            LOGGER.info("node {} maybe become leader , success count = {} , status : {}", peerSet.getSelf(), success, NodeStatus.Enum.value(status));
            // 如果投票期间,有其他服务器发送 appendEntry , 就可能变成 follower ,这时,应该停止.
            if (status == NodeStatus.FOLLOWER) {
                return;
            }
            // 加上自身.
            //TODO 这里就是通过投票成为主节点，可以处理一些prepareWork
            if (success >= peers.size() >> 1) {
                LOGGER.warn("node {} become leader ", peerSet.getSelf());
                status = NodeStatus.LEADER;
                peerSet.setLeader(peerSet.getSelf());
                votedFor = "";
                becomeLeaderToDoThing();
            } else {
                // else 重新选举
                votedFor = "";
            }

        }
    }

    /**
     * 初始化所有的 nextIndex 值为自己的最后一条日志的 index + 1. 如果下次 RPC 时, 跟随者和leader 不一致,就会失败.
     * 那么 leader 尝试递减 nextIndex 并进行重试.最终将达成一致.
     */
    private void becomeLeaderToDoThing() {
        nextIndexs = new ConcurrentHashMap<>();
        matchIndexs = new ConcurrentHashMap<>();
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            nextIndexs.put(peer, logModule.getLastIndex() + 1);
            matchIndexs.put(peer, 0L);
        }
    }


    class HeartBeatTask implements Runnable {

        @Override
        public void run() {

            if (status != NodeStatus.LEADER) {
                return;
            }

            long current = System.currentTimeMillis();
            if (current - preHeartBeatTime < heartBeatTick) {
                return;
            }
            LOGGER.info("===========HeartBeatTask-NextIndex =============");
            for (Peer peer : peerSet.getPeersWithOutSelf()) {
                LOGGER.info("Peer {} nextIndex={}", peer.getHostAndPort(), nextIndexs.get(peer));
            }

            preHeartBeatTime = System.currentTimeMillis();

            // 心跳只关心 term 和 leaderID
            for (Peer peer : peerSet.getPeersWithOutSelf()) {

                AppendLogParam param = AppendLogParam.newBuilder()
                        .entries(null)// 心跳,空日志.
                        .leaderId(peerSet.getSelf().getHostAndPort())
                        //向其他节点发送
                        .serverId(peer.getHostAndPort())
                        .term(currentTerm)
                        .build();

                Request<AppendLogParam> request = new Request<>(
                        Request.A_ENTRIES,
                        param,
                        peer.getHostAndPort());

                RaftThreadPool.execute(() -> {
                    try {
                        //通过心跳任务去获取别人的任期，从而加强一致性
                        Response response = getRpcClient().send(request);
                        AppendResult AppendLogResult = (AppendResult) response.getResult();
                        long term = AppendLogResult.getTerm();

                        if (term > currentTerm) {
                            LOGGER.error("self will become follower, his term : {}, but my term : {}", term, currentTerm);
                            currentTerm = term;
                            votedFor = "";
                            status = NodeStatus.FOLLOWER;
                        }
                    } catch (Exception e) {
                        LOGGER.error("HeartBeatTask RPC Fail, request URL : {} ", request.getUrl());
                    }
                }, false);
            }
        }
    }

    @Override
    @Deprecated
    public Result addPeer(Peer newPeer) {
        return delegate.addPeer(newPeer);
    }

    @Override
    @Deprecated
    public Result removePeer(Peer oldPeer) {
        return delegate.removePeer(oldPeer);
    }

}
