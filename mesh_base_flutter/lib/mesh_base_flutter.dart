import 'dart:typed_data';

import 'mesh_base_flutter_platform_interface.dart';

class MeshBaseFlutter {
  Future<String?> getPlatformVersion() {
    return MeshBaseFlutterPlatform.instance.getPlatformVersion();
  }

  Future<String> getId() async {
    return await MeshBaseFlutterPlatform.instance.getId();
  }

  Future<void> turnOn() {
    return MeshBaseFlutterPlatform.instance.on();
  }

  Future<void> turnOff() {
    return MeshBaseFlutterPlatform.instance.off();
  }

  Future<List<Device>> getNeighbors() {
    return MeshBaseFlutterPlatform.instance.getNeighbors();
  }

  Future<MeshStatus> getStatus() {
    return MeshBaseFlutterPlatform.instance.getStatus();
  }

  Future<SendResult> send({
    required MeshProtocol protocol,
    bool keepMessageId = false,
  }) {
    return MeshBaseFlutterPlatform.instance.send(
      protocol: protocol,
      keepMessageId: keepMessageId,
    );
  }

  Future<void> subscribe(MeshManagerListener listener) {
    return MeshBaseFlutterPlatform.instance.subscribe(listener);
  }

  Future<void> unsubscribe() {
    return MeshBaseFlutterPlatform.instance.unsubscribe();
  }
}

enum ConnectionType { BLE }

class Device {
  final String uuid;
  final String name;

  Device({required this.uuid, required this.name});

  factory Device.fromMap(Map map) {
    return Device(uuid: map['uuid'], name: map['name']);
  }

  Map<String, dynamic> toMap() => {'uuid': uuid, 'name': name};
}

class StatusProperty {
  final bool isSupported;
  final bool isOn;
  final bool isAllowed;

  StatusProperty({
    required this.isSupported,
    required this.isOn,
    required this.isAllowed,
  });

  factory StatusProperty.fromMap(Map map) {
    return StatusProperty(
      isSupported: map['isSupported'],
      isOn: map['isOn'],
      isAllowed: map['isAllowed'],
    );
  }
}

class MeshStatus {
  final bool isOn;
  final Map<ConnectionType, StatusProperty> statuses;

  MeshStatus({required this.isOn, required this.statuses});

  factory MeshStatus.fromMap(Map map) {
    final connectionStatuses = <ConnectionType, StatusProperty>{};
    (map['statuses'] as Map).forEach((key, value) {
      connectionStatuses[ConnectionType.values.byName(
        key,
      )] = StatusProperty.fromMap(value);
    });

    return MeshStatus(isOn: map['isOn'], statuses: connectionStatuses);
  }
}

class SendError extends Error {
  final String message;

  SendError({required this.message});

  factory SendError.fromMap(Map map) {
    return SendError(message: map['message'] ?? 'Unknown error');
  }

  @override
  String toString() => 'SendError: $message';
}

class MeshError extends Error {
  final String message;

  MeshError({required this.message});

  factory MeshError.fromMap(Map map) {
    return MeshError(message: map['message'] ?? 'Unknown error');
  }

  @override
  String toString() => 'MeshError: $message';
}

class SendResult {
  final SendError? error;
  final bool acked;
  final MeshProtocol? response;

  SendResult({
    required this.acked,
    required this.error,
    required this.response,
  });

  factory SendResult.fromMap(Map map) {
    return SendResult(
      acked: map['acked'] as bool? ?? false,
      error: map['error'] != null ? SendError.fromMap(map['error']) : null,
      response:
          map['response'] != null
              ? MeshProtocol.fromMap(map['response'])
              : null,
    );
  }
}

//TODO: consider what protocols and how they are used in java vs dart side
enum ProtocolType {
  SEND_MESSAGE,
  RECEIVE_MESSAGE,
  ACK,
  UNKNOWN_MESSAGE_TYPE,
  RAW_BYTES_MESSAGE,

  // keep in sync with java's enums
}

class MeshProtocol {
  final ProtocolType messageType;
  final int remainingHops;
  final int messageId;
  final String sender;
  final String destination;
  final Uint8List body;

  MeshProtocol({
    required this.messageType,
    required this.remainingHops,
    required this.messageId,
    required this.sender,
    required this.destination,
    required this.body,
  });

  Map<String, dynamic> toMap() => {
    'messageType': messageType.name,
    'remainingHops': remainingHops,
    'messageId': messageId,
    'sender': sender,
    'destination': destination,
    'body': body,
  };

  factory MeshProtocol.fromMap(Map map) {
    return MeshProtocol(
      messageType: ProtocolType.values.byName(map['messageType'] as String),
      remainingHops: map['remainingHops'] as int,
      messageId: map['messageId'] as int,
      sender: map['sender'] as String,
      destination: map['destination'] as String,
      body:
          map['body'] != null
              ? Uint8List.fromList(List<int>.from(map['body'] as List))
              : Uint8List(0),
    );
  }
}

class MeshManagerListener {
  final Function(MeshProtocol data)? onDataReceivedForSelf;
  final Function(MeshStatus status)? onStatusChange;
  final Function(Device device)? onNeighborConnected;
  final Function(Device device)? onNeighborDisconnected;
  final Function(MeshError error)? onError;

  MeshManagerListener({
    this.onDataReceivedForSelf,
    this.onStatusChange,
    this.onNeighborConnected,
    this.onNeighborDisconnected,
    this.onError,
  });
}
