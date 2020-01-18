package com.qgailab.raftkv.rpc;

import com.alipay.remoting.exception.RemotingException;

import com.qgailab.raftkv.exception.RaftRemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author linxu
 */
public class DefaultRpcClient implements RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRpcClient.class.getName());

    private final static com.alipay.remoting.rpc.RpcClient CLIENT = new com.alipay.remoting.rpc.RpcClient();
    static {
        CLIENT.init();
    }

    /**
     * default timeout is 200Sec
     */
    @Override
    public Response send(Request request) {
        return send(request, 200000);
    }

    @Override
    public Response send(Request request, int timeoutOfMills) {
        Response result = null;
        try {
            result = (Response) CLIENT.invokeSync(request.getUrl(), request, timeoutOfMills);
        } catch (RemotingException e) {
            e.printStackTrace();
            logger.info("rpc RaftRemotingException ");
            throw new RaftRemotingException();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return (result);
    }
}
