package io.github.meshbase.mesh_base_core.router;

import io.github.meshbase.mesh_base_core.global_interfaces.SendError;

public interface SendListener {

  void onError(SendError error);

  void onAck();

  void onResponse(MeshProtocol<?> protocol);
}
