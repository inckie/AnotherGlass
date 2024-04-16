package com.damn.anotherglass.glass.ee.host.core

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.damn.anotherglass.shared.gps.GPSServiceAPI
import com.damn.anotherglass.shared.gps.Location
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.rpc.RPCMessageListener
import com.damn.glass.shared.gps.MockGPS

// lets pretend for now its actually a Service
class HostService(private val context: Context) {

    private val client = WiFiClient()

    private val gps = MockGPS(context)

    fun start() {
        Log.i(TAG, "HostService started")
        val listener: RPCMessageListener = object : RPCMessageListener {
            override fun onWaiting() {
                Log.d(TAG, "Waiting")
            }

            override fun onConnectionStarted(device: String) {
                Log.d(TAG, "Connected to $device")
            }

            override fun onDataReceived(data: RPCMessage) {
                when(data.service) {
                    GPSServiceAPI.ID -> {
                        Log.d(TAG, "GPS data received")
                        if (data.type.equals(Location::class.java.getName()))
                            gps.publish(data.payload as Location)
                    }
                    else -> Log.e(TAG, "Unknown service: ${data.service}")
                }
            }

            override fun onConnectionLost(error: String?) {
                Log.e(TAG, "onConnectionLost: $error");
            }

            override fun onShutdown() {
                Log.d(TAG, "onShutdown")
            }
        }
        try {
            gps.start()
        } catch (e: SecurityException) {
            // Requires MOCK_LOCATION permission given through ADB
            // `adb shell appops set <id> android:mock_location allow`
            // where <uid> is from exception message
            // `java.lang.SecurityException: com.damn.anotherglass.glass.ee from uid 10063 not allowed to perform MOCK_LOCATION`
            // or `adb shell appops set com.damn.anotherglass.glass.ee android:mock_location allow`
            Log.e(TAG, "GPS mocking is not enabled: $e")
            Toast.makeText(
                context,
                "GPS provider not available, please enable location mocking using ADB or developer settings",
                Toast.LENGTH_LONG
            ).show()
        }
        client.start(context, listener)
    }

    fun stop() {
        Log.i(TAG, "HostService stopped")
        gps.remove()
        client.stop()
    }

    companion object {
        private const val TAG = "HostService"
    }
}
