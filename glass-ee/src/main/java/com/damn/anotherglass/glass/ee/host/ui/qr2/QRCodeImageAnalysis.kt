package com.damn.anotherglass.glass.ee.host.ui.qr2

import android.annotation.SuppressLint
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.nio.ByteBuffer
import java.util.concurrent.Executor

/**
 * Sets up and performs image analysis for QR code detection.
 */
class QRCodeImageAnalysis(
    private val executor: Executor,
    private val callback: QrCodeAnalysisCallback
) : ImageAnalysis.Analyzer {

    interface QrCodeAnalysisCallback {
        fun onQrCodeDetected(result: String)
    }

    /**
     * Builds and returns an [ImageAnalysis] use case configured for QR code scanning.
     */
    fun buildUseCase(): ImageAnalysis = ImageAnalysis.Builder()
        .setTargetResolution(Size(1920, 1080)) // should be enough for our short barcode
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(executor, this)
        }

    /**
     * Performs an analysis of the image, searching for the QR code, using the ZXing library.
     * This method is called for each frame from the camera.
     *
     * `@SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")`
     * is used because `image.getImage()` is an experimental API.
     */
    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) { // Renamed 'image' to 'imageProxy' for clarity
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            // IMPORTANT: Close the ImageProxy even if mediaImage is null
            imageProxy.close()
        } else {
            val buffer: ByteBuffer = mediaImage.planes[0].buffer
            val imageBytes = ByteArray(buffer.remaining())
            buffer.get(imageBytes)
            val width: Int = mediaImage.width
            val height: Int = mediaImage.height

            // Create a PlanarYUVLuminanceSource
            val source = PlanarYUVLuminanceSource(
                imageBytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = QRCodeReader().decode(
                    binaryBitmap,
                    mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                        DecodeHintType.CHARACTER_SET to "UTF-8"
                    )
                )
                if(result.text.isBlank()) return
                callback.onQrCodeDetected(result.text)
            } catch (e: com.google.zxing.NotFoundException) {
                // No QR code found in the current frame. This is expected.
            } catch (e: ChecksumException) {
                Log.e(TAG, "QR Code decoding error (ChecksumException)", e)
            } catch (e: com.google.zxing.FormatException) {
                Log.e(TAG, "QR Code decoding error (FormatException)", e)
            } finally {
                // IMPORTANT: You must close the ImageProxy, otherwise subsequent images may not be received.
                imageProxy.close()
            }
        }
    }

    companion object {
        private const val TAG: String = "QRCodeImageAnalysis"
    }
}