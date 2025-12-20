package com.damn.anotherglass.core

import com.damn.anotherglass.shared.device.BatteryStatusData
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds information about connected device.
 * Exposed as a flow from [GlassService].
 * If null, no device is connected.
 */
data class ConnectedDevice(
    val name: StateFlow<String>,
    val batteryStatus: StateFlow<BatteryStatusData?> // null if not available yet
)
