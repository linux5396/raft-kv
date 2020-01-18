package com.qgailab.raftkv.entity;


/**
 * @author linxu
 * @date 2020/1/18
 * <tip>take care of yourself.everything is no in vain.</tip>
 * 请求投票的参数实体，包含候选人ID、候选人任期号码、候选人的索引、候选人的日志任期号码、接收者ID。
 */
public class RequestVoteParam extends BaseParam {
    /**
     * 候选人ID
     * ip:port
     */
    private String candidateId;
    /**
     * 候选人的最后日志项索引值
     */
    private long lastLogIndex;
    /**
     * 候选人的最后日志项任期号
     */
    private long lastLogTerm;

    private RequestVoteParam(Builder builder) {
        setCandidateId(builder.candidateId);
        setLastLogIndex(builder.lastLogIndex);
        setLastLogTerm(builder.lastLogTerm);
        setServerId(builder.serverId);
        setTerm(builder.term);
    }

    @Override
    public String toString() {
        return "RequestVoteParam{" +
                "candidateId='" + candidateId + '\'' +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                ", term=" + term +
                ", serverId='" + serverId + '\'' +
                '}';
    }

    public static final class Builder {
        private long term;
        private String serverId;
        private String candidateId;
        private long lastLogIndex;
        private long lastLogTerm;

        private Builder() {
        }

        public Builder term(long val) {
            term = val;
            return this;
        }

        public Builder serverId(String val) {
            serverId = val;
            return this;
        }

        public Builder candidateId(String val) {
            candidateId = val;
            return this;
        }

        public Builder lastLogIndex(long val) {
            lastLogIndex = val;
            return this;
        }

        public Builder lastLogTerm(long val) {
            lastLogTerm = val;
            return this;
        }

        public RequestVoteParam build() {
            return new RequestVoteParam(this);
        }
    }

    /**
     * getter setter below
     **/
    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogIndex(long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }

    public void setLastLogTerm(long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }
}
