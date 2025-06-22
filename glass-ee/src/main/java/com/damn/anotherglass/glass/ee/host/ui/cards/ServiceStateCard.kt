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
import com.damn.anotherglass.glass.ee.host.core.BatteryStatus
import com.damn.anotherglass.glass.ee.host.core.IService
import com.damn.anotherglass.glass.ee.host.ui.MainActivity

class ServiceStateCard : BaseFragment() {

    // todo:
    //  - voice commands (only for some time interval, e.g. 5 seconds, to conserve battery)
    //  - connect to current gateway IP/scan IP barcode/last IPs menu

    private var statusLabel: TextView? = null
    // todo: replace with tile (text is too small)
    private var batteryLabel: TextView? = null

    private val batteryStatus: BatteryStatus by lazy { BatteryStatus(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.main_layout, container, false)
        statusLabel = TextView(context).apply {
            textSize = BODY_TEXT_SIZE.toFloat()
            typeface = Typeface.create(getString(R.string.thin_font), Typeface.NORMAL)
            val bodyLayout = view.findViewById<FrameLayout>(R.id.body_layout)
            bodyLayout.addView(this)
            val state = mainActivity().getServiceState()
            setText(serviceState(state.value))
            state.observe(viewLifecycleOwner) {
                setText(serviceState(it))
            }
        }
        batteryLabel = view.findViewById(R.id.footer)
        return view
    }

    override fun onSingleTapUp() {
        super.onSingleTapUp()
        mainActivity().let {
            if (null == it.getServiceState().value) {
                it.tryStartService()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        batteryStatus.observe(viewLifecycleOwner) {
            // todo: use drawables for battery icon
            val icon = "\uD83D\uDD0B" // glass does not support low battery icon
            batteryLabel?.text = "$icon ${it.level}%${if (it.isCharging) "⚡" else ""}"
        }
    }

    override fun onTapAndHold() {
        super.onTapAndHold()
        mainActivity().stopService()
    }

    private fun mainActivity() = requireActivity() as MainActivity

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
