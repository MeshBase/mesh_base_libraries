package io.github.meshbase.mesh_base_core.router;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * KeyShareBody is a multihop MeshProtocol where we share our public key to not just our
 * direct neighbors but Broadcasting it multiple hops away from us
 */
public class KeyShareBody implements MeshSerializer<KeyShareBody> {
    private final byte[] publicKey;

    public KeyShareBody(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + publicKey.length);
        buffer.putInt(publicKey.length);
        buffer.put(publicKey);
        return buffer.array();
    }

    public static KeyShareBody decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int length = buffer.getInt();
        byte[] keyBytes = new byte[length];
        if (length > 0) {
            buffer.get(keyBytes);
        }
        return new KeyShareBody(keyBytes);
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyShareBody that = (KeyShareBody) o;
        return Arrays.equals(that.publicKey, ((KeyShareBody) o).publicKey);
    }
}
