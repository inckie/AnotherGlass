# AnotherGlass for Google Glass

Companion application to handle Google Glass communication with a phone without Glass related Google Services.
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

Uses Java object stream to send data to Google Glass, since I don't want to mess with protocol buffers yet.

## AnotherGlass Plans

* Handle sockets lifecycle nicely (keep alive packets, probably)
* Handle Bluetooth reconnects (Glass sometimes loses the connection and immediately reconnects, but I stop services for now)
* Add one time notification stack activity (easy, but lazy)
* Add notifications filter options and better display
* Try to handle notifications action buttons on the Glass
* Make a better Service Dashboard LiveCard
* Enable/disable GPS dynamically (not sure if we can track it on the Glass though, but can add a toggle intent for use in my GlassWare at least)
* Bi-directional intent routing (navigation, sharing)
* Add file logging on both devices for debugging
* Map zoom levels selector

## GlassWare Plans

I plan to write a number of small GlassWare applications to provide contextual AR information.

These applications will work with their own back-ends, and in some cases, have companion applications on the mobile to offload heavy calculations, or access some specific device data.
Also they will make use of AnotherGlass to handle initial setup and route intents.

* Face capture/recognizer
* Better navigation directions
* Basic calendar agenda viewer
* Voice recorder
* Timer
