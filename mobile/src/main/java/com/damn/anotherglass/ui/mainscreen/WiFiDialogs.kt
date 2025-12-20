package com.damn.anotherglass.ui.mainscreen


import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.damn.anotherglass.databinding.ViewWifiDialogBinding
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.wifi.WiFiAPI
import com.damn.anotherglass.shared.wifi.WiFiConfiguration
import com.damn.anotherglass.ui.MainActivity
import com.damn.anotherglass.utility.QR2
import com.google.zxing.WriterException

fun connectWiFi(activity: ComponentActivity, serviceController: IServiceController) {
    val binding = ViewWifiDialogBinding.inflate(activity.layoutInflater)
    AlertDialog.Builder(activity)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            if (TextUtils.isEmpty(binding.edSsid.text)) return@setPositiveButton
            // pass can be empty
            serviceController.getService()?.send(
                RPCMessage(
                    WiFiAPI.ID,
                    WiFiConfiguration(binding.edSsid.toString(), binding.edPassword.toString())
                )
            )
        }
        .setNegativeButton(android.R.string.cancel, null)
        .setView(binding.root)
        .show()
}

fun showIPAddressDialog(activity: Activity) {
    val wm = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    @Suppress("DEPRECATION")
    val ipAddress = Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
    try {
        // todo: add device name to QR code data, in format "xxx.xxx.xxx.xxx|Device Name"
        val bitmap = QR2.generateBitmap(ipAddress, 400)
        val imageView = ImageView(activity).apply {
            setImageBitmap(bitmap)
            drawable.isFilterBitmap = false
        }
        AlertDialog.Builder(activity)
            .setTitle(ipAddress)
            .setView(imageView)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    } catch (e: WriterException) {
        Log.e(MainActivity.Companion.TAG, "showIPAddressDialog: ", e)
    }
}