package com.damn.anotherglass.glass.ee.host.ui.cards

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.damn.anotherglass.glass.ee.host.databinding.LayoutCardMapBinding
import com.damn.anotherglass.glass.ee.host.ui.MainActivity
import com.damn.anotherglass.glass.ee.host.utility.hasPermission
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import java.util.Locale

class MapCard : BaseFragment(), LocationListener {
    private var root: LayoutCardMapBinding? = null
    private var lastMapUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = LayoutCardMapBinding.inflate(inflater, container, false)
        return root!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        root = null
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun onResume() {
        super.onResume()

        val context = requireContext()

        if (MainActivity.gpsPermissions.any { !context.hasPermission(it) }) {
            root?.lblGpsStatus?.text = "GPS permissions not granted"
            return
        }

        val locationManager = context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

        if(!locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            root?.lblGpsStatus?.text = "GPS provider not available" // usually means MockGPS is not available
            return
        }

        root?.lblGpsStatus?.text = "Waiting for GPS signal..."

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
    }

    override fun onPause() {
        super.onPause()
        val context = requireContext()
        // no permission check required to remove listener
        val locationManager = context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(this)
        root?.mapView?.apply {
            Picasso.get().cancelRequest(this)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onLocationChanged(location: Location) {
        root?.apply {
            lblGpsStatus.text = "${location.latitude}, ${location.longitude}"
            mapView.apply {
                val url = getMapUrl(location)
                if (lastMapUrl == url) return
                lastMapUrl = url
                Picasso.get()
                    .load(url)
                    .noPlaceholder()
                    .memoryPolicy(MemoryPolicy.NO_STORE)
                    .into(this)
            }
        }
    }

    companion object{
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
