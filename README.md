
# AnotherGlass for Google Glass

![Logo](mobile/src/main/ic_launcher-playstore.png "Logo")

Companion application to handle **Google Glass Explorer Edition** and **Google Glass Enterprise Edition** communication with a phone without the official "My Glass" application and related Google Services.

Currently, the application can:
 * pass GPS data from the phone to the Glass
 * forwarding ongoing and one-shot notifications to the Glass (Explorer Edition only)
 * pass WiFi network information (SSID and password) from the phone to the Glass (Explorer Edition only)

## Building

Project consist of 5 modules:
* **glass** - Google Glass Explorer Edition application, containing Client Service
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

Uses Bluetooth to connect to the phone. WiFi tethering is not supported yet.

1. Pair the Glass with the phone.
2. Install both applications.
3. Select Bluetooth connection mode in the mobile application.
4. Toggle service switch on the phone application to start Host Service.
5. On the Glass, use a 'Sign in' menu option ('Help me sign in' voice command) to start Client Service.

### Google Glass Enterprise Edition

Uses WiFi to connect to the phone. Bluetooth is not supported (it has issues connecting from UI on latest firmware, and can only be connected from hidden Applications settings on my device, but disconnects often).

1. Setup and start WiFi hotspot on the phone.
2. Connect the Glass to the phone's WiFi hotspot.
3. Install both applications.
4. Select WiFi connection mode in the mobile application.
5. Toggle service switch on the phone application to start Host Service. Accept the permissions requests.
6. Run AnotherGlass application the Glass device from the launcher. Accept the permission requests.

Connection service keeps running in the background even if applicationis closed. Use swipe up or two fingers swipe down gestures to stop the service when in the application.

To enable GPS location passthrough on the Glass you will need to give the applicationMOCK_LOCATION permission. Run ADB command `adb shell appops set com.damn.anotherglass.glass.ee android:mock_location allow`, or tap on the Map card to open Developer settings and set AnotherGlass as GPS mocking application. Restart the service after the change.

## Details

Originally, Glass application was serving as a host, and mobile was supposed to connect to it when some updates needed to be passed over, like notification status change or URL intent. But it turned out that the primary use case for the service was to serve as a GPS location provider, since most of my glassware is relying directly on web backends through tethered connection, so I switched the roles. In the future, I can add some 'temporary disconnected' state, when the Glass side will disconnect from the mobile application, but will open listening port so mobile applicationcan 'knock' to re-instantiate the connection.

Uses Java object stream to send data to Google Glass, since I don't want to mess with protocol buffers yet.

## AnotherGlass Plans

### General

* Rewrite the connections loops, so listening thread will be waiting for the incoming messages, and sending will happen in a separate thread (how I have peek/sleep cycle typical for desktop applications).
* Enable/disable GPS dynamically (not sure if we can track it on the Glass though, but I can add a toggle intent/proxy service binding for use in my GlassWare at least).
* Bi-directional intent routing (navigation, sharing). Actually, done in separate repo due to tight integration with private GlassWare.
* Add file logging on both the Glass applications for debugging.
* Map Card zoom levels selector.

### Glass Enterprise Edition

* Improve initial connect experience on the Glass Enterprise Edition:
   * add service start/stop menu on Home card
   * move GPS permissions handling to the Map card and make the permissions optional to start the service
   * handle permissions and MockGPS status changes without restarting the service.
* Add option to run service without mobile application connection to simulate Explorer Edition features:
    * Add Tilt to Wake feature
    * Add Voice Commands
    * Implement WidgetHost to simulate Explorer Edition Timeline LiveCards
    * Add Notifications "Tail" to simulate Explorer Edition
    * Probably replace Launcher (need to add shortcut to Glass Settings then)
* Add Notifications mirroring support.
* Add Bluetooth connection mode.

### Glass Explorer Edition
* Make a better Service Dashboard LiveCard.
* Try to handle notifications action buttons on the Glass.
* Add Dismiss command to notifications (send message to mobile application).
* Add notifications filter options and better display.
* Add one time notification stack activity (easy, but lazy).
* Add WiFi connection mode.
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
