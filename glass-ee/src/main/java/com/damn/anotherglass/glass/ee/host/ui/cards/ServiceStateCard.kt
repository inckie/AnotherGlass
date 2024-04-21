package com.damn.anotherglass.glass.ee.host.ui.cards

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.damn.anotherglass.glass.ee.host.R
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
                    // todo: map status enum to text
                    text = state?.toString() ?: "Service is not running. Tap to start."
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

    private fun mainActivity() = (context as? MainActivity)

    companion object {
        private const val BODY_TEXT_SIZE = 40
        @JvmStatic
        fun newInstance(): ServiceStateCard = ServiceStateCard()
    }
}
