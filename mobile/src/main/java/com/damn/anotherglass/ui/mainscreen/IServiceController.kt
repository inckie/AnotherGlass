package com.damn.anotherglass.ui.mainscreen

import com.damn.anotherglass.core.ConnectedDevice
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.shared.rpc.RPCMessage
import kotlinx.coroutines.flow.StateFlow

interface IServiceController {
    val connectedDevice: StateFlow<ConnectedDevice?>
    fun startService()
    fun stopService()
    fun send(message: RPCMessage)
    fun getService(): GlassService?
}
