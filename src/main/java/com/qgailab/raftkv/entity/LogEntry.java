package com.qgailab.raftkv.entity;

import java.io.Serializable;

/**
 * @author linxu
 * @date 2020/1/18
 * <tip>take care of yourself.everything is no in vain.</tip>
 */
public class LogEntry implements Serializable, Comparable {
    /**
     * 日志索引
     */
    private Long index;
    /**
     * 日志任期号码
     */
    private long term;

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
