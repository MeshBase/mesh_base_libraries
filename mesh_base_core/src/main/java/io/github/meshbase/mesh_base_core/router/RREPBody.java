package io.github.meshbase.mesh_base_core.router;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RREPBody implements MeshSerializer<RREPBody>{
    public final int querySequenceNumber;
    public final UUID queryId;

    public final UUID routePath;

    private final static int RREP_BODY_PARTIAL_LENGTH = 36;
    public RREPBody(int querySequenceNumber, UUID queryId, UUID routePath) {
        this.querySequenceNumber = querySequenceNumber;
        this.queryId = queryId;
        this.routePath = routePath;
    }
    @Override
    public byte[] encode() {
        int totalRREPLength = RREP_BODY_PARTIAL_LENGTH;

        ByteBuffer buffer = ByteBuffer.allocate(totalRREPLength);
        buffer.putInt(querySequenceNumber);

        buffer.putLong(queryId.getMostSignificantBits());
        buffer.putLong(queryId.getLeastSignificantBits());

        buffer.putLong(routePath.getMostSignificantBits());
        buffer.putLong(routePath.getLeastSignificantBits());
        Log.d("my_mesh_router", "call it maaaagic");
        return buffer.array();
    }

    public static RREPBody decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int querySequenceNumber = buffer.getInt();

        long queryMostSigBits = buffer.getLong();
        long queryLeastSigBits = buffer.getLong();

        final UUID queryId = new UUID(queryMostSigBits, queryLeastSigBits);


        final UUID routePath = new UUID(buffer.getLong(), buffer.getLong());

        return new RREPBody(
                querySequenceNumber,
                queryId,
                routePath
        );
    }
}
