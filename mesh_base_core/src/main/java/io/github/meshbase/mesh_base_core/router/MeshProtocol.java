package io.github.meshbase.mesh_base_core.router;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Function;

public abstract class MeshProtocol<T extends MeshSerializer<T>> implements MeshSerializer<MeshProtocol<T>> {
    private static final String TAG = "my_mesh_protocol";
    //TODO: revert to protected when the BLETestScreen.kt doesn't need to decode bytes no more
    public UUID sender;
    public UUID destination;
    //TODO: revert to protected when the BLETestScreen.kt doesn't need to decode bytes no more
    public T body;
    protected int messageType;
    protected int remainingHops;
    protected int messageId;

    private static final int HEADER_LENGTH = 48;

    public MeshProtocol(int messageType, int remainingHops, int messageId, UUID sender, UUID destination, T body) {
        this.messageType = messageType;
        this.remainingHops = remainingHops;
        this.messageId = messageId;
        this.sender = sender;
        this.destination = destination;
        this.body = body;
    }

    public static MeshProtocol<?> decode(byte[] data) {
        Log.d(TAG, "Trying to decode recieved data of length" + data.length);
        ProtocolType type = getByteType(data);
        Log.d(TAG, "Data type is " + type);

        switch (type) {
            case ACK:
                return decode(data, AckMessageBody::decode);
            case PING:
                return decode(data, PingMessageBody::decode);
            case SEND_MESSAGE:
                return decode(data, SendMessageBody::decode);
            case RREQ:
                return decode(data, RREQBody::decode);
            case RREP:
                return decode(data, RREPBody::decode);
            case RAW_BYTES_MESSAGE:
                return decode(data, RawBytesBody::decode);
            case FILE_TRANSFER:
                return decode(data, FileTransferBody::decode);
            default:
                throw new IllegalArgumentException("Unsupported protocol type: " + type);
        }

    }

    public static <T extends MeshSerializer<T>> MeshProtocol<T> decode(byte[] data,
                                                                       Function<byte[], T> bodyDecoder) {
        Log.d(TAG, "meshserializer trying to decode");
        if (data.length < HEADER_LENGTH) {
            Log.e(TAG, "Data length is too small");
            throw new IllegalArgumentException("Buffer data cannot be determined due to small length size. [SMALL_HEADER_SIZE]");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        int messageType = buffer.getInt();
        int remainingHops = buffer.getInt();
        int messageId = buffer.getInt();
        UUID sender = new UUID(buffer.getLong(), buffer.getLong());

        long mostSignificantBits = buffer.getLong();
        long leastSignificantBits = buffer.getLong();

        UUID destination = null;

        if (mostSignificantBits != 0L || leastSignificantBits != 0L) {
            destination = new UUID(mostSignificantBits, leastSignificantBits);
        }

        Log.d(TAG, "Destination is " + destination);

        int bodyLength = buffer.getInt();

        byte[] bodyBytes = new byte[bodyLength];
        buffer.get(bodyBytes);
        Log.d(TAG, "body bytes are ");
        T body = bodyDecoder.apply(bodyBytes);

        Log.d(TAG, "body is " + body);

        return new ConcreteMeshProtocol<>(messageType, remainingHops, messageId, sender, destination, body);

    }


    public static ProtocolType getByteType(byte[] data) {
        if (data.length < 4) {
            throw new IllegalArgumentException("Buffer data cannot be determined due to small length size.[CANNOT_DETERMINE_TYPE]");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int messageType = buffer.getInt();

        switch (messageType) {
            case 0:
                return ProtocolType.ACK;
            case 1:
                return ProtocolType.SEND_MESSAGE;
            case 2:
                return ProtocolType.RECEIVE_MESSAGE;
            case 3:
                return ProtocolType.RAW_BYTES_MESSAGE;
            case 4:
                return ProtocolType.RREQ;
            case 5:
                return ProtocolType.RREP;
            case 6:
                return ProtocolType.PING;
            case 7:
                return ProtocolType.FILE_TRANSFER;
            default:
                return ProtocolType.UNKNOWN_MESSAGE_TYPE;
        }
    }

    @Override
    public byte[] encode() {
        byte[] bodyBytes = body != null ? body.encode() : new byte[0];
        int bodyLength = bodyBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH + bodyLength);
        buffer.putInt(messageType);
        buffer.putInt(remainingHops);
        buffer.putInt(messageId);
        buffer.putLong(sender.getMostSignificantBits());
        buffer.putLong(sender.getLeastSignificantBits());
        if (destination == null) {
            buffer.putLong(0L);
            buffer.putLong(0L);
        } else {
            buffer.putLong(destination.getMostSignificantBits());
            buffer.putLong(destination.getLeastSignificantBits());
        }
        buffer.putInt(bodyLength);
        buffer.put(bodyBytes);
        return buffer.array();

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeshProtocol<?> that = (MeshProtocol<?>) o;

        return messageType == that.messageType &&
                remainingHops == that.remainingHops &&
                messageId == that.messageId &&
                sender.equals(that.sender) &&
                body.equals(that.body);
    }

    @Override
    public int hashCode() {
        int result = messageType;
        result = 31 * result + remainingHops;
        result = 31 * result + messageId;
        result = 31 * result + sender.hashCode();
        result = 31 * result + body.hashCode();

        return result;
    }

    public ProtocolType getByteType() {
        byte[] data = this.encode();
        return getByteType(data);
    }

    public int getMessageId() {
        return this.messageId;
    }

    public int getRemainingHops() {
        return this.remainingHops;
    }
}
