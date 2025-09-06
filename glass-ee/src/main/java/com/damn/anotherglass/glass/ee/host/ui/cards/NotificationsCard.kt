package com.damn.anotherglass.glass.ee.host.ui.cards

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.damn.glass.shared.notifications.NotificationController
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding
import com.damn.anotherglass.glass.ee.host.ui.NotificationsActivity
import com.damn.anotherglass.glass.ee.host.ui.extensions.LayoutNotificationsStackBindingEx.bindData
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NotificationController.instance.getNotifications()
            .onEach {
                if (it.isEmpty()) return@onEach  // we are about to be removed
                binding?.apply {
                    bindData(it.last(), requireContext())
                    // todo: add another place for counter
                    if (it.size > 1) footer.text = "" + it.size
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
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