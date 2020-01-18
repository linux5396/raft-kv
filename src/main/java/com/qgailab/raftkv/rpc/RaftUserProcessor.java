package com.qgailab.raftkv.rpc;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;
import com.qgailab.raftkv.exception.RaftNotSupportException;


/**
 *
 * @author linxu
 */
public abstract class RaftUserProcessor<T> extends AbstractUserProcessor<T> {

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request) {
        throw new RaftNotSupportException(
            "Raft Server not support handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request) ");
    }


    @Override
    public String interest() {
        return Request.class.getName();
    }
}
