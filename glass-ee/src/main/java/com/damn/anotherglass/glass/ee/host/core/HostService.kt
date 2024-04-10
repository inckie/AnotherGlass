package com.damn.anotherglass.glass.ee.host.core

import android.content.Context
import android.util.Log
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
            Log.e(TAG, "GPS mocking is not enabled: $e")
        }
        client.start(context, listener)
    }

    fun stop() {
        Log.i(TAG, "HostService stopped")
        gps.remove()
    }

    companion object {
        private const val TAG = "HostService"
    }
}
