package com.damn.anotherglass.glass.ee.host.ui.cards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import coil.request.CachePolicy
import com.damn.anotherglass.glass.ee.host.databinding.LayoutCardMapBinding
import com.damn.anotherglass.glass.ee.host.gpsPermissions
import com.damn.anotherglass.glass.ee.host.utility.hasPermission
import com.damn.anotherglass.glass.ee.host.utility.locationManager
import java.util.Locale

class MapCard : BaseFragment(), LocationListener {

    private var root: LayoutCardMapBinding? = null
    private var lastMapUrl: String? = null

    // todo: write "integrated" location provider, that can also work on broadcasts
    // todo: add zoom in/out menu commands

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = LayoutCardMapBinding.inflate(inflater, container, false)
        .also { root = it }
        .root

    override fun onDestroyView() {
        super.onDestroyView()
        root = null
    }

    override fun onSingleTapUp() {
        super.onSingleTapUp()

        val context = requireContext()
        val locationManager = context.locationManager()
        // probably will not work on Glass
        if (!locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun onResume() {
        super.onResume()

        val context = requireContext()

        if (!hasLocationPermissions(context)) {
            root?.lblGpsStatus?.text = "GPS permissions not granted"
            return
        }

        val locationManager = context.locationManager()

        if (!locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            // usually means MockGPS is not available
            root?.lblGpsStatus?.text = "GPS provider not available. Tap to open developer settings or enable using ADB."
            // todo: also ask service to re-init mock GPS
            return
        }

        // rate and distance are managed by mobile app, so we can use 0, 0
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        root?.lblGpsStatus?.text = "Waiting for GPS signal…"
    }

    override fun onPause() {
        super.onPause()
        val context = requireContext()
        if (hasLocationPermissions(context)) {
            val locationManager = context.locationManager()
            locationManager.removeUpdates(this)
        }
    }

    override fun onLocationChanged(location: Location) {
        root?.apply {
            @SuppressLint("SetTextI18n")
            lblGpsStatus.text = "${location.latitude}, ${location.longitude}"
            mapView.apply {
                val url = getMapUrl(location)
                if (lastMapUrl == url) return
                load(url) {
                    diskCachePolicy(CachePolicy.DISABLED)
                    lifecycle(getViewLifecycleOwner())
                    crossfade(false)
                    listener(
                        onSuccess = { _, _ -> lastMapUrl = url },
                        onError = { _, _ -> Log.e(TAG, "Failed to load $url") })
                }
            }
        }
    }

    companion object {

        private const val TAG = "MapCard"

        // We need at least one
        private fun hasLocationPermissions(context: Context) =
            gpsPermissions.any { context.hasPermission(it) }

        // todo: copypasted from Explorer app
        private fun getMapUrl(location: Location): String {
            return String.format(
                Locale.getDefault(),
                "https://static-maps.yandex.ru/1.x/?lang=en_US" +
                        "&size=640,360&z=15&l=map&pt=" +
                        "%f,%f" +
                        ",pm2rdl",
                location.longitude, location.latitude
            )
        }

        @JvmStatic
        fun newInstance(): BaseFragment = MapCard()
    }
}
