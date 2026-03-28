package com.damn.glass.shared.maps

import android.location.Location
import java.util.Locale

object MapBitmap {

    @JvmStatic
    fun getMapUrl(location: Location): String {
        return String.format(
            Locale.getDefault(),
            "https://static-maps.yandex.ru/1.x/?lang=en_US" +
                    "&size=640,360&z=15&l=map&pt=" +
                    "%f,%f" +
                    ",pm2rdl",
            location.longitude, location.latitude
        )
    }
}