package io.github.meshbase.mesh_base_core.mesh_logger;

public abstract class MeshEventsModel {
    public final String event_type;
    public final String source_id;

    public MeshEventsModel(String event_type, String source_id) {
        this.event_type = event_type;
        this.source_id = source_id;
    }
}

