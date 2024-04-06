# AnotherGlass for Google Glass

Companion application to handle **Google Glass Explorer Edition** communication with a phone without official My Glass application and related Google Services.

Currently, the application can:
 * pass GPS data from the phone to the Glass
 * forwarding ongoing and one-shot notifications to the Glass
 * pass WiFi network information (SSID and password) from the phone to the Glass

## Building

Project consist of 3 modules:
* **glass** - Google Glass application, containing Client Service
* **mobile** - companion application for mobile device, containing Host Service
* **shared** - a shared module with shared classes and constants

## Running
1. Pair the Glass with the phone.
2. Install both apps
3. Toggle service switch on the phone application to start Host Service
4. On Glass, use a 'Sign in' menu option ('Help me sign in' voice command) to start Client Service

## Details

Originally, Glass application was serving as a host, and mobile was supposed to connect to it when some updates needed to be passed over, like notification status change or URL intent. But it turned out that the primary use case for the service was to serve as a GPS location provider, since most of my glassware is relying directly on web backends through tethered connection, so I switched the roles. In the future, I can add some 'temporary disconnected' state, when the Glass side will disconnect from the mobile app, but will open listening port so mobile app can 'knock' to re-instantiate the connection.

Uses Java object stream to send data to Google Glass, since I don't want to mess with protocol buffers yet.

## AnotherGlass Plans

* Add version for Google Glass Enterprise Edition 2 (Android 8.1). Will require a separate module, since it lacks stock Glass APIs like LiveCards, and also needs to handle permissions and so on. Latest firmware has known issues with Bluetooth, so I will need to add WiFi tethering support mode.
* Rewrite the connections loops, so listening thread will be waiting for the incoming messages, and sending will happen in a separate thread (how I have peek/sleep cycle typical for desktop applications).
* Handle Bluetooth reconnects (Glass sometimes loses the Bluetooth connection and immediately reconnects, but I stop the service for now).
* Add one time notification stack activity (easy, but lazy).
* Add notifications filter options and better display.
* Try to handle notifications action buttons on the Glass.
* Make a better Service Dashboard LiveCard.
* Enable/disable GPS dynamically (not sure if we can track it on the Glass though, but can add a toggle intent for use in my GlassWare at least).
* Bi-directional intent routing (navigation, sharing). Actually, done in separate repo due to tight integration with private GlassWare.
* Add file logging on both devices for debugging.
* Map zoom levels selector.

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
