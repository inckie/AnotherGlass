package com.damn.anotherglass.glass.ee.host.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.Settings
import com.damn.anotherglass.glass.ee.host.ui.BaseActivity
import com.example.glass.ui.GlassGestureDetector
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class SettingsActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: SettingsPagerAdapter
    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_pager2_layout)

        settings = Settings(this)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.page_indicator)

        adapter = SettingsPagerAdapter(
            getPages(),
            onPageClick = { position -> handlePageTap(position) }
        )
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
    }

    override fun onResume() {
        super.onResume()
        // Refresh items in case settings were modified in child activities
        adapter.updatePages(getPages())
    }

    private fun getPages(): List<SettingsPageItem> {
        val tiltEnabled = settings.tiltToWake
        val threshold = settings.tiltAngleThreshold
        val tiltStatus = if (tiltEnabled) {
            getString(R.string.tilt_status_enabled_format, threshold)
        } else {
            getString(R.string.tilt_status_disabled_label)
        }

        val wakeOnNotif = settings.wakeOnNotification
        val notifStatus = if (wakeOnNotif) {
            getString(R.string.settings_wake_on_notification_enabled)
        } else {
            getString(R.string.settings_wake_on_notification_disabled)
        }
        val notifHint = if (wakeOnNotif) {
            getString(R.string.settings_tap_to_turn_off)
        } else {
            getString(R.string.settings_tap_to_turn_on)
        }

        return listOf(
            SettingsPageItem(
                id = PAGE_TILT_TO_WAKE,
                icon = R.drawable.ic_tilt_to_wake,
                title = getString(R.string.tilt_title),
                status = tiltStatus,
                actionHint = getString(R.string.settings_tap_to_configure)
            ),
            SettingsPageItem(
                id = PAGE_WAKE_ON_NOTIFICATION,
                icon = R.drawable.ic_notifications,
                title = getString(R.string.settings_wake_on_notification_title),
                status = notifStatus,
                actionHint = notifHint
            ),
            SettingsPageItem(
                id = PAGE_ABOUT,
                icon = R.drawable.ic_info,
                title = getString(R.string.settings_about_title),
                status = getString(R.string.settings_about_subtitle),
                actionHint = getString(R.string.settings_about_hint)
            )
        )
    }

    private fun handlePageTap(position: Int) {
        val page = adapter.getPageAt(position) ?: return
        when (page.id) {
            PAGE_TILT_TO_WAKE -> {
                val intent = Intent(this, TiltToWakeSettingsActivity::class.java)
                startActivity(intent)
            }
            PAGE_WAKE_ON_NOTIFICATION -> {
                settings.wakeOnNotification = !settings.wakeOnNotification
                adapter.updatePages(getPages())
            }
            PAGE_ABOUT -> {
                // Info only, no sub-activity
            }
        }
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean =
        when (gesture) {
            GlassGestureDetector.Gesture.SWIPE_FORWARD -> {
                if (viewPager.currentItem < adapter.itemCount - 1) {
                    viewPager.setCurrentItem(viewPager.currentItem + 1, true)
                }
                true
            }
            GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
                if (viewPager.currentItem > 0) {
                    viewPager.setCurrentItem(viewPager.currentItem - 1, true)
                }
                true
            }
            GlassGestureDetector.Gesture.TAP -> {
                handlePageTap(viewPager.currentItem)
                true
            }
            else -> super.onGesture(gesture)
        }

    data class SettingsPageItem(
        val id: Int,
        @param:DrawableRes val icon: Int,
        val title: String,
        val status: String,
        val actionHint: String
    )

    private class SettingsPagerAdapter(
        private var items: List<SettingsPageItem>,
        private val onPageClick: (Int) -> Unit
    ) : RecyclerView.Adapter<SettingsPagerAdapter.ViewHolder>() {

        fun updatePages(newItems: List<SettingsPageItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        fun getPageAt(position: Int): SettingsPageItem? =
            items.getOrNull(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_settings_page_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.icon.setImageResource(item.icon)
            holder.title.text = item.title
            holder.status.text = item.status
            holder.actionHint.text = item.actionHint
            holder.itemView.setOnClickListener { onPageClick(position) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.item_icon)
            val title: TextView = itemView.findViewById(R.id.item_title)
            val status: TextView = itemView.findViewById(R.id.item_status)
            val actionHint: TextView = itemView.findViewById(R.id.item_action_hint)
        }
    }

    companion object {
        private const val PAGE_TILT_TO_WAKE = 1
        private const val PAGE_WAKE_ON_NOTIFICATION = 2
        private const val PAGE_ABOUT = 3
    }
}
