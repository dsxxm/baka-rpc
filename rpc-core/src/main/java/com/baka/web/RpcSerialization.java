package com.baka.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.baka.protocol.RpcMessage;

/**
 * RpcSerialization 根据协议 codec 执行序列化。
 */
public final class RpcSerialization {

    private RpcSerialization() {
    }

    public static byte[] serialize(byte codec, Object value) {
        if (codec != RpcMessage.Codec.JAVA) {
            throw new IllegalArgumentException("unsupported RPC codec: " + codec);
        }

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("failed to serialize RPC payload", e);
        }
    }

    public static <T> T deserialize(byte codec, byte[] bytes, Class<T> type) {
        if (codec != RpcMessage.Codec.JAVA) {
            throw new IllegalArgumentException("unsupported RPC codec: " + codec);
        }

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return type.cast(in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("failed to deserialize RPC payload", e);
        }
    }
}
