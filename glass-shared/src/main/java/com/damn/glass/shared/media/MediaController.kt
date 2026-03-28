package com.damn.glass.shared.media

import com.damn.anotherglass.shared.media.MediaAPI
import com.damn.anotherglass.shared.media.MediaCommandData
import com.damn.anotherglass.shared.media.MediaStateData
import com.damn.anotherglass.shared.rpc.RPCMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Minimal abstraction so glass-shared does not depend on a concrete transport. */
fun interface IRPCSender {
    fun send(message: RPCMessage)
}

class MediaController {

    private val _state = MutableStateFlow<MediaStateData?>(null)
    private var sender: IRPCSender? = null

    fun getState(): StateFlow<MediaStateData?> = _state

    fun onMediaStateUpdate(state: MediaStateData) {
        _state.value = state
    }

    fun onServiceConnected() {
        // Clear stale state on reconnect
        _state.value = null
    }

    /** Called by the host service when it starts, so UI commands can be forwarded. */
    fun setService(rpsSender: IRPCSender) {
        sender = rpsSender
    }

    /** Called by the host service when it stops. */
    fun clearService() {
        sender = null
        _state.value = null
    }

    /** Called by UI cards to send a playback command to the phone. */
    fun sendCommand(command: MediaCommandData) {
        sender?.send(RPCMessage(MediaAPI.ID, command))
    }

    companion object {
        @JvmStatic
        val instance: MediaController by lazy { MediaController() }
    }
}
