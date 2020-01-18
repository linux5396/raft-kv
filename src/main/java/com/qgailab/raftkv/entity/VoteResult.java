package com.qgailab.raftkv.entity;

import java.io.Serializable;

/**
 * @author linxu
 * @date 2020/1/18
 * <tip>take care of yourself.everything is no in vain.</tip>
 */
public class VoteResult implements Serializable {
    /**
     * 整个群体的任期号
     */
    long term;
    /**
     * 是否赢得投票
     */
    boolean voteGranted;

    public VoteResult(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    public VoteResult(long term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * @return 失败
     */
    public static VoteResult fail() {
        return new VoteResult(false);
    }

    /**
     * @return 成功
     */
    public static VoteResult ok() {
        return new VoteResult(true);
    }

    private VoteResult(Builder builder) {
        this.term = builder.term;
        this.voteGranted = builder.voteGranted;
    }

    public static final class Builder {

        private long term;
        private boolean voteGranted;

        private Builder() {
        }

        public Builder term(long term) {
            this.term = term;
            return this;
        }

        public Builder voteGranted(boolean voteGranted) {
            this.voteGranted = voteGranted;
            return this;
        }

        public VoteResult build() {
            return new VoteResult(this);
        }
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    public void setVoteGranted(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }
}
