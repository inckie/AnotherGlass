package com.damn.anotherglass.glass.ee.host.ui.cards

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.BatteryStatus
import com.damn.anotherglass.glass.ee.host.core.ConnectionUtils.getHostIPAddress
import com.damn.anotherglass.glass.ee.host.core.IService
import com.damn.anotherglass.glass.ee.host.databinding.LayoutCardServiceBinding
import com.damn.anotherglass.glass.ee.host.ui.qr2.CameraActivity
import com.damn.anotherglass.glass.ee.host.ui.MainActivity
import com.damn.anotherglass.glass.ee.host.ui.menu.DynamicMenuActivity

class ServiceStateCard : BaseFragment() {

    // todo:
    //  - voice commands (only for some time interval, e.g. 5 seconds, to conserve battery)
    // todo WIP:
    //  - persist last used IPs, maybe also remember WiFi name (will require location permission)

    private lateinit var binding: LayoutCardServiceBinding
    private val batteryStatus: BatteryStatus by lazy { BatteryStatus(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LayoutCardServiceBinding.inflate(layoutInflater)
        binding.serviceState.apply {
            val state = mainActivity().getServiceState()
            setText(serviceState(state.value))
            binding.hint.text = serviceHint(state.value, resources)
            state.observe(viewLifecycleOwner) {
                setText(serviceState(it))
                binding.hint.text = serviceHint(it, resources)
            }
        }
        return binding.root
    }

    override fun onSingleTapUp() {
        super.onSingleTapUp()
        mainActivity().let {
            if (null == it.getServiceState().value) {
                it.tryStartService()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            REQUEST_CODE_PICK_IP -> data?.getStringExtra(DynamicMenuActivity.EXTRA_SELECTED_ITEM_TAG)?.let { tag ->
                Log.d("IPPicker", "Selected IP: $tag")
                when(tag) {
                    // todo: check if we have wifi connection, and it looks like tethering one
                    "gateway_ip" -> mainActivity().tryStartService()
                    "barcode_scanner" -> scanBarcode()
                    else -> mainActivity().tryStartService(tag) // recent IP
                }
            }
            REQUEST_CODE_SCAN_BARCODE -> data?.getStringExtra(CameraActivity.SCAN_RESULT)?.let {
                it.substringBefore("|").also { ip ->
                    Log.d("BarcodeScanner", "Scanned IP: $ip")
                    mainActivity().tryStartService(ip)
                }
            }
        }
    }

    private fun ServiceStateCard.scanBarcode() {
        try {
            val intent = Intent(requireActivity(), CameraActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCAN_BARCODE)
        } catch (e: ActivityNotFoundException) {
            Log.e("ServiceStateCard", "Error starting barcode scanner", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        batteryStatus.observe(viewLifecycleOwner) {
            // todo: replace with tile (text is too small)
            binding.footer.text = BatteryStatus.batteryStatusString(it)
        }
    }

    override fun onTapAndHold() {
        super.onTapAndHold()
        val mainActivity = mainActivity()
        val serviceState = mainActivity.getServiceState().value
        when (serviceState) {
            null, IService.ServiceState.DISCONNECTED -> showIPPicker(mainActivity)
            else -> mainActivity.stopService()
        }
    }

    private fun showIPPicker(activity: MainActivity) {
        DynamicMenuActivity.createIntent(activity, ipMenuItems(activity)).also {
            startActivityForResult(it, REQUEST_CODE_PICK_IP)
        }
    }

    private fun mainActivity() = requireActivity() as MainActivity

    companion object {

        private const val REQUEST_CODE_PICK_IP = 1
        private const val REQUEST_CODE_SCAN_BARCODE = 2

        @JvmStatic
        fun newInstance(): ServiceStateCard = ServiceStateCard()

        @StringRes
        private fun serviceState(state: IService.ServiceState?): Int = when (state) {
            null -> R.string.msg_service_not_running
            IService.ServiceState.INITIALIZING -> R.string.msg_service_initializing
            IService.ServiceState.WAITING -> R.string.msg_service_waiting
            IService.ServiceState.CONNECTED -> R.string.msg_service_connected
            IService.ServiceState.DISCONNECTED -> R.string.msg_service_disconnected
        }

        fun serviceHint(state: IService.ServiceState?, resources: Resources): String = when (state) {
            null -> resources.getString(R.string.msg_service_not_running_hint)
            IService.ServiceState.INITIALIZING -> ""
            IService.ServiceState.WAITING -> ""
            IService.ServiceState.CONNECTED -> resources.getString(R.string.msg_service_connected_hint)
            IService.ServiceState.DISCONNECTED -> ""
        }

        private fun ipMenuItems(context: Context) = arrayListOf(
            DynamicMenuActivity.DynamicMenuItem(
                id = 0,
                text = context.getString(R.string.lbl_gateway_ip),
                icon = R.drawable.ic_wifi_tethering,
                tag = "gateway_ip",
                subtext = getHostIPAddress(context)
            ),
            // barcode format: `xxx.xxx.xxx.xxx[|User readable name]`
            DynamicMenuActivity.DynamicMenuItem(
                id = 1,
                text = context.getString(R.string.lbl_barcode),
                icon = R.drawable.ic_qr_code_scanner,
                tag = "barcode_scanner"
            ),
            // todo: last used IPs
            DynamicMenuActivity.DynamicMenuItem(
                id = 2,
                text = "192.168.1.241",
                icon = R.drawable.ic_save,
                tag = "192.168.1.241"
            )
        )
    }
}
