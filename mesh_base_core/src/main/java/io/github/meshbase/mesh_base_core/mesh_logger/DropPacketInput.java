package io.github.meshbase.mesh_base_core.mesh_logger;

public class DropPacketInput extends MeshEventsModel {
    public final String packet_id;
    public final String reason;

    public DropPacketInput(String source_id, String packet_id, String reason) {
        super("DROP_PACKET", source_id);
        this.packet_id = packet_id;
        this.reason = reason;
    }
}
