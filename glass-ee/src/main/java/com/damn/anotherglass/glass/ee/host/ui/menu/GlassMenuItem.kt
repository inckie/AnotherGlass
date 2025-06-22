package com.damn.anotherglass.glass.ee.host.ui.menu

import android.graphics.drawable.Drawable

data class GlassMenuItem(
    val id: Int,
    val text: String,
    val icon: Drawable?,
    val tag: String? = null
)