package com.qgailab.raftkv.entity;

import java.io.Serializable;

/**
 * @author linxu
 * @date 2020/1/18
 * <tip>take care of yourself.everything is no in vain.</tip>
 * 基础参数类型
 */
public class BaseParam implements Serializable {
    /**
     * 接收者的ID，参数格式如下：
     * <p>
     * ip:port
     * </p>
     */
    String serverId;
    /**
     * 候选者的任期号码,candidateId.
     */
    long term;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }
}
