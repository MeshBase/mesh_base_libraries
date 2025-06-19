# Mesh Base Java Library

A Java library for building mesh network applications, designed for easy integration with Android and cross-platform projects (including Flutter via MethodChannels).

---

## Installation

### Using Maven Central

Add the following to your `build.gradle` dependencies:

```gradle
dependencies {
    implementation "io.github.meshbase:mesh_base_core:1.1"
}
```

Or, if you use Maven:

```xml
<dependency>
    <groupId>io.github.meshbase</groupId>
    <artifactId>mesh_base_core</artifactId>
    <version>1.1</version>
</dependency>
```

> **Note:** If you are developing locally, you may need to publish the library to your local Maven repository:
> ```sh
> ./gradlew publishToMavenLocal
> ```

---

## Getting Started

### 1. Initialize the Mesh Manager

```java
import io.github.meshbase.mesh_base_core.mesh_manager.MeshManager;

MeshManager meshManager = new MeshManager(context); // context is usually your Activity
```

### 2. Turn Mesh On/Off

```java
meshManager.on();   // Enable mesh networking
meshManager.off();  // Disable mesh networking
```

### 3. Get Device ID

```java
UUID selfId = meshManager.getId();
```

### 4. Discover Neighbors

```java
List<Device> neighbors = meshManager.getNeighbors();
for (Device device : neighbors) {
    System.out.println(device.getName() + " - " + device.getUuid());
}
```

### 5. Send a Message

```java
MeshProtocol<RawBytesBody> protocol = new ConcreteMeshProtocol<>(
    ProtocolType.RAW_BYTES_MESSAGE,
    10, // remaining hops
    -1, // messageId (auto-assigned if -1)
    selfId,
    destinationUuid,
    new RawBytesBody("Hello Mesh!".getBytes())
);

meshManager.send(protocol, new SendListener() {
    @Override
    public void onAck() {
        // Message was acknowledged
    }

    @Override
    public void onError(SendError e) {
        // Handle error
    }

    @Override
    public void onResponse(MeshProtocol<?> response) {
        // Handle response
    }
}, false); // keepMessageId
```

### 6. Subscribe to Mesh Events

```java
meshManager.subscribe(new MeshManagerListener() {
    @Override
    public void onDataReceivedForSelf(MeshProtocol<?> protocol) {
        // Handle incoming data
    }

    @Override
    public void onStatusChange(Status status) {
        // Handle status updates
    }

    @Override
    public void onNeighborConnected(Device device) {
        // Handle new neighbor
    }

    @Override
    public void onNeighborDisconnected(Device device) {
        // Handle neighbor disconnect
    }

    @Override
    public void onError(Exception e) {
        // Handle errors
    }
});
```

---

## API Overview

- **MeshManager**
    - `on()` / `off()`: Enable/disable mesh networking.
    - `getId()`: Get this device's unique mesh ID.
    - `getNeighbors()`: List currently connected mesh neighbors.
    - `send(MeshProtocol, SendListener, boolean)`: Send a message.
    - `subscribe(MeshManagerListener)`: Listen for mesh events.

- **MeshManagerListener**
    - `onDataReceivedForSelf(MeshProtocol<?>)`
    - `onStatusChange(Status)`
    - `onNeighborConnected(Device)`
    - `onNeighborDisconnected(Device)`
    - `onError(Exception)`

- **SendListener**
    - `onAck()`
    - `onError(SendError)`
    - `onResponse(MeshProtocol<?>)`

- **MeshProtocol** / **ConcreteMeshProtocol**
    - Represents a message in the mesh network.

---

## Example

```java
MeshManager meshManager = new MeshManager(activity);
meshManager.on();

meshManager.subscribe(new MeshManagerListener() {
    @Override
    public void onDataReceivedForSelf(MeshProtocol<?> protocol) {
        System.out.println("Received: " + new String(((RawBytesBody)protocol.body).getContent()));
    }
    // ... other overrides ...
});
```

---

## License

[MIT](LICENSE)

---

