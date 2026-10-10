package com.damn.anotherglass.glass.ee.host.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.HostService
import com.damn.anotherglass.glass.ee.host.ui.BaseActivity
import com.damn.anotherglass.glass.ee.host.ui.launcher.AppLauncherActivity
import com.damn.anotherglass.glass.ee.host.utility.isRunning
import com.example.glass.ui.GlassGestureDetector

class VoiceMenuActivity : BaseActivity() {

    data class VoiceCommandItem(
        val id: Int,
        val label: String,
        val action: String,
        @param:DrawableRes val icon: Int = 0,
        val intent: Intent? = null
    )

    private val commands = mutableListOf<VoiceCommandItem>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CommandAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(FEATURE_VOICE_COMMANDS)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_menu)

        initCommands()

        recyclerView = findViewById(R.id.commandRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = CommandAdapter(commands) { position ->
            if (position in commands.indices) {
                adapter.selectedPosition = position
                executeCommand(commands[position])
            }
        }
        recyclerView.adapter = adapter
    }

    private fun initCommands() {
        val canStart = intent.getBooleanExtra(
            EXTRA_CAN_START_SERVICE,
            !isRunning<HostService>()
        )

        if (canStart) {
            commands.add(
                VoiceCommandItem(
                    id = COMMAND_ID_SERVICE,
                    label = getString(R.string.voice_start_service),
                    action = ACTION_START_SERVICE,
                    icon = R.drawable.ic_phone_android
                )
            )
        } else {
            commands.add(
                VoiceCommandItem(
                    id = COMMAND_ID_SERVICE,
                    label = getString(R.string.voice_stop_service),
                    action = ACTION_STOP_SERVICE,
                    icon = R.drawable.ic_phone_android
                )
            )
        }

        commands.add(
            VoiceCommandItem(
                id = COMMAND_ID_START_APP,
                label = getString(R.string.voice_start_app),
                action = ACTION_START_APP,
                icon = R.drawable.ic_style
            )
        )
    }

    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        if (featureId != FEATURE_VOICE_COMMANDS) {
            return super.onCreatePanelMenu(featureId, menu)
        }
        menu.clear()
        commands.forEachIndexed { index, command ->
            menu.add(0, index, index, command.label)
            when (command.action) {
                ACTION_START_SERVICE -> {
                    menu.add(0, index, index, "Connect to phone")
                    menu.add(0, index, index, "Connect")
                }
                ACTION_STOP_SERVICE -> {
                    menu.add(0, index, index, "Disconnect from phone")
                    menu.add(0, index, index, "Disconnect")
                }
                ACTION_START_APP -> {
                    menu.add(0, index, index, "Start application")
                    menu.add(0, index, index, "Start app")
                }
            }
        }
        return commands.isNotEmpty()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val index = item.itemId
        if (index in commands.indices) {
            executeCommand(commands[index])
            return true
        }
        return super.onContextItemSelected(item)
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean = when (gesture) {
        GlassGestureDetector.Gesture.SWIPE_FORWARD -> {
            if (commands.isNotEmpty()) {
                val next = (adapter.selectedPosition + 1).coerceAtMost(commands.size - 1)
                adapter.selectedPosition = next
                recyclerView.smoothScrollToPosition(next)
            }
            true
        }
        GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
            if (commands.isNotEmpty()) {
                val prev = (adapter.selectedPosition - 1).coerceAtLeast(0)
                adapter.selectedPosition = prev
                recyclerView.smoothScrollToPosition(prev)
            }
            true
        }
        GlassGestureDetector.Gesture.TAP -> {
            if (commands.isNotEmpty() && adapter.selectedPosition in commands.indices) {
                executeCommand(commands[adapter.selectedPosition])
            }
            true
        }
        else -> super.onGesture(gesture)
    }

    private fun executeCommand(command: VoiceCommandItem) {
        if (command.intent != null) {
            startActivity(command.intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
            return
        }
        when (command.action) {
            ACTION_START_APP -> {
                startActivity(Intent(this, AppLauncherActivity::class.java))
                finish()
            }
            ACTION_START_SERVICE,
            ACTION_STOP_SERVICE -> {
                val data = Intent().putExtra(EXTRA_ACTION, command.action)
                setResult(RESULT_OK, data)
                finish()
            }
        }
    }

    private class CommandAdapter(
        private val items: List<VoiceCommandItem>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<CommandAdapter.ViewHolder>() {

        var selectedPosition: Int = 0
            set(value) {
                val prev = field
                field = value
                notifyItemChanged(prev)
                notifyItemChanged(value)
            }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.command_icon)
            val text: TextView = view.findViewById(R.id.command_text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_voice_command_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.text.text = item.label
            if (item.icon != 0) {
                holder.icon.setImageResource(item.icon)
                holder.icon.visibility = View.VISIBLE
            } else {
                holder.icon.visibility = View.GONE
            }
            val isSelected = position == selectedPosition
            holder.text.alpha = if (isSelected) 1.0f else 0.5f
            holder.icon.alpha = if (isSelected) 1.0f else 0.5f
            holder.itemView.setOnClickListener {
                onItemClick(position)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_CAN_START_SERVICE = "extra_can_start_service"

        const val ACTION_START_SERVICE = "action_start_service"
        const val ACTION_STOP_SERVICE = "action_stop_service"
        const val ACTION_START_APP = "action_start_app"

        private const val COMMAND_ID_SERVICE = 1
        private const val COMMAND_ID_START_APP = 2

        private const val FEATURE_VOICE_COMMANDS = 14

        @JvmStatic
        fun createIntent(context: Context, canStartService: Boolean): Intent =
            Intent(context, VoiceMenuActivity::class.java).apply {
                putExtra(EXTRA_CAN_START_SERVICE, canStartService)
            }
    }
}
