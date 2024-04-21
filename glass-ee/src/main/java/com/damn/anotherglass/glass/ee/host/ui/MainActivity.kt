package com.damn.anotherglass.glass.ee.host.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.viewpager.widget.ViewPager
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.HostService
import com.damn.anotherglass.glass.ee.host.core.IService
import com.damn.anotherglass.glass.ee.host.ui.cards.BaseFragment
import com.damn.anotherglass.glass.ee.host.ui.cards.MapCard
import com.damn.anotherglass.glass.ee.host.ui.cards.ServiceStateCard
import com.damn.anotherglass.glass.ee.host.utility.isRunning
import com.example.glass.ui.GlassGestureDetector
import com.google.android.material.tabs.TabLayout


// TODO:
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

    // todo: observe service actual state
    private val serviceState = MutableLiveData<IService.ServiceState?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_pager_layout)
        fragments.add(ServiceStateCard.newInstance())
        fragments.add(MapCard.newInstance())

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

    fun tryStartService() {
        // todo: check if we have wifi connection, and it looks like tethering one
        // todo: pass address to service
        startService(Intent(this, HostService::class.java))
        connection.bindGlassService()
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

    fun getServiceState(): LiveData<IService.ServiceState?> = serviceState

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
            val s = (service as HostService.LocalBinder).getService()
            this.service = s
            serviceState.postValue(s.state)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceState.postValue(null)
            service = null
            unbindGlassService()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
