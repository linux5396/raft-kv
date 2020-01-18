package com.qgailab.raftkv;


import com.qgailab.raftkv.entity.LogEntry;

/**
 * @author linxu
 * 日志模块的接口定义
 */
public interface LogModule {
    /**
     * 写日志
     */
    void write(LogEntry logEntry);

    /**
     * 读日志
     */
    LogEntry read(Long index);

    /**
     * 移出从offset开始的日志
     */
    void removeOnStartIndex(Long startIndex);

    /**
     * 获取最后的日志
     */
    LogEntry getLast();

    /**
     * 获取最后的日志索引
     */
    Long getLastIndex();
}
