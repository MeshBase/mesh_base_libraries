import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:mesh_base_flutter/mesh_base_flutter.dart';

final PREFIX = "my_example_app";

class ExampleTestScreen extends StatefulWidget {
  const ExampleTestScreen({super.key});
  @override
  State<ExampleTestScreen> createState() => _BleTestScreenState();
}

class _BleTestScreenState extends State<ExampleTestScreen> {
  final mesh = MeshBaseFlutter();
  final List<Device> _connected = [];
  bool _meshOn = false, _bleOn = false;
  String _selfId = '';
  String _message = '';

  late final MeshManagerListener _listener;

  @override
  void initState() {
    super.initState();
    _listener = MeshManagerListener(
      onDataReceivedForSelf: _handleData,
      onStatusChange: _updateStatus,
      onNeighborConnected: (d) => setState(() => _connected.add(d)),
      onNeighborDisconnected:
          (d) =>
              setState(() => _connected.removeWhere((x) => x.uuid == d.uuid)),
      onError: (e) => _showSnack('Error: $e'),
    );

    // Subscribe and turn on
    debugPrint("$PREFIX subscribing");
    mesh.subscribe(_listener).then((_) async {
      debugPrint("$PREFIX turning on");
      await mesh.turnOn();
      debugPrint("$PREFIX turned on");
    });

    mesh.getId().then((id) {
      setState(() {
        _selfId = id;
      });
    });
  }

  @override
  void dispose() {
    mesh.unsubscribe();
    super.dispose();
  }

  void _handleData(MeshProtocol protocol) {
    final text = utf8.decode(protocol.body);
    _showSnack('Recv: "$text" from ${protocol.sender}');

    if (protocol.messageType == ProtocolType.RAW_BYTES_MESSAGE) {
      // auto‐reply
      final replyText = 'Reply to "$text"';
      final reply = MeshProtocol(
        messageType: ProtocolType.RAW_BYTES_MESSAGE,
        remainingHops: 10,
        messageId: protocol.messageId,
        sender: protocol.destination,
        destination: protocol.sender,
        body: Uint8List.fromList(utf8.encode(replyText)),
      );
      mesh.send(protocol: reply, keepMessageId: true).then((res) {
        if (res.acked) {
          _showSnack('Reply acked');
        } else if (res.error != null) {
          _showSnack('Reply error: ${res.error}');
        } else if (res.response != null) {
          _showSnack(
            'Reply received (should not happen): ${res.response.toString()}',
          );
        }
      });
    }
  }

  void _updateStatus(MeshStatus status) {
    debugPrint("$PREFIX new status update");
    setState(() {
      _meshOn = status.isOn;
      _bleOn = status.statuses[ConnectionType.BLE]?.isOn ?? false;
    });
  }

  void _showSnack(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), duration: const Duration(seconds: 1)),
    );
  }

  @override
  Widget build(BuildContext c) {
    return Column(
      children: [
        Text(
          'Mesh: ${_meshOn ? 'ON' : 'OFF'}    BLE: ${_bleOn ? 'ON' : 'OFF'}',
        ),
        const SizedBox(height: 8),
        ElevatedButton(
          onPressed: () => _meshOn ? mesh.turnOff() : mesh.turnOn(),
          child: Text(_meshOn ? 'Turn Off' : 'Turn On'),
        ),
        const SizedBox(height: 16),
        Expanded(
          child: ListView.builder(
            itemCount: _connected.length,
            itemBuilder: (_, i) {
              final d = _connected[i];
              return ListTile(
                title: Text('${d.name} (${d.uuid})'),
                trailing: ElevatedButton(
                  onPressed: () {
                    final protocol = MeshProtocol(
                      messageType: ProtocolType.RAW_BYTES_MESSAGE,
                      remainingHops: -1,
                      messageId: -1,
                      sender: _selfId,
                      destination: d.uuid,
                      body: Uint8List.fromList(utf8.encode(_message)),
                    );
                    mesh.send(protocol: protocol, keepMessageId: false).then((
                      res,
                    ) {
                      if (res.acked) {
                        _showSnack('Ack for "$_message"');
                      } else if (res.error != null) {
                        _showSnack('Send error: ${res.error}');
                      } else if (res.response?.body != null) {
                        _showSnack(
                          "Response received from :${d.uuid} ${utf8.decode(res.response!.body)}",
                        );
                      }
                    });
                  },
                  child: const Text('Send'),
                ),
              );
            },
          ),
        ),

        TextButton(
          onPressed: () async {
            final protocol = MeshProtocol(
              messageType: ProtocolType.RAW_BYTES_MESSAGE,
              remainingHops: -1,
              messageId: -1,
              sender: _selfId,
              destination: BROADCAST_UUID,
              body: Uint8List.fromList(utf8.encode(_message)),
            );
            mesh.send(protocol: protocol, keepMessageId: false).then((res) {
              if (res.acked) {
                _showSnack('Ack for broadcast"$_message"');
              } else if (res.error != null) {
                _showSnack('Send error: ${res.error}');
              } else if (res.response?.body != null) {
                _showSnack(
                  'Response received (should not happen for broadcast): ${utf8.decode(res.response!.body)}',
                );
              }
            });
          },
          child: Text("broadcast"),
        ),
        TextField(
          decoration: const InputDecoration(labelText: 'Message'),
          onChanged: (v) => _message = v,
        ),
      ],
    );
  }
}
