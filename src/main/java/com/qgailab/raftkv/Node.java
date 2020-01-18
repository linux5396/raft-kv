package com.qgailab.raftkv;


import com.qgailab.raftkv.client.ClientKVAck;
import com.qgailab.raftkv.client.ClientKVReq;
import com.qgailab.raftkv.common.NodeInfo;
import com.qgailab.raftkv.entity.AppendLogParam;
import com.qgailab.raftkv.entity.AppendResult;
import com.qgailab.raftkv.entity.RequestVoteParam;
import com.qgailab.raftkv.entity.VoteResult;

/**
 * @author linxu
 * <p>
 * <p>
 * <p>
 * 一个RPC分布式网络节点的基本功能：
 * 处理请求投票 RPC.
 * 处理附加日志请求.
 * 处理客户端请求.
 * 转发给 leader 节点.
 */
public interface Node<T> extends LifeCycle {

    /**
     * 设置节点信息
     */
    void loadNodeInfo(NodeInfo nodeInfo);

    /**
     * 处理请求投票 RPC.
     *
     * @param param
     * @return
     */
    VoteResult handlerRequestVote(RequestVoteParam param);

    /**
     * 处理附加日志请求.
     *
     * @param param
     * @return
     */
    AppendResult handlerAppendEntries(AppendLogParam param);

    /**
     * 处理客户端请求.
     *
     * @param request
     * @return
     */
    ClientKVAck handlerClientRequest(ClientKVReq request);

    /**
     * 转发给 leader 节点.
     *
     * @param request
     * @return
     */
    ClientKVAck redirect(ClientKVReq request);

}
