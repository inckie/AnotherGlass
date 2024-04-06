package com.damn.anotherglass.glass.ee.host.core

object ConnectionUtils {
    fun isPossiblyTethering(ip: String): Boolean {
        // Check is IP address is in the range of possible tethering addresses.
        // default tethering IP ranges addresses are
        // - USB is  192.168.42.1 and 255.255.255.0
        // - Wifi is 192.168.43.1 and 255.255.255.0
        // - BT is limited to max default of 5 connections. 192.168.44.1 to 192.168.48.1
        // and gateway 192.168.42.129,
        // but different devices can have different settings: 192.168.6.0, 192.168.200.0, etc.
        // as defined in the device config.xml config_dhcp_range/config_tether_dhcp_range values
        // or configurable in device settings.
        // We just check for 192.168. mask, and that bluetooth is not connected in the calling code
        return ip.startsWith("192.168.")
    }
}