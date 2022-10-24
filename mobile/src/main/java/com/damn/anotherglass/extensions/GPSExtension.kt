package com.damn.anotherglass.extensions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.R
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.logging.ALog
import com.damn.anotherglass.shared.RPCMessage
import com.damn.anotherglass.shared.gps.GPSServiceAPI

@SuppressLint("MissingPermission")
class GPSExtension(private val service: GlassService) : LocationListener {
    private val locationManager: LocationManager =
        service.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val log = ALog(Logger.get(TAG))

    fun start() {
        if(!hasGeoPermission(service)) {
            log.w(TAG, "Permission not granted, cannot start GPS")
            service.settings.isGPSEnabled = false
            Toast.makeText(service, R.string.msg_no_gps_permission, Toast.LENGTH_LONG).show()
            return
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_TIME_BW_UPDATES,
            0f,
            this
        )
        log.i(TAG, "GPS extension started")
    }

    fun stop() {
        locationManager.removeUpdates(this)
        log.i(TAG, "GPS extension stopped")
    }

    override fun onLocationChanged(location: Location) {
        log.d(TAG, "GPS extension received location update")
        val loc = com.damn.anotherglass.shared.gps.Location()
        loc.accuracy = location.accuracy
        loc.latitude = location.latitude
        loc.longitude = location.longitude
        loc.altitude = location.altitude
        loc.bearing = location.bearing
        loc.speed = location.speed
        val rpcMessage = RPCMessage(GPSServiceAPI.ID, loc)
        service.send(rpcMessage)
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
    override fun onProviderEnabled(s: String) {}
    override fun onProviderDisabled(s: String) {}

    companion object {
        private const val TAG = "GPSExtension"
        private const val MIN_TIME_BW_UPDATES = 30 * 1000L // 30 seconds

        @JvmStatic
        fun hasGeoPermission(context: Context): Boolean =
            gpsPermissions.all {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    context,
                    it
                )
            }

        @JvmStatic
        val gpsPermissions =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            else
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
    }
}