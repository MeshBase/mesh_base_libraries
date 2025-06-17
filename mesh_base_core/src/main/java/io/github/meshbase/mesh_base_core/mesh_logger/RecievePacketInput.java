package io.github.meshbase.mesh_base_core.mesh_logger;

public class RecievePacketInput extends MeshEventsModel {
    public final String destination_id;
    public final String packet_id;
    public final String technology;

    public RecievePacketInput(String source_id, String destination_id, String packet_id, String technology) {
        super("RECIEVE_PACKET", source_id);
        this.destination_id = destination_id;
        this.packet_id = packet_id;
        this.technology = technology;
    }
}
