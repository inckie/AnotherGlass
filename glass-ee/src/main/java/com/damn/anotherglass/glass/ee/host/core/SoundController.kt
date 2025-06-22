package com.damn.anotherglass.glass.ee.host.core

import android.content.Context
import android.media.SoundPool
import com.damn.anotherglass.glass.ee.host.R

class SoundController(private val context: Context) {

    // todo:
    //  - load on demand
    //  - enable/disable in settings

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .build()

    private val soundsMap = SoundEffect.entries.associateWith {
        soundPool.load(context, it.resourceId, 1)
    }

    fun playSound(effect: SoundEffect) {
        soundsMap[effect]?.let {
            soundPool.play(it, 1f, 1f, 0, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }

    enum class SoundEffect(
        val resourceId: Int
    ) {
        ConnectionLost(R.raw.connection_lost),
        NotificationPosted(R.raw.notification_posted),
        PhotoTaken(R.raw.photo_taken),
    }
}