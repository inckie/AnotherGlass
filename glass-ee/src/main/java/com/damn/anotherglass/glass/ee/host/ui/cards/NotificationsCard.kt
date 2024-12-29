package com.damn.anotherglass.glass.ee.host.ui.cards

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.damn.anotherglass.glass.ee.host.core.NotificationController
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding

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
                val last = it.last()
                title.text = last.title
                text.text = last.text
                footer.text = if (it.size > 1) "" + it.size else last.tickerText
                timestamp.text = DateUtils.formatDateTime(context, last.postedTime, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL)
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