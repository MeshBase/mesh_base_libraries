package io.github.meshbase.mesh_base_core.router;

import java.nio.ByteBuffer;

import kotlin.jvm.internal.ByteSpreadBuilder;

public class FileTransferBody implements MeshSerializer<FileTransferBody> {
    public int magic;
    public int transferId;

    public short seqNum;

    public short totalChunks;

    public byte[] data;

    private static final int BODY_LENGTH = 12;


    public FileTransferBody(int magic, int transferId, short seqNum, short totalChunks, byte[] data) {
        this.magic = magic;
        this.transferId = transferId;
        this.seqNum = seqNum;
        this.totalChunks = totalChunks;
        this.data = data;

    }
    @Override
    public byte[] encode() {
        int dataLength = data.length;
        ByteBuffer buffer = ByteBuffer.allocate(BODY_LENGTH + dataLength);
        buffer.putInt(magic);
        buffer.putInt(transferId);
        buffer.putShort(seqNum);
        buffer.putShort(totalChunks);
        buffer.put(data);

        return buffer.array();
    }

    public static FileTransferBody decode(byte[] arr) {
        ByteBuffer buffer = ByteBuffer.wrap(arr);

        int mgk = buffer.getInt();
        int tfrId = buffer.getInt();

        short sNum = buffer.getShort();
        short ttlChunk = buffer.getShort();

        byte[] dt = new byte[arr.length - BODY_LENGTH];
        buffer.get(dt);

        return new FileTransferBody(mgk, tfrId, sNum, ttlChunk, dt);
    }
}
