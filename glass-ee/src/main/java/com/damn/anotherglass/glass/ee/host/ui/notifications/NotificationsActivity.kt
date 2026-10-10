package com.damn.anotherglass.glass.ee.host.ui.notifications

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding
import com.damn.anotherglass.glass.ee.host.databinding.ViewPager2LayoutBinding
import com.damn.anotherglass.glass.ee.host.ui.BaseActivity
import com.damn.anotherglass.glass.ee.host.ui.extensions.bindData
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.glass.shared.notifications.NotificationController
import com.damn.glass.shared.notifications.NotificationId
import com.example.glass.ui.GlassGestureDetector
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NotificationsActivity : BaseActivity() {

    // todo:
    //  - voice commands
    //  - awake and present on new notifications (optional)

    private val adapter = NotificationsPagerAdapter()
    private var onTapListener: (() -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ViewPager2LayoutBinding.inflate(layoutInflater).apply {
            setContentView(root)
            root.background = ColorDrawable(Color.BLACK)
            viewPager.adapter = adapter
            TabLayoutMediator(pageIndicator, viewPager) { _, _ -> }.attach()
        }

        onTapListener = {
            adapter.currentList.getOrNull(binding.viewPager.currentItem)?.let { notification ->
                NotificationController.instance.dismissNotification(NotificationId(notification))
                true
            } ?: false
        }

        NotificationController.instance.notifications
            .onEach { list ->
                if (list.isEmpty()) {
                    finish()
                } else {
                    adapter.submitList(list)
                }
            }
            .launchIn(lifecycleScope)
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean = when (gesture) {
        GlassGestureDetector.Gesture.TAP -> onTapListener?.invoke() ?: false
        else -> super.onGesture(gesture)
    }

    class NotificationsPagerAdapter : RecyclerView.Adapter<NotificationsPagerAdapter.NotificationViewHolder>() {

        var currentList: List<NotificationData> = emptyList()
            private set

        fun submitList(list: List<NotificationData>) {
            currentList = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            NotificationViewHolder(
                LayoutNotificationsStackBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            holder.binding.bindData(currentList[position])
        }

        override fun getItemCount(): Int = currentList.size

        class NotificationViewHolder(val binding: LayoutNotificationsStackBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}