For more details, see the [JavaDoc](https://github.com/hayk2377/mesh) or the [example code](mesh_base_flutter/example/lib/TestScreen.dart) for cross-platform usage.

# mesh_base_flutter

A Flutter plugin for communicating with the native Java mesh library, enabling mesh networking in your Flutter apps via platform channels.

---

## Installation

Add this to your `pubspec.yaml`:

```yaml
dependencies:
  mesh_base_flutter:
    git:
      url: https://github.com/hayk2377/mesh.git
      path: mesh_base_libraries/mesh_base_flutter
```
Or, if published to pub.dev:
```yaml
dependencies:
  mesh_base_flutter: ^0.0.1
```

Then run:
```sh
flutter pub get
```

---

## Getting Started

### 1. Import the Package

```dart
import 'package:mesh_base_flutter/mesh_base_flutter.dart';
```

### 2. Initialize and Subscribe

```dart
final mesh = MeshBaseFlutter();

final listener = MeshManagerListener(
  onDataReceivedForSelf: (protocol) {
    // Handle received data
  },
  onStatusChange: (status) {
    // Handle status updates
  },
  onNeighborConnected: (device) {
    // Handle new neighbor
  },
  onNeighborDisconnected: (device) {
    // Handle neighbor disconnect
  },
  onError: (error) {
    // Handle errors
  },
);

await mesh.subscribe(listener);
await mesh.turnOn();
```

### 3. Get Device ID

```dart
String selfId = await mesh.getId();
```

### 4. Discover Neighbors

```dart
List<Device> neighbors = await mesh.getNeighbors();
```

### 5. Send a Message

```dart
final protocol = MeshProtocol(
  messageType: ProtocolType.RAW_BYTES_MESSAGE,
  remainingHops: 10,
  messageId: -1,
  sender: selfId,
  destination: neighborId,
  body: Uint8List.fromList(utf8.encode("Hello Mesh!")),
);

final result = await mesh.send(protocol: protocol, keepMessageId: false);

if (result.acked) {
  // Message was acknowledged
} else if (result.error != null) {
  // Handle error
} else if (result.response != null) {
  // Handle response
}
```

### 6. Broadcast a Message

```dart
final protocol = MeshProtocol(
  messageType: ProtocolType.RAW_BYTES_MESSAGE,
  remainingHops: -1,
  messageId: -1,
  sender: selfId,
  destination: BROADCAST_UUID,
  body: Uint8List.fromList(utf8.encode("Broadcast message")),
);

await mesh.send(protocol: protocol, keepMessageId: false);
```

### 7. Unsubscribe and Turn Off

```dart
await mesh.unsubscribe();
await mesh.turnOff();
```

---

## API Overview

- **MeshBaseFlutter**
    - `Future<void> turnOn() / turnOff()`: Enable/disable mesh networking.
    - `Future<String> getId()`: Get this device's unique mesh ID.
    - `Future<List<Device>> getNeighbors()`: List currently connected mesh neighbors.
    - `Future<MeshStatus> getStatus()`: Get mesh and BLE status.
    - `Future<SendResult> send({required MeshProtocol protocol, bool keepMessageId})`: Send a message.
    - `Future<void> subscribe(MeshManagerListener listener)`: Listen for mesh events.
    - `Future<void> unsubscribe()`: Stop listening for events.

- **MeshManagerListener**
    - `onDataReceivedForSelf(MeshProtocol data)`
    - `onStatusChange(MeshStatus status)`
    - `onNeighborConnected(Device device)`
    - `onNeighborDisconnected(Device device)`
    - `onError(dynamic error)`

- **MeshProtocol**
    - `messageType`, `remainingHops`, `messageId`, `sender`, `destination`, `body`

- **Device**
    - `uuid`, `name`

- **MeshStatus**
    - `isOn`, `statuses` (per connection type)

---

## Example

See [`example/lib/TestScreen.dart`](example/lib/TestScreen.dart) for a full UI sample.

---

## License

[MIT](LICENSE)

---

For more details, see the [Java library documentation](../mesh_base_core/README.md) or the [example app](example/).
