package com.qgailab.raftkv.rpc;

/**
 * @author linxu
 */
public interface RpcClient {
    /**
     * RPC发送
     */
    Response send(Request request);

    /**
     * RPC发送，带有超时机制
     */
    Response send(Request request, int timeout);
}
