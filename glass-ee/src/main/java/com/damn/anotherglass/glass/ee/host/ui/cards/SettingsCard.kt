package com.damn.anotherglass.glass.ee.host.ui.cards

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.Settings
import com.damn.anotherglass.glass.ee.host.ui.settings.SettingsActivity

class SettingsCard : BaseFragment() {

    private lateinit var settings: Settings
    private var previewTextView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        settings = Settings(requireContext())
        val view = inflater.inflate(R.layout.layout_card_settings, container, false)
        previewTextView = view.findViewById(R.id.settings_preview)
        updatePreview()
        return view
    }

    override fun onResume() {
        super.onResume()
        updatePreview()
    }

    private fun updatePreview() {
        if (!::settings.isInitialized) return
        val enabled = settings.tiltToWake
        val threshold = settings.tiltAngleThreshold
        val statusText = if (enabled) {
            getString(R.string.tilt_preview_enabled_format, threshold)
        } else {
            getString(R.string.tilt_preview_disabled_label)
        }
        previewTextView?.text = statusText
    }

    override fun onSingleTapUp() {
        val context = context ?: return
        val intent = Intent(context, SettingsActivity::class.java)
        startActivity(intent)
    }

    companion object {
        @JvmStatic
        fun newInstance(): BaseFragment = SettingsCard()
    }
}
