package io.github.meshbase.mesh_base_core.router;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RREPBody implements MeshSerializer<RREPBody>{
    public final int querySequenceNumber;
    public final UUID queryId;

    public final List<UUID> routePath;

    private final static int RREP_BODY_PARTIAL_LENGTH = 20;
    public RREPBody(int querySequenceNumber, UUID queryId, List<UUID> routePath) {
        this.querySequenceNumber = querySequenceNumber;
        this.queryId = queryId;
        this.routePath = routePath;
    }
    @Override
    public byte[] encode() {
        int routePathLength = routePath.size();
        int totalRREPLength = (routePathLength * 16) + RREP_BODY_PARTIAL_LENGTH;

        ByteBuffer buffer = ByteBuffer.allocate(totalRREPLength);
        buffer.putInt(querySequenceNumber);

        buffer.putLong(queryId.getMostSignificantBits());
        buffer.putLong(queryId.getLeastSignificantBits());

        buffer.putInt(routePathLength);
        for (UUID deviceId: routePath) {
            buffer.putLong(deviceId.getMostSignificantBits());
            buffer.putLong(deviceId.getLeastSignificantBits());
        }
        return buffer.array();
    }

    public static RREPBody decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int querySequenceNumber = buffer.getInt();

        long queryMostSigBits = buffer.getLong();
        long queryLeastSigBits = buffer.getLong();

        UUID queryId = new UUID(queryMostSigBits, queryLeastSigBits);

        int routePathLength = buffer.getInt();

        final List<UUID> routePath = new ArrayList<>();
        for (int i = 0; i < routePathLength; i++) {
            long deviceMostSigBits = buffer.getLong();
            long deviceLeastSigBits = buffer.getLong();

            UUID routeDevice = new UUID(deviceMostSigBits, deviceLeastSigBits);
            routePath.add(routeDevice);
        }
        return new RREPBody(
                querySequenceNumber,
                queryId,
                routePath
        );
    }
}
