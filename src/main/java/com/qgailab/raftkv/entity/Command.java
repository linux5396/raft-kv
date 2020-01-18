package com.qgailab.raftkv.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author linxu
 * @date 2020/1/18
 * <tip>take care of yourself.everything is no in vain.</tip>
 * 命令实体，每个命令实体由键值对构成。
 */
public class Command implements Serializable {
    /**
     * 序列化后的键
     */
    private String key;
    /**
     * 序列化后的值
     */
    private String value;

    public Command(String key, String value) {
        this.key = key;
        this.value = value;
    }

    private Command(Builder builder) {
        setKey(builder.key);
        setValue(builder.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Command command = (Command) o;
        return Objects.equals(key, command.key) &&
                Objects.equals(value, command.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private String key;
        private String value;

        private Builder() {
        }

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public Command build() {
            return new Command(this);
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
