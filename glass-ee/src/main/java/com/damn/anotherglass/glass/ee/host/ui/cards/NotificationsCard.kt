package com.damn.anotherglass.glass.ee.host.ui.cards

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.damn.anotherglass.glass.ee.host.core.NotificationController
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding
import com.damn.anotherglass.shared.notifications.NotificationData

class NotificationsCard : BaseFragment() {

    // todo:
    //  - if more that one single shot notification present, open notification stack activity on tap
    //  - if only one notification present, open dismiss activity on tap
    //  - handle single shot/ongoing notifications separately

    private var binding: LayoutNotificationsStackBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = LayoutNotificationsStackBinding.inflate(inflater).apply {
        binding = this
    }.root

    override fun onAttach(context: Context) {
        super.onAttach(context)
        NotificationController.instance.getNotifications().observe(this) {
            if (it.isEmpty()) return@observe  // we are about to be removed
            binding?.apply {
                it.last()
                bindData(it.last(), context)
                // todo: add another place for counter
                if (it.size > 1) footer.text = "" + it.size
            }
        }
    }

    private fun LayoutNotificationsStackBinding.bindData(
        last: NotificationData,
        context: Context
    ) {
        title.text = last.title
        text.text = last.text
        footer.text = last.tickerText
        timestamp.text = DateUtils.formatDateTime(
            context,
            last.postedTime,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
        )
        if (last.icon == null) {
            imgIcon.setImageResource(android.R.drawable.ic_dialog_info)
        } else {
            try {
                val bitmap = BitmapFactory.decodeByteArray(last.icon, 0, last.icon.size)
                imgIcon.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode icon: " + e.message, e)
                imgIcon.setImageResource(android.R.drawable.ic_dialog_info)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val TAG = "NotificationsCard"

        @JvmStatic
        fun newInstance(): NotificationsCard = NotificationsCard()
    }
}