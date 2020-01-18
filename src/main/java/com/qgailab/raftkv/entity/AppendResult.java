package com.qgailab.raftkv.entity;

import java.io.Serializable;

/**
 * @author linxu
 * @date 2020/1/18
 * <tip>take care of yourself.everything is no in vain.</tip>
 * 日志追加结果
 */
public class AppendResult implements Serializable {
    /**
     * 当前任期号码，用于更新自己
     */
    private long term;
    /**
     * 跟随者包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真
     */
    private boolean success;

    public AppendResult(long term) {
        this.term = term;
    }

    public AppendResult(boolean success) {
        this.success = success;
    }

    public AppendResult(long term, boolean success) {
        this.term = term;
        this.success = success;
    }

    public static AppendResult fail() {
        return new AppendResult(false);
    }

    public static AppendResult ok() {
        return new AppendResult(true);
    }

    public AppendResult(Builder builder) {
        setTerm(builder.term);
        setSuccess(builder.success);
    }

    @Override
    public String toString() {
        return "AppendResult{" +
                "term=" + term +
                ", success=" + success +
                '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {

        private long term;
        private boolean success;

        private Builder() {
        }

        public Builder term(long val) {
            term = val;
            return this;
        }

        public Builder success(boolean val) {
            success = val;
            return this;
        }

        public AppendResult build() {
            return new AppendResult(this);
        }
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
