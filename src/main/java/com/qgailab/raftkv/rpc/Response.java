package com.qgailab.raftkv.rpc;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author linxu
 */
@Data
public class Response<T> implements Serializable {


    private T result;

    public Response(T result) {
        this.result = result;
    }

    @SuppressWarnings("unchecked")
    private Response(Builder builder) {
        setResult((T) builder.result);
    }

    public static Response ok() {
        return new Response<>("ok");
    }

    public static Response fail() {
        return new Response<>("fail");
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {

        private Object result;

        private Builder() {
        }

        public Builder result(Object val) {
            result = val;
            return this;
        }

        public Response build() {
            return new Response(this);
        }
    }
}
