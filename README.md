
# AnotherGlass for Google Glass

![Logo](mobile/src/main/ic_launcher-playstore.png "Logo")

Companion application to handle **Google Glass Explorer Edition** and **Google Glass Enterprise Edition** communication with a phone without the official "My Glass" application and related Google Services.

Currently, the application can:
 * pass GPS data from the phone to the Glass
 * forward ongoing and one-shot notifications to the Glass
 * control media playback on the phone (play/pause, next, previous) directly from Glass
 * perform tilt to wake up on Enterprise Edition like on Explorer Edition (be aware of battery usage)
 * pass WiFi network information (SSID and password) from the phone to the Glass (Explorer Edition only)

## Building

Project consist of 5 modules:
* **glass-xe** - Google Glass Explorer Edition application, containing Client Service
* **glass-ee** - Google Glass Enterprise Edition application, containing Client Service
* **mobile** - companion application for mobile device, containing Host Service
* **shared** - a shared module with shared classes and constants
* **shared-glass** - a shared module between Glass applications

Open and build in Android studio or using gradle command line.
Builds can be signed by adding following properties in the root `local.properties` file:

```
keystore_path=<path to key store>
keystore_password=<keystore password>
key_alias=<key alias>
key_password=<key password>
```

## Running

### Google Glass Explorer Edition

Supports both Bluetooth and WiFi connection modes.

1. Install both applications.
2. Start Host Service from the phone app.
3. On Glass, trigger/start AnotherGlass and choose connection mode:
   - `Bluetooth` to connect over paired Bluetooth.
   - `Wi-Fi` to connect using current network gateway IP.
   - `Scan Barcode` to scan server IP from QR code (`ip` or `ip|label` format).
4. If needed, use the long-press options and menu cards to reconnect using gateway IP or last scanned IP.

For Bluetooth mode:
1. Pair the Glass with the phone.
2. Install both applications.
3. Select Bluetooth connection mode in the phone application.
4. Start service on Glass and choose `Bluetooth`.

For WiFi mode:
1. Ensure Glass and phone are on the same network, or use phone hotspot.
2. Start service on Glass and choose `Wi-Fi`, or `Scan Barcode` to provide an explicit server IP.
3. If QR scan succeeds, the scanned IP is remembered and shown as a quick reconnect option.

### Google Glass Enterprise Edition

Uses WiFi to connect to the phone. Bluetooth is not supported (it has issues connecting from UI on latest firmware, and can only be connected from hidden Applications settings on my device, but disconnects often).

1. Setup and start WiFi hotspot on the phone.
2. Connect the Glass to the phone's WiFi hotspot.
3. Install both applications.
4. Select WiFi connection mode in the mobile application.
5. Toggle service switch on the phone application to start Host Service. Accept the permissions requests.
6. Run AnotherGlass application the Glass device from the launcher. Accept the permission requests.

Connection service keeps running in the background even if application is closed. Use swipe up or two fingers swipe down gestures to stop the service when in the application.

Without using a hotspot, if both devices are on the same WiFi network, you can generate a QR code with the phone local IP address and scan it on the Glass by long-pressing the Service state card and selecting `Barcode` option.

To enable GPS location passthrough on the Glass you will need to give the applicationMOCK_LOCATION permission. Run ADB command `adb shell appops set com.damn.anotherglass.glass.ee android:mock_location allow`, or tap on the Map card to open Developer settings and set AnotherGlass as GPS mocking application. Restart the service after the change.

## Details

Originally, Glass application was serving as a host, and mobile was supposed to connect to it when some updates needed to be passed over, like notification status change or URL intent. But it turned out that the primary use case for the service was to serve as a GPS location provider, since most of my glassware is relying directly on web backends through tethered connection, so I switched the roles. In the future, I can add some 'temporary disconnected' state, when the Glass side will disconnect from the mobile application, but will open listening port so mobile application can 'knock' to re-instantiate the connection.

