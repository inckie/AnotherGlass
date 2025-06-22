package com.damn.anotherglass.glass.ee.host.ui.cards

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil3.load
import coil3.request.CachePolicy
import coil3.request.lifecycle
import coil3.request.transitionFactory
import coil3.transition.CrossfadeTransition
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.databinding.LayoutCardMapBinding
import com.damn.anotherglass.glass.ee.host.gpsPermissions
import com.damn.anotherglass.glass.ee.host.utility.hasPermission
import com.damn.anotherglass.glass.ee.host.utility.locationManager
import java.util.Locale


class MapCard : BaseFragment() {

    private lateinit var locationManager: LocationManager
    private var root: LayoutCardMapBinding? = null
    private var lastMapUrl: String? = null

    private val statusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {
                updateState()
            }
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = updateMap(location)
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
        @Deprecated("Deprecated in Java", ReplaceWith("Unit"))
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    // todo: write "integrated" location provider, that can also work on broadcasts
    // todo: add zoom in/out menu commands

    private enum class State {
        MissingGeoPermissions,
        MissingMockLocationPermissions,
        MockNotRunning,
        Active,
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationManager = context.locationManager()
    }

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
        val state = getState()
        @Suppress("DEPRECATION")
        when (state) {
            State.MissingGeoPermissions -> requestPermissions(gpsPermissions, PERMISSIONS_REQUEST_LOCATION)
            State.MissingMockLocationPermissions -> startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            State.MockNotRunning -> {} // todo: try to notify service to start mock (if service is running)
            State.Active -> {} // todo: show zoom in/out overlay
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) = when (requestCode) {
        PERMISSIONS_REQUEST_LOCATION -> updateState()
        else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        updateState()
        IntentFilter().let {
            it.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            requireContext().registerReceiver(statusReceiver, it)
        }
        val context = requireContext()
        lastMapUrl = null // we lost image, force reload
        @SuppressLint("MissingPermission")
        if (hasLocationPermissions(context)) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                updateMap(it)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val context = requireContext()
        if (hasLocationPermissions(context)) {
            locationManager.removeUpdates(locationListener)
        }
        context.unregisterReceiver(statusReceiver)
    }

    private fun updateState() {
        val state = getState()
        @SuppressLint("MissingPermission")
        root?.lblGpsStatus?.text = when (state) {
            State.MissingGeoPermissions -> getString(R.string.msg_gps_permissions_not_granted)
            State.MissingMockLocationPermissions -> getString(R.string.msg_gps_provider_not_available)
            State.MockNotRunning -> getString(R.string.msg_gps_mock_not_running)
            State.Active -> {
                // rate and minimal distance are managed by mobile app, so we can use 0, 0
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0f,
                    locationListener
                )
                getString(R.string.msg_waiting_for_gps_signal)
            }
        }
    }

    private fun updateMap(location: Location) {
        root?.apply {
            @SuppressLint("SetTextI18n")
            lblGpsStatus.text = "${location.latitude}, ${location.longitude}"
            mapView.apply {
                val url = getMapUrl(location)
                if (lastMapUrl == url) return
                load(url) {
                    diskCachePolicy(CachePolicy.DISABLED)
                    lifecycle(getViewLifecycleOwner())
                    transitionFactory(CrossfadeTransition.Factory())
                    listener(
                        onSuccess = { _, _ -> lastMapUrl = url },
                        onError = { _, _ -> Log.e(TAG, "Failed to load $url") })
                }
            }
        }
    }

    private fun getState(): State = requireContext().let {
        when {
            !hasLocationPermissions(it) -> State.MissingGeoPermissions
            // order is important. Maybe device has a real provider, which is good enough (we should check its active though…)
            locationManager.allProviders.contains(LocationManager.GPS_PROVIDER) -> State.Active
            !hasMockPermission(it) -> State.MissingMockLocationPermissions
            else -> State.MockNotRunning
        }
    }


    companion object {

        private const val TAG = "MapCard"
        private const val PERMISSIONS_REQUEST_LOCATION = 1

        // We need at least one
        private fun hasLocationPermissions(context: Context) =
            gpsPermissions.any { context.hasPermission(it) }

        private fun hasMockPermission(context: Context): Boolean {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_PERMISSIONS
                )
                packageInfo.requestedPermissions?.any { it == "android.permission.ACCESS_MOCK_LOCATION" }
                    ?: false
            } catch (e: Exception) {
                Log.e(TAG, "checkForAllowMockLocationsApps failed: " + e.message)
                // todo:try to install and remove mock provider to check if permission is granted
                false
            }
        }

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
