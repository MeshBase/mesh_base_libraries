package com.example.mesh_base.router;

import java.nio.ByteBuffer;
import java.util.Objects;

public class AckMessageBody implements MeshSerializer<AckMessageBody>{
    private final String message;


    public AckMessageBody(String message) {
        this.message = message;
    }

    public static AckMessageBody decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int messageLength = buffer.getInt();
        byte[] msgBytes = new byte[messageLength];
        String receiveMsg = "";

        if (messageLength > 0) {
            buffer.get(msgBytes);
            receiveMsg = new String(msgBytes);
        }

        return new AckMessageBody(receiveMsg);
    }
    @Override
    public byte[] encode() {
        int messageLength = message.length();
        ByteBuffer buffer = ByteBuffer.allocate(4 + messageLength);

        buffer.putInt(messageLength);
        buffer.put(message.getBytes());

        return buffer.array();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AckMessageBody that = (AckMessageBody) o;

        return Objects.equals(message,that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }
}
