package com.damn.anotherglass.glass.ee.host.ui.qr2

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.ui.BaseActivity
import com.damn.anotherglass.glass.ee.host.utility.hasPermission

class CameraActivity : BaseActivity(), QRCodeImageAnalysis.QrCodeAnalysisCallback {

    private lateinit var qrCodePreview: QRCodePreview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        val previewView = findViewById<PreviewView>(R.id.preview_view)
        qrCodePreview = QRCodePreview(this, previewView)

        if (allPermissionsGranted())
            qrCodePreview.startCamera(this)
        else
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted())
                qrCodePreview.startCamera(this@CameraActivity)
            else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        qrCodePreview.release()
    }

    override fun onQrCodeDetected(result: String) {
        setResult(RESULT_OK, intent.putExtra(SCAN_RESULT, result))
        finish()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        baseContext.hasPermission(it)
    }

    companion object {
        const val SCAN_RESULT = "SCAN_RESULT"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}