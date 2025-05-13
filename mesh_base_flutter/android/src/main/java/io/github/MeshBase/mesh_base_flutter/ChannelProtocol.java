package io.github.MeshBase.mesh_base_flutter;


import java.util.Objects;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.github.meshbase.mesh_base_core.router.MeshSerializer;
import io.github.meshbase.mesh_base_core.router.ProtocolType;

class ChannelMeshProtocol {
  public final ProtocolType messageType;
  public final int remainingHops;
  public final int messageId;
  public final String sender;       // UUID.toString()
  public final String destination;  // UUID.toString()
  public final byte[] body;         // raw bytes

  public ChannelMeshProtocol(ProtocolType messageType,
                      int remainingHops,
                      int messageId,
                      String sender,
                      String destination,
                      byte[] body) {
    this.messageType    = messageType;
    this.remainingHops  = remainingHops;
    this.messageId      = messageId;
    this.sender         = sender;
    this.destination    = destination;
    this.body           = body;
  }

  public Map<String, Object> toMap() {
    Map<String,Object> m = new HashMap<>();
    m.put("messageType",   messageType.name());
    m.put("remainingHops", remainingHops);
    m.put("messageId",     messageId);
    m.put("sender",        sender);
    m.put("destination",   destination);

    ArrayList<Object> rawBody = new ArrayList<>(body.length);
    for (byte b : body) {
      rawBody.add(b & 0xFF);
    }
    m.put("body", rawBody);

    return m;
  }

  public static ChannelMeshProtocol fromMap(Map<String, Object> m) {
    ProtocolType messageType     = ProtocolType.valueOf ((String) Objects.requireNonNull(m.get("messageType")));
    int remainingHops   = ((Number) Objects.requireNonNull(m.get("remainingHops"))).intValue();
    int messageId       = ((Number) Objects.requireNonNull(m.get("messageId"))).intValue();
    String sender       = (String) Objects.requireNonNull(m.get("sender"));
    String destination  = (String) Objects.requireNonNull(m.get("destination"));

    byte[] body = (byte[]) Objects.requireNonNull(m.get("body"));
    return new ChannelMeshProtocol(
        messageType,
        remainingHops,
        messageId,
        sender,
        destination,
        body
    );
  }
}
