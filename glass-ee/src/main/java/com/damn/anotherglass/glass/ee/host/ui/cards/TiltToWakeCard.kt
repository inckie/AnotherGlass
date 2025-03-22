package com.damn.anotherglass.glass.ee.host.ui.cards

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.Settings
import com.damn.anotherglass.glass.ee.host.core.tiltawake.TiltToWakeService
import com.damn.anotherglass.glass.ee.host.ui.cards.TextLayoutFragment.Companion.BODY_TEXT_SIZE
import com.damn.anotherglass.glass.ee.host.utility.isRunning

class TiltToWakeCard : BaseFragment() {

    // TODO:
    //  - test screen
    //  - angle setting

    private lateinit var textView: TextView
    private lateinit var settings: Settings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        settings = Settings(requireContext())
        val view = inflater.inflate(R.layout.main_layout, container, false)
        val bodyLayout = view.findViewById<FrameLayout>(R.id.body_layout)
        textView = TextView(context).apply {
            text = serviceStatusLabel(context.isRunning<TiltToWakeService>())
            textSize = BODY_TEXT_SIZE.toFloat()
            setTypeface(Typeface.create(getString(R.string.thin_font), Typeface.NORMAL))
            bodyLayout.addView(this)
        }
        return view
    }

    private fun serviceStatusLabel(running: Boolean) =
        when {
            running -> getString(R.string.lbl_wake_service_tap_to_stop)
            else -> getString(R.string.wake_service_tap_to_start)
        }

    // We do not properly update service status if it will be changed outside of this card
    override fun onTapAndHold() {
        super.onTapAndHold()
        val cxt = requireContext()
        val running = cxt.isRunning<TiltToWakeService>()

        settings.tiltToWake = !running

        if (running) TiltToWakeService.stopService(cxt)
        else TiltToWakeService.startService(cxt)

        // hope it will start/stop
        textView.text = serviceStatusLabel(!running)
    }

    companion object {
        @JvmStatic
        fun newInstance(): BaseFragment = TiltToWakeCard()
    }
}