package com.qgailab.raftkv;


import com.qgailab.raftkv.entity.LogEntry;

/**
 * 状态机接口.
 * @author linxu
 */
public interface StateMachine {

    /**
     * 将数据应用到状态机.
     *
     * 原则上,只需这一个方法(apply). 其他的方法是为了更方便的使用状态机.
     * @param logEntry 日志中的数据.
     */
    void apply(LogEntry logEntry);

    LogEntry get(String key);

    String getString(String key);

    void setString(String key, String value);

    void delString(String... key);

}
