package com.qgailab.raftkv.common;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linxu
 * @date 2020/1/18
 * <tip>take care of yourself.everything is no in vain.</tip>
 * 节点集合
 */
@Data
public class PeerSet implements Serializable {
    private List<Peer> peers = new ArrayList<>();
    private volatile Peer leader;
    private volatile Peer self;

    private PeerSet() {
    }

    public static PeerSet getInstance() {
        return PeerSetLazyHolder.INSTANCE;
    }

    /**
     * 使用内部静态类实现单例模式的懒惰加载
     */
    private static class PeerSetLazyHolder {
        private static final PeerSet INSTANCE = new PeerSet();
    }

    public void addPeer(Peer peer) {
        peers.add(peer);

    }

    public void removePeer(Peer peer) {
        peers.remove(peer);
    }

    /**
     * 获取除了自身之外的其他节点
     */
    public List<Peer> getPeersWithOutSelf() {
        List<Peer> list2 = new ArrayList<>(peers);
        list2.remove(self);
        return list2;
    }
}
