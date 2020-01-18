package com.qgailab.raftkv.dynamic.memship;


import com.qgailab.raftkv.common.Peer;

/**
 *
 * 集群配置变更接口.
 *
 * @author linxu
 */
@Deprecated//member ship cannot used
public interface ClusterMembershipChanges {

    /**
     * 添加节点.
     * @param newPeer
     * @return
     */
    Result addPeer(Peer newPeer);

    /**
     * 删除节点.
     * @param oldPeer
     * @return
     */
    Result removePeer(Peer oldPeer);
}

