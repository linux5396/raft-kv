package com.qgailab.raftkv.rpc;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;


/**
 * @author linxu
 */
@Data
public class Request<T> implements Serializable {

    /**
     * 投票请求
     *
     * @see DefaultRpcServer
     */
    public static final int R_VOTE = 0;
    /**
     * 附加日志
     *
     * @see DefaultRpcServer
     * @see com.qgailab.raftkv.internal.impl.DefaultNode
     */
    public static final int A_ENTRIES = 1;
    /**
     * 客户端请求，是操作请求
     *
     * @see DefaultRpcServer
     */
    public static final int CLIENT_REQ = 2;
    /**
     * 配置变更. add
     */
    public static final int CHANGE_CONFIG_ADD = 3;
    /**
     * 配置变更. remove
     */
    public static final int CHANGE_CONFIG_REMOVE = 4;
    /**
     * 请求类型
     */
    private int cmd = -1;

    /**
     * param
     *
     * @see com.qgailab.raftkv.internal.entity.AentryParam
     * @see com.qgailab.raftkv.internal.entity.RvoteParam
     * @see com.qgailab.raftkv.client.ClientKVAck
     */
    private T obj;

    String url;

    public Request() {
    }

    public Request(T obj) {
        this.obj = obj;
    }

    public Request(int cmd, T obj, String url) {
        this.cmd = cmd;
        this.obj = obj;
        this.url = url;
    }

    @SuppressWarnings("unchecked")
    private Request(Builder builder) {
        setCmd(builder.cmd);
        setObj((T) builder.obj);
        setUrl(builder.url);
    }

    public static Builder newBuilder() {
        return new Builder<>();
    }


    public final static class Builder<T> {

        private int cmd;
        private Object obj;
        private String url;

        private Builder() {
        }

        public Builder cmd(int val) {
            cmd = val;
            return this;
        }

        public Builder obj(Object val) {
            obj = val;
            return this;
        }

        public Builder url(String val) {
            url = val;
            return this;
        }

        public Request<T> build() {
            return new Request<T>(this);
        }
    }

}
