package com.damn.anotherglass.ui.mainscreen

import com.damn.anotherglass.core.ConnectedDevice
import com.damn.anotherglass.shared.media.MediaCommandData
import com.damn.anotherglass.shared.media.MediaStateData
import com.damn.anotherglass.shared.rpc.RPCMessage
import kotlinx.coroutines.flow.StateFlow

interface IServiceController {
    val connectedDevice: StateFlow<ConnectedDevice?>
    val mediaState: StateFlow<MediaStateData?>
    fun startService()
    fun stopService()
    fun sendMediaCommand(command: MediaCommandData)
    fun send(message: RPCMessage)
}
