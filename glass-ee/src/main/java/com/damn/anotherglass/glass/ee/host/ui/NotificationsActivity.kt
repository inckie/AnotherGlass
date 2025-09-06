package com.damn.anotherglass.glass.ee.host.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.damn.glass.shared.notifications.NotificationController
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding
import com.damn.anotherglass.glass.ee.host.databinding.ViewPager2LayoutBinding
import com.damn.anotherglass.glass.ee.host.ui.extensions.LayoutNotificationsStackBindingEx.bindData
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.glass.shared.notifications.NotificationId
import com.example.glass.ui.GlassGestureDetector
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NotificationsActivity : BaseActivity() {

    // todo:
    //  - voice commands
    //  - awake and present on new notifications (optional)

    // some glue to redirect gesture to the adapter subclass
    // for now we just dismiss the notification on tap
    private lateinit var onTapListener: () -> Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewPager2LayoutBinding.inflate(layoutInflater).apply {
            setContentView(root)
            root.background = ColorDrawable(Color.BLACK)
            val adapter = NotificationsPagerAdapter(
                NotificationController.instance.getNotifications(),
                this@NotificationsActivity
            )
            viewPager.adapter = adapter
            TabLayoutMediator(pageIndicator, viewPager) { _, _ ->}.attach()
            onTapListener = {
                val notification = adapter.getData(viewPager.currentItem)
                NotificationController.instance.dismissNotification(NotificationId(notification))
                true
            }
        }
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture) = when (gesture) {
        GlassGestureDetector.Gesture.TAP -> onTapListener()
        else -> super.onGesture(gesture)
    }

    class NotificationsPagerAdapter(
        notificationsFlow: StateFlow<List<NotificationData>>,
        activity: NotificationsActivity
    ) : RecyclerView.Adapter<NotificationsPagerAdapter.NotificationViewHolder>() {

        private var notifications: List<NotificationData> = notificationsFlow.value

        init {
            notificationsFlow
                .onEach {
                    if (it.isEmpty()) {
                        activity.finish() // close activity if no notifications
                    } else {
                        notifications = it
                        notifyDataSetChanged()
                    }
                }
                .launchIn(activity.lifecycleScope)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            NotificationViewHolder(LayoutNotificationsStackBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ))

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = notifications[position]
            holder.binding.bindData(notification, holder.itemView.context)
        }

        override fun getItemCount(): Int = notifications.size

        fun getData(currentItem: Int) = notifications[currentItem]

        class NotificationViewHolder(val binding: LayoutNotificationsStackBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}
