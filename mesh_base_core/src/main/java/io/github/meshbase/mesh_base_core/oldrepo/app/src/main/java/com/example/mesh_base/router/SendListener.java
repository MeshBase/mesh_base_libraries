package com.example.mesh_base.router;

import com.example.mesh_base.global_interfaces.SendError;

public interface SendListener {

  void onError(SendError error);

  void onAck();

  void onResponse(MeshProtocol<?> protocol);
}
