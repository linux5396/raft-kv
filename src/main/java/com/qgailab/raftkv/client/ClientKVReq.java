package com.qgailab.raftkv.client;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author linxu
 * @date 2019/11/16
 * <tip>take care of yourself.everything is no in vain.</tip>
 *
 */
@Data
public class ClientKVReq implements Serializable {

    public static int PUT = 0;
    public static int GET = 1;

    public static int PROPOSE = 2;
    public static int DEL=3;
    int type;

    String key;

    String value;

    private ClientKVReq(Builder builder) {
        setType(builder.type);
        setKey(builder.key);
        setValue(builder.value);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public enum Type {
        PUT(0), GET(1), PROPOSE(2),DEL(3);
        int code;

        Type(int code) {
            this.code = code;
        }

        public static Type value(int code) {
            for (Type type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }


    public static final class Builder {

        private int type;
        private String key;
        private String value;

        private Builder() {
        }


        public Builder type(int val) {
            type = val;
            return this;
        }

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public ClientKVReq build() {
            return new ClientKVReq(this);
        }
    }
}
