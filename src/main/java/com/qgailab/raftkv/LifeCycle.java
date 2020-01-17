package com.qgailab.raftkv;

/**
 * @author linxu
 * this is the lifecycle of the server node.
 */
public interface LifeCycle {
    /**
     * 节点的生命周期初始化
     *
     * @throws Throwable
     */
    void init() throws Throwable;

    /**
     * 节点的销毁
     *
     * @throws Throwable
     */
    void destroy() throws Throwable;
}
