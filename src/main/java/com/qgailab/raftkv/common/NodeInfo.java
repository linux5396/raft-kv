package com.qgailab.raftkv.common;

import lombok.Data;

import java.util.List;

/**
 * @author linxu
 * @date 2020/1/18
 * <tip>take care of yourself.everything is no in vain.</tip>
 * 节点的信息，主要是节点的地址。
 */
@Data
public class NodeInfo {
    /**
     * 自身 selfPort  端口号
     */
    public int selfPort;
    /**
     * 自身的host
     */
    public String selfHost;

    /**
     * 所有节点地址.IP:PORT
     */
    public List<String> peerAddress;
}
