package com.damn.anotherglass.glass.ee.host.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.HostService
import com.damn.anotherglass.glass.ee.host.core.IService
import com.damn.anotherglass.glass.ee.host.gpsPermissions
import com.damn.anotherglass.glass.ee.host.ui.cards.BaseFragment
import com.damn.anotherglass.glass.ee.host.ui.cards.MapCard
import com.damn.anotherglass.glass.ee.host.ui.cards.TextLayoutFragment
import com.damn.anotherglass.glass.ee.host.utility.hasPermission
import com.damn.anotherglass.glass.ee.host.utility.isRunning
import com.example.glass.ui.GlassGestureDetector
import com.google.android.material.tabs.TabLayout


// TODO:
//  - add connect/disconnect action on tap
//  - add GPS permissions/enable mock/enable/disable menu on top of MapCard
//  (remove location permissions requirement for service start)
//  - add option to connect by barcode
//  - add Bluetooth connection support (and WiFi for xe)?
//  - add zoom levels to map card
//  - add controls cards: slider, Gyro lists
//  - add notifications card
//  - extract string constants

class MainActivity : BaseActivity() {

    private val connection = GlassServiceConnection()

    private val fragments: MutableList<BaseFragment> = ArrayList()
    private lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_pager_layout)
        fragments.add(TextLayoutFragment.newInstance("Initializing…", "", "", null))

        viewPager = findViewById(R.id.viewPager)
        viewPager.isSaveFromParentEnabled = false
        viewPager.setAdapter(object : FragmentStatePagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment = fragments[position]
            override fun getCount(): Int = fragments.size
            override fun getItemPosition(o: Any): Int = POSITION_NONE // TODO: hack
        })

        val tabLayout = findViewById<TabLayout>(R.id.page_indicator)
        tabLayout.setupWithViewPager(viewPager, true)

        tryStartService()
    }

    override fun onResume() {
        super.onResume()
        // do not bind if service is not running to avoid starting it
        if(isRunning<HostService>())
            connection.bindGlassService()
    }

    override fun onPause() {
        super.onPause()
        connection.unbindGlassService()
    }

    private fun tryStartService() {
        // todo: check if we have wifi connection, and it looks like tethering one
        // todo: pass address to service
        if (!checkLocationPermission()) return
        startService(Intent(this, HostService::class.java))
        connection.bindGlassService()
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
                    tryStartService()
                } else {
                    Log.e(TAG, "Location permission denied")
                    // todo: show missing permission message
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean =
        when (gesture) {
            GlassGestureDetector.Gesture.TAP -> {
                fragments[viewPager.currentItem].onSingleTapUp()
                true
            }

            GlassGestureDetector.Gesture.SWIPE_UP, // for emulator
            GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_DOWN -> {
                stopService(Intent(this, HostService::class.java))
                true
            }

            else -> super.onGesture(gesture)
        }

    private inner class GlassServiceConnection : ServiceConnection {
        var service: IService? = null
            private set
        private var bound = false

        fun bindGlassService() {
            try {
                if (bound) {
                    unbindService(connection)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bound = bindService(Intent(this@MainActivity, HostService::class.java), connection, 0)
        }

        fun unbindGlassService() {
            if (!bound) return
            bound = false
            service = null
            unbindService(connection)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            this.service = (service as HostService.LocalBinder).getService()
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            unbindGlassService()
            updateUI()
        }
    }

    private fun updateUI() = when (connection.service) {
        null -> serviceNotRunningState()
        else -> serviceRunningState()
    }

    private fun serviceRunningState() {
        // todo: I can't actually just replace the fragments directly, but I hack it for now
        fragments.clear()
        fragments.add(
            TextLayoutFragment.newInstance(
                "Connected to the service. Swipe down with two fingers to stop the service", "", ""
            )
        )
        fragments.add(MapCard.newInstance())
        viewPager.adapter?.notifyDataSetChanged()
    }

    private fun serviceNotRunningState() {
        fragments.clear()
        fragments.add(TextLayoutFragment.newInstance("Service was shut down", "", ""))
        viewPager.adapter?.notifyDataSetChanged()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSIONS_REQUEST_LOCATION = 1
    }
}
