package com.qgailab.raftkv.common;

import lombok.Data;

/**
 * @author linxu
 * @date 2020/1/18
 * <tip>take care of yourself.everything is no in vain.</tip>
 */
@Data
public class Peer {
    /**
     * 节点地址
     */
    private final String hostAndPort;

    public Peer(String hostAndPort) {
        this.hostAndPort = hostAndPort;
    }
}
