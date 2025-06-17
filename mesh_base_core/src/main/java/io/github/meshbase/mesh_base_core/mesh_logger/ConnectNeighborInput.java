package io.github.meshbase.mesh_base_core.mesh_logger;

public class ConnectNeighborInput extends MeshEventsModel {
    public final String neighbor_id;
    public final String technology;

    public ConnectNeighborInput(String source_id, String neighbor_id, String technology) {
        super("CONNECT_NEIGHBOR", source_id);
        this.neighbor_id = neighbor_id;
        this.technology = technology;
    }
}
