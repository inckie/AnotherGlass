package com.damn.anotherglass.glass.ee.host.ui.cards

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding
import com.damn.anotherglass.glass.ee.host.ui.extensions.bindData
import com.damn.anotherglass.glass.ee.host.ui.notifications.NotificationsActivity
import com.damn.glass.shared.notifications.NotificationController
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
    ): View = LayoutNotificationsStackBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NotificationController.instance.notifications
            .onEach { list ->
                val latest = list.lastOrNull() ?: return@onEach
                binding?.apply {
                    bindData(latest)
                    // todo: add another place for counter
                    if (list.size > 1) footer.text = list.size.toString()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onSingleTapUp() {
        startActivity(Intent(requireActivity(), NotificationsActivity::class.java))
    }

    companion object {
        @JvmStatic
        fun newInstance(): NotificationsCard = NotificationsCard()
    }
}