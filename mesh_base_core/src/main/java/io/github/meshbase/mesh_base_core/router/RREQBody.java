package io.github.meshbase.mesh_base_core.router;

import java.nio.ByteBuffer;
import java.util.UUID;

public class RREQBody implements MeshSerializer<RREQBody>{
    protected final int querySequenceNumber;
    protected final UUID querySequenceId;
    protected UUID previousHop;
    private final int hopCount;
    private final int maxHops;
    private static final int BODY_LENGTH = 44;


    RREQBody(int querySequenceNumber, UUID querySequenceId, UUID previousHop, int hopCount, int maxHops) {
        this.querySequenceNumber = querySequenceNumber;
        this.querySequenceId = querySequenceId;
        this.previousHop = previousHop;
        this.hopCount = hopCount;
        this.maxHops = maxHops;
    }

    @Override
    public byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(BODY_LENGTH);
        buffer.putInt(querySequenceNumber);
        // query sequence id
        buffer.putLong(querySequenceId.getMostSignificantBits());
        buffer.putLong(querySequenceId.getLeastSignificantBits());
        // previous hop
        buffer.putLong(previousHop.getMostSignificantBits());
        buffer.putLong(previousHop.getLeastSignificantBits());
        //hop count
        buffer.putInt(hopCount);
        // max hops
        buffer.putInt(maxHops);

        return buffer.array();
    }

    public static RREQBody decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int querySeqNum = buffer.getInt();

        long queryMostSigBits = buffer.getLong();
        long queryLeastSigBits = buffer.getLong();
        UUID querySeqId = new UUID(queryMostSigBits, queryLeastSigBits);

        long prevMostSigBits = buffer.getLong();
        long prevLeastSigBits = buffer.getLong();
        UUID prevHopId = new UUID(prevMostSigBits, prevLeastSigBits);

        int hops = buffer.getInt();
        int maximumHops = buffer.getInt();

        return new RREQBody(
                    querySeqNum,
                    querySeqId,
                    prevHopId,
                    hops,
                    maximumHops
        );
    }
}
