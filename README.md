# AnotherGlass for Google Glass

Companion application to handle Google Glass communication with a phone without Glass related Google Services.
Currently, the application can:
 * pass GPS data from the phone to the Google Glass
 * pass WiFi network information (SSID and password) from the phone to the Google Glass

## Building

Project consist of 3 modules:
* **glass** - Google Glass application, containing Host Service
* **mobile** - companion application for mobile device, containing Client Service
* **shared** - a shared module with shared classes and constants

## Details

Uses Java object stream to send data to Google Glass, since I don't want to mess with protocol buffers yet.
Glass application acts as a Bluetooth server, so phone can connect and disconnect when needed to save power (not sure how useful this is).

## AnotherGlass Plans

* Handle sockets lifecycle nicely
* Make a better Dashboard LiveCard
* Route notifications to the glass as 'My Glass' app were doing.
* Enable/disable GPS dynamically
* Bi-directional intent routing (navigation, sharing)

Notifications routing will require finding some way to add cards to the timeline (it was removed from API in latest firmware), or use some another way to present it, like live card.

## GlassWare Plans

I plan to write a number of small GlassWare applications to provide contextual AR information.

These applications will work with their own back-ends, and in some cases, have companion applications on the mobile to offload heavy calculations, or access some specific device data.
Also they will make use of AnotherGlass to handle initial setup and route intents.

* Face capture/recognizer
* Better navigation directions
* Basic calendar agenda viewer
* Voice recorder
