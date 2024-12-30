package com.damn.anotherglass.glass.ee.host.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.viewpager.widget.PagerAdapter
import com.damn.anotherglass.glass.ee.host.core.NotificationController
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding
import com.damn.anotherglass.glass.ee.host.databinding.ViewPagerLayoutBinding
import com.damn.anotherglass.glass.ee.host.ui.extensions.LayoutNotificationsStackBindingEx.bindData
import com.damn.anotherglass.shared.notifications.NotificationData

class NotificationsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewPagerLayoutBinding.inflate(layoutInflater).apply {
            setContentView(root)
            root.background = ColorDrawable(Color.BLACK)
            viewPager.adapter = NotificationsPagerAdapter(
                NotificationController.instance.getNotifications(),
                this@NotificationsActivity,
                layoutInflater
            )
            pageIndicator.setupWithViewPager(viewPager, true)
        }
    }

    class NotificationsPagerAdapter(
        notificationsLiveData: LiveData<List<NotificationData>>,
        notificationsActivity: NotificationsActivity,
        private val layoutInflater: LayoutInflater
    ) : PagerAdapter() {

        private var notifications: List<NotificationData> = notificationsLiveData.value!!
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        init {
            notificationsLiveData.observe(notificationsActivity) {
                if (it.isEmpty()) {
                    notificationsActivity.finish()  // if no notifications, finish activity to avoid unnecessary resource consumption
                } else {
                    notifications = it
                }
            }
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any =
            LayoutNotificationsStackBinding.inflate(layoutInflater).apply {
                container.addView(root)
                val data = notifications[position]
                bindData(data, layoutInflater.context)
            }.root

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) =
            container.removeView(obj as View)

        override fun getCount(): Int = notifications.size

        override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj
    }
}
