package com.example.mesh_base.router;

import java.util.UUID;

public class ConcreteMeshProtocol<T extends MeshSerializer<T>> extends MeshProtocol<T>{
    public ConcreteMeshProtocol(int messageType, int remainingHops, int messageId, UUID sender, UUID destination, T body) {
        super(messageType, remainingHops, messageId, sender, destination, body);
    }
}
