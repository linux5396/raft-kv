package com.qgailab.raftkv.rpc;

import com.alipay.remoting.BizContext;
import com.qgailab.raftkv.client.ClientKVReq;
import com.qgailab.raftkv.common.Peer;
import com.qgailab.raftkv.entity.AppendLogParam;
import com.qgailab.raftkv.entity.RequestVoteParam;
import com.qgailab.raftkv.impl.DefaultNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Raft Server in protocol rpc.
 *
 * @author linxu
 */
@SuppressWarnings("unchecked")
public class DefaultRpcServer<T> implements RpcServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRpcServer.class);
    private volatile boolean flag;

    private DefaultNode<T> node;

    private com.alipay.remoting.rpc.RpcServer rpcServer;

    public DefaultRpcServer(int port, DefaultNode node) {

        if (flag) {
            return;
        }
        synchronized (this) {
            if (flag) {
                return;
            }

            rpcServer = new com.alipay.remoting.rpc.RpcServer(port, false, false);
            rpcServer.registerUserProcessor(new RaftUserProcessor<Request>() {
                @Override
                public Object handleRequest(BizContext bizCtx, Request request) throws Exception {
                    return handlerRequest(request);
                }
            });
            this.node = node;
            flag = true;
        }

    }

    @Override
    public void start() {
        LOGGER.info("rpc server starting");
        rpcServer.start();
    }

    @Override
    public void stop() {
        LOGGER.warn("rpc server stopping.");
        rpcServer.stop();
    }

    /**
     * RPC请求的处理
     *
     * @param request 请求
     * @return 处理结果
     */
    @Override
    public Response handlerRequest(Request request) {
        switch (request.getCmd()) {
            case Request.R_VOTE:
                return new Response(node.handlerRequestVote((RequestVoteParam) request.getObj()));
            case Request.A_ENTRIES:
                return new Response(node.handlerAppendEntries((AppendLogParam) request.getObj()));
            case Request.CLIENT_REQ:
                return new Response(node.handlerClientRequest((ClientKVReq) request.getObj()));
            /*case Request.CHANGE_CONFIG_REMOVE:
                return new Response(node.removePeer((Peer) request.getObj()));
            case Request.CHANGE_CONFIG_ADD:
                return new Response(node.addPeer((Peer) request.getObj()));*/
            default:
                return null;
        }
    }


}
