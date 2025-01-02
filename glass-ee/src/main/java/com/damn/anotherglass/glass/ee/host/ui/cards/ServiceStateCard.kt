package com.damn.anotherglass.glass.ee.host.ui.cards

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.IService
import com.damn.anotherglass.glass.ee.host.ui.MainActivity

class ServiceStateCard : BaseFragment() {

    private var statusLabel: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.main_layout, container, false)
        statusLabel = TextView(context).apply {
            textSize = BODY_TEXT_SIZE.toFloat()
            setTypeface(Typeface.create(getString(R.string.thin_font), Typeface.NORMAL))
            val bodyLayout = view.findViewById<FrameLayout>(R.id.body_layout)
            bodyLayout.addView(this)
            mainActivity()?.let {
                it.getServiceState().observe(viewLifecycleOwner) { state ->
                    text = getString(serviceState(state))
                }
            }
        }
        return view
    }

    override fun onSingleTapUp() {
        super.onSingleTapUp()
        mainActivity()?.let {
            if(null == it.getServiceState().value) {
                it.tryStartService()
            }
        }
    }

    override fun onTapAndHold() {
        super.onTapAndHold()
        mainActivity()?.stopService()
    }

    private fun mainActivity() = (context as? MainActivity)

    companion object {
        private const val BODY_TEXT_SIZE = 40
        @JvmStatic
        fun newInstance(): ServiceStateCard = ServiceStateCard()

        @StringRes
        private fun serviceState(state: IService.ServiceState?): Int = when (state) {
            null -> R.string.msg_service_not_running
            IService.ServiceState.INITIALIZING -> R.string.msg_service_initializing
            IService.ServiceState.WAITING -> R.string.msg_service_waiting
            IService.ServiceState.CONNECTED -> R.string.msg_service_connected
            IService.ServiceState.DISCONNECTED -> R.string.msg_service_disconnected
        }
    }
}
