package com.qgailab.raftkv.rpc;

/**
 * @author linxu
 */
public interface RpcServer {
    /**
     * 启动
     */
    void start();

    /**
     * 结束
     */
    void stop();

    /**
     * Multipart dispatcher.
     */
    Response handlerRequest(Request request);

}
