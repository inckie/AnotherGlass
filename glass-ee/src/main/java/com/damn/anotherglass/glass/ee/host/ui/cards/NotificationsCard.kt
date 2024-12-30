package com.damn.anotherglass.glass.ee.host.ui.cards

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.damn.anotherglass.glass.ee.host.core.NotificationController
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding
import com.damn.anotherglass.glass.ee.host.ui.NotificationsActivity
import com.damn.anotherglass.glass.ee.host.ui.extensions.LayoutNotificationsStackBindingEx.bindData

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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onSingleTapUp() {
        requireActivity().startActivity(Intent(requireActivity(), NotificationsActivity::class.java))
    }

    companion object {
        private const val TAG = "NotificationsCard"

        @JvmStatic
        fun newInstance(): NotificationsCard = NotificationsCard()
    }
}