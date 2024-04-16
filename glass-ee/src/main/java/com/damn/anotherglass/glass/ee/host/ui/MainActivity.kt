package com.damn.anotherglass.glass.ee.host.ui

import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.HostService
import com.damn.anotherglass.glass.ee.host.gpsPermissions
import com.damn.anotherglass.glass.ee.host.ui.cards.BaseFragment
import com.damn.anotherglass.glass.ee.host.ui.cards.MapCard
import com.damn.anotherglass.glass.ee.host.utility.hasPermission
import com.example.glass.ui.GlassGestureDetector
import com.google.android.material.tabs.TabLayout

/**
 * Main activity of the application. It provides viewPager to move between fragments.
 */
class MainActivity : BaseActivity() {

    private val fragments: MutableList<BaseFragment> = ArrayList()
    private lateinit var viewPager: ViewPager

    private lateinit var client: HostService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_pager_layout)
        viewPager = findViewById(R.id.viewPager)

        fragments.add(MapCard.newInstance())
        viewPager.setAdapter(ScreenSlidePagerAdapter(supportFragmentManager))

        val tabLayout = findViewById<TabLayout>(R.id.page_indicator)
        tabLayout.setupWithViewPager(viewPager, true)

        // todo: must be a service
        client = HostService(this)

        tryStartService()
    }

    private fun tryStartService() {
        // todo: check if we have wifi connection, and it looks like tethering one
        if (!checkLocationPermission()) return
        client.start()
    }

    private fun checkLocationPermission(): Boolean {
        val missing = gpsPermissions.filter { !hasPermission(it) }
        return when {
            missing.isEmpty() -> true
            else -> {
                requestPermissions(missing.toTypedArray(), PERMISSIONS_REQUEST_LOCATION)
                false
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    client.start()
                } else {
                    Log.e(TAG, "Location permission denied")
                    // todo: show missing permission message
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.stop()
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean =
        when (gesture) {
            GlassGestureDetector.Gesture.TAP -> {
                fragments[viewPager.currentItem].onSingleTapUp()
                true
            }

            else -> super.onGesture(gesture)
        }

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment = fragments[position]

        override fun getCount(): Int = fragments.size
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSIONS_REQUEST_LOCATION = 1

    }
}
