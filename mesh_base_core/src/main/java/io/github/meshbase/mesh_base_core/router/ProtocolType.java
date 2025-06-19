package io.github.meshbase.mesh_base_core.router;

public enum ProtocolType {
    SEND_MESSAGE,
    RECEIVE_MESSAGE,
    ACK,
    UNKNOWN_MESSAGE_TYPE,
    RAW_BYTES_MESSAGE,
    RREQ,
    RREP,
    PING,
    KEY_SHARE,
    // add more type here
}
