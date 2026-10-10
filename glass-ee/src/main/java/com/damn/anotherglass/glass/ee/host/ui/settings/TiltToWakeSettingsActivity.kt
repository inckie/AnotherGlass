package com.damn.anotherglass.glass.ee.host.ui.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.Settings
import com.damn.anotherglass.glass.ee.host.core.SoundController
import com.damn.anotherglass.glass.ee.host.core.TiltSensitivity
import com.damn.anotherglass.glass.ee.host.core.tiltawake.TiltDetector
import com.damn.anotherglass.glass.ee.host.core.tiltawake.TiltToWakeService
import com.damn.anotherglass.glass.ee.host.ui.BaseActivity
import com.example.glass.ui.GlassGestureDetector
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TiltToWakeSettingsActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var settings: Settings
    private val soundController by lazy { SoundController(this) }
    private val handler = Handler(Looper.getMainLooper())

    private var testDetector: TiltDetector? = null
    private var testViewHolder: TestViewHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_pager2_layout)

        settings = Settings(this)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.page_indicator)

        val adapter = TiltSettingsAdapter(
            getToggleState = { settings.tiltToWake },
            getSensitivity = { TiltSensitivity.fromAngle(settings.tiltAngleThreshold) },
            onToggleClick = { toggleTiltToWake() },
            onSensitivityClick = { cycleSensitivity() },
            onBindTest = { holder -> testViewHolder = holder }
        )
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == POSITION_TEST) {
                    startTestDetection()
                } else {
                    stopTestDetection()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (viewPager.currentItem == POSITION_TEST) {
            startTestDetection()
        }
    }

    override fun onPause() {
        super.onPause()
        stopTestDetection()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTestDetection()
        soundController.release()
    }

    private fun toggleTiltToWake() {
        val newState = !settings.tiltToWake
        settings.tiltToWake = newState
        if (newState) {
            TiltToWakeService.startService(this)
        } else {
            TiltToWakeService.stopService(this)
        }
        viewPager.adapter?.notifyItemChanged(POSITION_TOGGLE)
    }

    private fun cycleSensitivity() {
        val current = TiltSensitivity.fromAngle(settings.tiltAngleThreshold)
        val next = TiltSensitivity.next(current)
        settings.tiltAngleThreshold = next.thresholdDegrees
        testDetector?.angleThreshold = next.thresholdDegrees
        viewPager.adapter?.notifyItemChanged(POSITION_SENSITIVITY)
        viewPager.adapter?.notifyItemChanged(POSITION_TEST)
    }

    private fun startTestDetection() {
        if (testDetector == null) {
            testDetector = TiltDetector(this, settings.tiltAngleThreshold).apply {
                onTiltSample = { pitch, _, isTilted ->
                    handler.post {
                        val holder = testViewHolder ?: return@post
                        holder.currentAngle.text = "${pitch.toInt()}°"
                        if (isTilted && holder.triggerBanner.text != getString(R.string.tilt_test_triggered)) {
                            holder.triggerBanner.text = getString(R.string.tilt_test_holding)
                            holder.triggerBanner.setTextColor(0xFFFFEB3B.toInt()) // Solid bright yellow
                        } else if (!isTilted && holder.triggerBanner.text != getString(R.string.tilt_test_triggered)) {
                            holder.triggerBanner.text = getString(R.string.tilt_test_ready)
                            holder.triggerBanner.setTextColor(ContextCompat.getColor(this@TiltToWakeSettingsActivity, R.color.glass_subtitle_text))
                        }
                    }
                }
                onTiltTriggered = {
                    handler.post {
                        val holder = testViewHolder ?: return@post
                        holder.triggerBanner.text = getString(R.string.tilt_test_triggered)
                        holder.triggerBanner.setTextColor(0xFF00E676.toInt()) // Solid bright green
                        soundController.playSound(SoundController.SoundEffect.NotificationPosted)

                        handler.postDelayed({
                            holder.triggerBanner.text = getString(R.string.tilt_test_ready)
                            holder.triggerBanner.setTextColor(ContextCompat.getColor(this@TiltToWakeSettingsActivity, R.color.glass_subtitle_text))
                        }, 1500)
                    }
                }
            }
        } else {
            testDetector?.angleThreshold = settings.tiltAngleThreshold
        }
        testDetector?.start()
    }

    private fun stopTestDetection() {
        testDetector?.stop()
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean =
        when (gesture) {
            GlassGestureDetector.Gesture.SWIPE_FORWARD -> {
                if (viewPager.currentItem < NUM_PAGES - 1) {
                    viewPager.setCurrentItem(viewPager.currentItem + 1, true)
                }
                true
            }
            GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
                if (viewPager.currentItem > 0) {
                    viewPager.setCurrentItem(viewPager.currentItem - 1, true)
                }
                true
            }
            GlassGestureDetector.Gesture.TAP -> {
                when (viewPager.currentItem) {
                    POSITION_TOGGLE -> toggleTiltToWake()
                    POSITION_SENSITIVITY -> cycleSensitivity()
                    POSITION_TEST -> {
                        // In test mode, tapping restarts/resets test status
                        testViewHolder?.triggerBanner?.text = getString(R.string.tilt_test_ready)
                        testViewHolder?.triggerBanner?.setTextColor(0xCCFFFFFF.toInt())
                    }
                }
                true
            }
            else -> super.onGesture(gesture)
        }

    private class TiltSettingsAdapter(
        private val getToggleState: () -> Boolean,
        private val getSensitivity: () -> TiltSensitivity,
        private val onToggleClick: () -> Unit,
        private val onSensitivityClick: () -> Unit,
        private val onBindTest: (TestViewHolder) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount(): Int = NUM_PAGES

        override fun getItemViewType(position: Int): Int = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                POSITION_TOGGLE -> {
                    val view = inflater.inflate(R.layout.layout_tilt_toggle_page, parent, false)
                    ToggleViewHolder(view)
                }
                POSITION_SENSITIVITY -> {
                    val view = inflater.inflate(R.layout.layout_tilt_sensitivity_page, parent, false)
                    SensitivityViewHolder(view)
                }
                POSITION_TEST -> {
                    val view = inflater.inflate(R.layout.layout_tilt_test_page, parent, false)
                    TestViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid viewType: $viewType")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is ToggleViewHolder -> {
                    val enabled = getToggleState()
                    holder.status.text = if (enabled) {
                        holder.itemView.context.getString(R.string.tilt_status_enabled)
                    } else {
                        holder.itemView.context.getString(R.string.tilt_status_disabled)
                    }
                    holder.actionHint.text = if (enabled) {
                        holder.itemView.context.getString(R.string.tilt_tap_to_turn_off)
                    } else {
                        holder.itemView.context.getString(R.string.tilt_tap_to_turn_on)
                    }
                    val iconColor = ContextCompat.getColor(
                        holder.itemView.context,
                        if (enabled) R.color.glass_primary_text else R.color.glass_hint_text
                    )
                    holder.icon.setColorFilter(iconColor)
                    holder.icon.alpha = 1.0f
                    holder.itemView.setOnClickListener { onToggleClick() }
                }
                is SensitivityViewHolder -> {
                    val sensitivity = getSensitivity()
                    holder.level.setText(sensitivity.titleRes)
                    holder.desc.setText(sensitivity.descriptionRes)
                    holder.itemView.setOnClickListener { onSensitivityClick() }
                }
                is TestViewHolder -> {
                    val sensitivity = getSensitivity()
                    holder.thresholdDisplay.text = holder.itemView.context.getString(
                        R.string.tilt_threshold_format,
                        sensitivity.thresholdDegrees
                    )
                    holder.currentAngle.text = "--°"
                    holder.triggerBanner.text = holder.itemView.context.getString(R.string.tilt_test_ready)
                    holder.triggerBanner.setTextColor(
                        ContextCompat.getColor(holder.itemView.context, R.color.glass_subtitle_text)
                    )
                    onBindTest(holder)
                }
            }
        }
    }

    class ToggleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.toggle_icon)
        val status: TextView = itemView.findViewById(R.id.toggle_status)
        val actionHint: TextView = itemView.findViewById(R.id.toggle_action_hint)
    }

    class SensitivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val level: TextView = itemView.findViewById(R.id.sensitivity_level)
        val desc: TextView = itemView.findViewById(R.id.sensitivity_desc)
    }

    class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thresholdDisplay: TextView = itemView.findViewById(R.id.threshold_display)
        val currentAngle: TextView = itemView.findViewById(R.id.current_angle)
        val triggerBanner: TextView = itemView.findViewById(R.id.trigger_banner)
    }

    companion object {
        const val POSITION_TOGGLE = 0
        const val POSITION_SENSITIVITY = 1
        const val POSITION_TEST = 2
        const val NUM_PAGES = 3
    }
}
