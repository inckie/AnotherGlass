package com.damn.anotherglass.utility

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.damn.anotherglass.shared.BinaryData
import java.io.ByteArrayOutputStream

fun BinaryData?.toBitmap(): Bitmap? {
    val bytes = this?.bytes ?: return null
    if (bytes.isEmpty()) return null
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun Bitmap.toPngBinaryData(): BinaryData? {
    return toBinaryData(Bitmap.CompressFormat.PNG, 100, MIME_TYPE_PNG)
}

fun Bitmap.toJpegBinaryData(quality: Int): BinaryData? {
    return toBinaryData(Bitmap.CompressFormat.JPEG, quality, MIME_TYPE_JPEG)
}

private fun Bitmap.toBinaryData(
    format: Bitmap.CompressFormat,
    quality: Int,
    mimeType: String,
): BinaryData? {
    return ByteArrayOutputStream().use { output ->
        if (compress(format, quality, output)) {
            BinaryData(output.toByteArray(), mimeType)
        } else {
            null
        }
    }
}

private const val MIME_TYPE_PNG = "image/png"
private const val MIME_TYPE_JPEG = "image/jpeg"

