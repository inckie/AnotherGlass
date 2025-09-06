package com.damn.anotherglass.glass.host.notifications

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.glass.shared.notifications.NotificationController
import com.damn.glass.shared.notifications.NotificationId
import com.google.android.glass.media.Sounds
import com.google.android.glass.widget.CardScrollAdapter
import com.google.android.glass.widget.CardScrollView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationsActivity : Activity() {
    private lateinit var mCardScroller: CardScrollView

    private var mNotifications: List<NotificationData> = listOf()

    // we cant use latest lifecycle due to API levels, so use coroutines directly
    private val customScope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        mNotifications = NotificationController.instance
            .getDismissibleNotifications()
            .sortedByDescending { it.postedTime }

        mCardScroller = CardScrollView(this)
        if (mNotifications.isEmpty()) {
            // allow to create mCardScroller for empty list, to avoid checks later
            finish()
            return
        }

        customScope.launch{
            NotificationController.instance.getNotifications().collect {
                mNotifications = it.filter { !it.isOngoing }.sortedByDescending { it.postedTime }
                if(mNotifications.isEmpty())
                    finish()
                else
                    mCardScroller.adapter?.notifyDataSetChanged()
            }
        }

        mCardScroller.adapter = object : CardScrollAdapter() {
            override fun getCount(): Int = mNotifications.size

            override fun getItem(position: Int): Any? = mNotifications[position]

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? =
                buildView(mNotifications[position])

            override fun getPosition(item: Any?): Int {
                // if not found returns -1, save value as AdapterView.INVALID_POSITION
                if (item is NotificationData) return mNotifications.indexOf(item)
                return AdapterView.INVALID_POSITION
            }
        }

        // todo: handle controls: allow to dismiss single/all
        mCardScroller.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                val notification = mNotifications[position]
                NotificationController.instance.dismissNotification(NotificationId(notification))
                val am = getSystemService(AUDIO_SERVICE) as AudioManager?
                am?.playSoundEffect(Sounds.DISMISSED)
            }
        setContentView(mCardScroller)
    }

    override fun onResume() {
        super.onResume()
        mCardScroller.activate()
    }

    override fun onPause() {
        mCardScroller.deactivate()
        super.onPause()
    }

    override fun onDestroy() {
        customScope.cancel()
        super.onDestroy()
    }

    private fun buildView(notification: NotificationData): View? =
        NotificationViewBuilder.buildView(this, notification).view
}

fun NotificationController.getDismissibleNotifications(): List<NotificationData> =
    this.getNotifications().value.filter { !it.isOngoing }
