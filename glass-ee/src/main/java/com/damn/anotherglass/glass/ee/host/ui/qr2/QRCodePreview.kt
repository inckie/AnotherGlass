package com.damn.anotherglass.glass.ee.host.ui.qr2

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.damn.anotherglass.glass.ee.host.ui.qr2.QRCodeImageAnalysis.QrCodeAnalysisCallback
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRCodePreview(
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun startCamera(callback: QrCodeAnalysisCallback) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }

            val qrCodeAnalyzer = QRCodeImageAnalysis(
                cameraExecutor,
                callback
            )

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    qrCodeAnalyzer.buildUseCase()
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(previewView.context))
    }

    fun release() {
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "QRCodePreview"
    }
}
