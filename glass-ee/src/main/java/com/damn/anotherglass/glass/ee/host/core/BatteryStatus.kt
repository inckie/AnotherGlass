package com.damn.anotherglass.glass.ee.host.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.MutableLiveData

class BatteryStatus(private val context: Context) : MutableLiveData<BatteryStatus.BatteryStatusData>() {

    data class BatteryStatusData(val level: Int, val isCharging: Boolean)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = when {
                level >= 0 && scale > 0 -> (level * 100.0 / scale).toInt()
                else -> -1
            }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = BatteryManager.BATTERY_STATUS_CHARGING == status ||
                    BatteryManager.BATTERY_STATUS_FULL == status

            postValue(BatteryStatusData(percentage, isCharging))
        }
    }

    override fun onActive() {
        super.onActive()
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            context.registerReceiver(null, it)
        }
        receiver.onReceive(context, batteryStatus)
    }

    override fun onInactive() {
        super.onInactive()
        context.unregisterReceiver(receiver)
    }

    companion object {
        @JvmStatic
        fun batteryStatusString(data: BatteryStatusData): String {
            // todo: use drawables for battery icon
            val icon = "\uD83D\uDD0B" // glass does not support low battery icon
            return "$icon ${data.level}%${if (data.isCharging) "⚡" else ""}"
        }
    }
}