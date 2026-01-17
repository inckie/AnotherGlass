# AnotherGlass - Copilot Instructions

## Project Overview

AnotherGlass is a companion application suite for **Google Glass Explorer Edition (XE)** and **Google Glass Enterprise Edition (EE)** that enables communication with a phone without requiring the official "My Glass" application or Google Services.

### Core Capabilities
- GPS data passthrough from phone to Glass
- Notification forwarding (ongoing and one-shot)
- Tilt-to-wake functionality (EE)
- WiFi network configuration sharing (XE)

## Architecture

### Module Structure

```
AnotherGlass/
├── mobile/          # Android companion app (phone)
├── glass-xe/        # Google Glass Explorer Edition app
├── glass-ee/        # Google Glass Enterprise Edition app
├── shared/          # Shared classes & RPC protocol
├── glass-shared/    # Shared code between Glass apps (GPS mocking, notifications)
├── externals/       # Git submodules (x-ray logging, glass-enterprise-samples)
└── python/          # Debug Python client for testing glass-ee
```

### Communication Flow

```
┌─────────────────┐         RPC         ┌─────────────────┐
│     Mobile      │ ◄──────────────────► │   Glass XE/EE   │
│  (Host/Server)  │   Bluetooth/WiFi    │    (Client)     │
└─────────────────┘                     └─────────────────┘
```

- **Mobile app** acts as the **Host** - runs `GlassService` which manages connections
- **Glass apps** act as **Clients** - run `HostService` (confusing naming, but it's the Glass-side service)
- **XE** uses **Bluetooth** connection
- **EE** uses **WiFi** connection (connects to phone's hotspot)

### Key Classes

| Module | Key Class | Description |
|--------|-----------|-------------|
| `mobile` | `GlassService` | Main service managing connections and extensions |
| `mobile` | `BluetoothHost` / `WiFiHost` | Connection implementations |
| `mobile` | `GPSExtension` / `NotificationExtension` | Feature providers |
| `glass-xe` | `HostService` | Glass XE service with LiveCard UI |
| `glass-xe` | `BluetoothClient` | Bluetooth connection client |
| `glass-ee` | `HostService` | Glass EE service |
| `glass-ee` | `WiFiClient` | WiFi connection client |
| `shared` | `RPCMessage` / `RPCHandler` | RPC protocol implementation |
| `shared` | `IMessageSerializer` | JSON or Java object serialization |
| `glass-shared` | `MockGPS` | GPS location mocking provider |
| `glass-shared` | `NotificationController` | Notification state management |

### RPC Protocol

Messages are exchanged via `RPCMessage` containing:
- `service`: Service identifier (e.g., `"MockGPS"`, `"Notifications"`, `"device"`)
- `type`: Payload class name
- `payload`: Data object (e.g., `Location`, `NotificationData`, `BatteryStatusData`)

Serialization options (configured in `SerializerProvider`):
- Java Object Streams (default)
- JSON Lines (required for Python debug client)

## Build System

### Requirements
- **JDK 17** (for mobile, glass-ee)
- **JDK 8** (for glass-xe due to GDK constraints)
- **Android SDK** with API levels 19, 27-36
- **Glass Development Kit Preview** (for glass-xe)

### Building

```bash
# Full build
./gradlew assemble

# Individual modules
./gradlew :mobile:assembleDebug
./gradlew :glass-xe:assembleDebug
./gradlew :glass-ee:assembleDebug
```

### Signing (Optional)

Add to `local.properties`:
```properties
keystore_path=<path to key store>
keystore_password=<keystore password>
key_alias=<key alias>
key_password=<key password>
```

## Technology Stack

| Module | Language | Min SDK | Key Libraries |
|--------|----------|---------|---------------|
| `mobile` | Kotlin | 23 | Jetpack Compose, Navigation, Coroutines, EventBus, ZXing |
| `glass-xe` | Java/Kotlin | 19 | Glass GDK, Picasso |
| `glass-ee` | Kotlin | 27 | CameraX, Coil, EventBus, ViewBinding |
| `shared` | Java | 19 | Gson, AndroidX Annotations |
| `glass-shared` | Kotlin | 19 | Coroutines |

## Common Development Tasks

### Adding a New Extension (Mobile)

1. Create extension class in `mobile/src/main/java/com/damn/anotherglass/extensions/`
2. Implement start/stop lifecycle methods
3. Use `GlassService.send(RPCMessage)` to send data
4. Register in `GlassService.onCreate()`

### Adding a New Data Type

1. Define data class in `shared/src/main/java/com/damn/anotherglass/shared/`
2. Must implement `Serializable` for Java object serialization
3. Add routing logic in Glass `HostService.route()` or `onDataReceived()`

### Testing Glass EE Without Phone

Use the Python debug client:
```bash
cd python
pip install -r requirements.txt
python client.py --ui  # GUI mode
python client.py       # Console mode
```
**Note**: Set `SerializerProvider.currentSerializer` to `JSON` for Python compatibility.

## Glass-Specific Considerations

### Explorer Edition (XE)
- Uses Glass GDK with LiveCard API
- Bluetooth pairing required before use
- Voice commands via "Help me sign in" trigger
- Limited to Java 8 due to SDK constraints

### Enterprise Edition (EE)
- Standard Android with custom UI patterns
- Gesture-based navigation (swipe, tap)
- QR code scanning for WiFi/IP configuration
- Requires MOCK_LOCATION permission for GPS: 
  ```bash
  adb shell appops set com.damn.anotherglass.glass.ee android:mock_location allow
  ```

## Code Style

- Kotlin preferred for new code (except glass-xe)
- Use coroutines for async operations
- Follow Android lifecycle patterns
- Prefix member variables with `m` in existing Java code
- Use EventBus for cross-component communication on Glass

## Git Submodules

Initialize submodules after clone:
```bash
git submodule update --init --recursive
```

Submodules:
- `externals/x-ray/` - Logging framework
- `externals/glass-enterprise-samples/` - Glass EE gesture library
