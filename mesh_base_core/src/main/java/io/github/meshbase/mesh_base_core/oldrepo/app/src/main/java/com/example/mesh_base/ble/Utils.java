package com.example.mesh_base.ble;
import java.nio.ByteBuffer;
import java.util.UUID;

class CommonConstants {
    public static final UUID SERVICE_UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb");
    public static final UUID MESSAGE_UUID = UUID.fromString("0000beef-0000-1000-8000-00805f9b34fb");
    public static final UUID ID_UUID = UUID.fromString("b000000f-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
}

class ConvertUUID {
    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID bytesToUUID(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Invalid UUID byte array length: " + bytes.length);
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long mostSigBits = bb.getLong();
        long leastSigBits = bb.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }
}
