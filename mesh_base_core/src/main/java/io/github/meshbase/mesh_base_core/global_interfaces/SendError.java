package io.github.meshbase.mesh_base_core.global_interfaces;

public class SendError extends Exception {
  String message;

  public SendError(String message) {
    this.message = message;
  }
}
