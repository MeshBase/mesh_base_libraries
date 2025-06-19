package io.github.meshbase.mesh_base_core.router;

public enum ProtocolType {
    SEND_MESSAGE,
    RECEIVE_MESSAGE,
    ACK,
    UNKNOWN_MESSAGE_TYPE,
    RAW_BYTES_MESSAGE,
    FILE_TRANSFER,
    RREQ,
    RREP,
    PING,
    // add more type here
}
