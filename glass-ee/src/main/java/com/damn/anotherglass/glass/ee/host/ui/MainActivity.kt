package com.damn.anotherglass.glass.ee.host.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.viewpager.widget.ViewPager
import com.damn.anotherglass.glass.ee.host.BuildConfig
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.HostService
import com.damn.anotherglass.glass.ee.host.core.IService
import com.damn.anotherglass.glass.ee.host.core.Settings
import com.damn.anotherglass.glass.ee.host.core.tiltawake.TiltToWakeService
import com.damn.anotherglass.glass.ee.host.debug.DebugManager
import com.damn.anotherglass.glass.ee.host.ui.MainActivityEx.addNotificationsModule
import com.damn.anotherglass.glass.ee.host.ui.cards.BaseFragment
import com.damn.anotherglass.glass.ee.host.ui.cards.MapCard
import com.damn.anotherglass.glass.ee.host.ui.cards.ServiceStateCard
import com.damn.anotherglass.glass.ee.host.ui.cards.TiltToWakeCard
import com.damn.anotherglass.glass.ee.host.utility.isRunning
import com.example.glass.ui.GlassGestureDetector
import com.google.android.material.tabs.TabLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


// TODO:
//  - add option to connect by barcode
//  - add Bluetooth connection support (and WiFi for xe)?
//  - add zoom levels to map card
//  - add controls cards: slider, Gyro lists
//  - migrate to ViewPagers2 (with current ViewPagers2 version will need a few hacks
//      for current fragment gesture routing and some other issues)

class MainActivity : BaseActivity() {

    private val connection = GlassServiceConnection()
    private val fragments: MutableList<BaseFragment> = ArrayList()
    private lateinit var viewPager: ViewPager

    // todo: observe service actual state
    private val serviceState = MutableLiveData<IService.ServiceState?>()

    private val timeLine = object : ITimeline {
        override fun addFragment(fragment: BaseFragment, priority: Int, scrollTo: Boolean) {
            // todo: implement priority
            fragments.add(0, fragment)
            viewPager.adapter?.notifyDataSetChanged()
            if(scrollTo) viewPager.setCurrentItem(0, false)
        }

        override fun removeFragment(tag: String) {
            if(fragments.removeIf { it.tag == tag }) {
                viewPager.adapter?.notifyDataSetChanged()
            }
        }

        override fun <T : BaseFragment> removeByType(cls: Class<T>) {
            if(fragments.removeIf { it.javaClass == cls }) {
                viewPager.adapter?.notifyDataSetChanged()
            }
        }

        override fun <T : BaseFragment> indexOfFirst(java: Class<T>): Int =
            fragments.indexOfFirst { it.javaClass == java }

        override fun setCurrent(index: Int, smoothScroll: Boolean) =
            viewPager.setCurrentItem(index, smoothScroll)
    }

    private val debugManager: DebugManager by lazy { DebugManager() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_pager_layout)
        fragments.add(TiltToWakeCard.newInstance()) // -1 fragment (settings)
        fragments.add(ServiceStateCard.newInstance()) // default fragment
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

        addNotificationsModule(timeLine)

        timeLine.setCurrent(1, false)

        if(Settings(this).tiltToWake)
            TiltToWakeService.startService(this)

        tryStartService()
    }

    override fun onResume() {
        super.onResume()
        // do not bind if service is not running to avoid starting it
        if(isRunning<HostService>())
            connection.bindGlassService()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        connection.unbindGlassService()
        EventBus.getDefault().unregister(this)
    }

    fun tryStartService(ip: String? = null) {

        HostService.startService(this, ip)
        connection.bindGlassService()
    }

    fun stopService() {
        stopService(Intent(this, HostService::class.java))
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean =
        when (gesture) {
            GlassGestureDetector.Gesture.TAP -> {
                fragments[viewPager.currentItem].onSingleTapUp()
                true
            }
            GlassGestureDetector.Gesture.TAP_AND_HOLD -> {
                fragments[viewPager.currentItem].onTapAndHold()
                true
            }
            else -> super.onGesture(gesture)
        }

    fun getServiceState(): LiveData<IService.ServiceState?> = serviceState

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if(BuildConfig.DEBUG) {
            when(keyCode) {
                KeyEvent.KEYCODE_N -> {
                    if (event.isShiftPressed)
                        debugManager.removeNotification()
                    else
                        debugManager.postNotification()
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onServiceState(state: IService.ServiceState) {
        serviceState.postValue(state)
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
            serviceState.postValue(null)
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