Can use Java object stream or JSON Lines to send data to Google Glass, since I don't want to mess with protocol buffers yet.

## Media Playback Controls

While the Host Service is running, AnotherGlass mirrors the currently playing media session from the phone to Glass in near real-time. Any app that integrates with the Android media system (Spotify, YouTube, YouTube Music, Podcast apps, etc.) is supported automatically — no extra setup is required.

**What you see:**
- The current track title, artist, and playback state are shown on the phone app UI.
- **Explorer Edition:** a dedicated media live card appears in the Glass timeline whenever something is playing. Tap it to open the playback controls screen.
- **Enterprise Edition:** a media card appears in the Glass timeline when a session is active.

**Controls from Glass:**
- **Tap** — play / pause toggle.
- **Double-tap** — skip to next track.
- **Explorer Edition playback screen:** swipe left/right to reach dedicated Previous and Next track cards; tap either to send that command.

Controls that the source app does not support are automatically disabled based on what the app reports as available actions.

The media card disappears automatically when there is nothing playing.

## Debug Python client
There is a simple Python client in `python` folder to test the Glass Enterprise application without the mobile application. It can send fake GPS coordinates, notifications, and simulated media playback state to the Glass, and also receives and handles media control commands sent back from the Glass.
Make sure to change communication protocol to JSON Lines by setting `SerializerProvider.currentSerializer` to `JSON`.

## APK Server
There is a small GUI tool in `python/apk_server.py` to simplify sideloading APK files onto the Glass over WiFi without ADB.

1. Run `apk_server.py` (requires `qrcode` and `Pillow` from `python/requirements.txt`).
2. The tool scans its own directory for `.apk` files and lists them.
3. Select the APK you want to install and press **Start** — a local HTTP server starts serving that file.
4. A QR code with the download URL is displayed. Scan it on the Glass using the `Scan Barcode` connection option (or any QR scanner), then open the URL in a browser or download manager to install the APK.
5. Press **Stop** or close the window when done.

## AnotherGlass Plans

### General

* Rewrite the connections loops, so listening thread will be waiting for the incoming messages, and sending will happen in a separate thread (how I have peek/sleep cycle typical for desktop applications).
* Enable/disable GPS dynamically (not sure if we can track it on the Glass though, but I can add a toggle intent/proxy service binding for use in my GlassWare at least).
* Bi-directional intent routing (navigation, sharing). Actually, done in separate repo due to tight integration with private GlassWare.
* Add file logging on both the Glass applications for debugging.
* Map Card zoom levels selector.

### Glass Enterprise Edition

* Improve initial connect experience on the Glass Enterprise Edition:
   * handle permissions and MockGPS status changes without restarting the service.
* Add option to run service without mobile application connection to simulate Explorer Edition features:
    * Add Voice Commands
    * Implement WidgetHost to simulate Explorer Edition Timeline LiveCards
    * Add Notifications "Tail" to simulate Explorer Edition
    * Probably replace Launcher (need to add shortcut to Glass Settings then)
* Add Bluetooth connection mode.

### Glass Explorer Edition
* Make a better Service Dashboard LiveCard.
* Try to handle notifications action buttons on the Glass.
* Add Dismiss command to notifications (send message to mobile application).
* Add notifications filter options and better display.
* Add one time notification stack activity (easy, but lazy).
* Handle Bluetooth reconnects (Glass sometimes loses the Bluetooth connection and immediately reconnects, but I stop the service for now).

## GlassWare Plans

I plan to write a number of small GlassWare applications to provide contextual AR information.

These applications will work with their own back-ends, and in some cases, have companion applications on the mobile to offload heavy calculations, or access some specific device data.
Also they will make use of AnotherGlass to handle initial setup and route intents.

* Face capture/recognizer (written, but not public)
* Better navigation directions (written, but not public)
* Basic calendar agenda viewer
* Voice recorder
* Timer
* Better QR2 scanner
