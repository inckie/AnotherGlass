package com.damn.anotherglass.glass.ee.host.ui.cards

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.BatteryStatus
import com.damn.anotherglass.glass.ee.host.core.IService
import com.damn.anotherglass.glass.ee.host.core.Settings
import com.damn.anotherglass.glass.ee.host.databinding.LayoutCardServiceBinding
import com.damn.anotherglass.glass.ee.host.ui.launcher.AppLauncherActivity
import com.damn.anotherglass.glass.ee.host.ui.qr2.CameraActivity
import com.damn.anotherglass.glass.ee.host.ui.MainActivity
import com.damn.anotherglass.glass.ee.host.ui.menu.DynamicMenuActivity
import com.damn.anotherglass.glass.ee.host.ui.voice.VoiceMenuActivity
import android.content.res.ColorStateList
import android.graphics.Color
import com.damn.glass.shared.rpc.ConnectionUtils.getHostIPAddress

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
        val state = mainActivity().getServiceState()
        binding.serviceState.setText(serviceState(state.value))
        binding.hint.text = serviceHint(state.value, resources)
        updateStatusIcon(state.value)
        state.observe(viewLifecycleOwner) {
            binding.serviceState.setText(serviceState(it))
            binding.hint.text = serviceHint(it, resources)
            updateStatusIcon(it)
        }
        binding.okGlass.setOnClickListener { openVoiceMenu() }
        return binding.root
    }

    private fun updateStatusIcon(state: IService.ServiceState?) {
        when (state) {
            null, IService.ServiceState.DISCONNECTED -> {
                binding.statusIcon.setImageResource(R.drawable.ic_phone_android)
                binding.statusIcon.imageTintList = null
                binding.statusIcon.alpha = 0.85f
            }
            IService.ServiceState.INITIALIZING, IService.ServiceState.WAITING -> {
                binding.statusIcon.setImageResource(R.drawable.ic_wifi_tethering)
                binding.statusIcon.imageTintList = null
                binding.statusIcon.alpha = 1.0f
            }
            IService.ServiceState.CONNECTED -> {
                binding.statusIcon.setImageResource(R.drawable.ic_phone_android)
                binding.statusIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                binding.statusIcon.alpha = 1.0f
            }
        }
    }

    override fun onSingleTapUp() {
        super.onSingleTapUp()
        mainActivity().let {
            val state = it.getServiceState().value
            if (state == null || state == IService.ServiceState.DISCONNECTED) {
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
                    Settings(requireContext()).lastScannedServerIp = ip
                    mainActivity().tryStartService(ip)
                }
            }
            REQUEST_CODE_VOICE_MENU -> data?.getStringExtra(VoiceMenuActivity.EXTRA_ACTION)?.let { action ->
                when (action) {
                    VoiceMenuActivity.ACTION_START_SERVICE -> mainActivity().tryStartService()
                    VoiceMenuActivity.ACTION_STOP_SERVICE -> mainActivity().stopService()
                    VoiceMenuActivity.ACTION_START_APP -> startActivity(Intent(requireContext(), AppLauncherActivity::class.java))
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
            binding.batteryValue.text = BatteryStatus.batteryStatusString(it)
        }
    }

    override fun onTwoFingerTap() {
        startActivity(Intent(requireContext(), AppLauncherActivity::class.java))
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

    override fun onCreateVoiceCommandMenu(menu: Menu): Boolean {
        val ctx = context ?: return false
        val okGlass = ctx.getString(R.string.ok_glass)
        menu.add(0, VOICE_COMMAND_OK_GLASS, 0, okGlass)
        menu.add(0, VOICE_COMMAND_OK_GLASS, 0, "ok glass")
        return true
    }

    override fun onVoiceCommand(itemId: Int): Boolean = when (itemId) {
        VOICE_COMMAND_OK_GLASS -> {
            openVoiceMenu()
            true
        }
        else -> false
    }

    private fun openVoiceMenu() {
        val ctx = context ?: return
        val serviceState = mainActivityOrNull()?.getServiceState()?.value
        val canStart = serviceState == null || serviceState == IService.ServiceState.DISCONNECTED
        val intent = VoiceMenuActivity.createIntent(ctx, canStart)
        startActivityForResult(intent, REQUEST_CODE_VOICE_MENU)
    }

    private fun showIPPicker(activity: MainActivity) {
        DynamicMenuActivity.createIntent(activity, ipMenuItems(activity)).also {
            startActivityForResult(it, REQUEST_CODE_PICK_IP)
        }
    }

    private fun mainActivity() = requireActivity() as MainActivity

    private fun mainActivityOrNull(): MainActivity? = activity as? MainActivity

    companion object {

        private const val REQUEST_CODE_PICK_IP = 1
        private const val REQUEST_CODE_SCAN_BARCODE = 2
        private const val REQUEST_CODE_VOICE_MENU = 3
        private const val VOICE_COMMAND_OK_GLASS = 10_000

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
            IService.ServiceState.INITIALIZING -> resources.getString(R.string.msg_service_initializing)
            IService.ServiceState.WAITING -> resources.getString(R.string.msg_service_waiting)
            IService.ServiceState.CONNECTED -> resources.getString(R.string.msg_service_connected_hint)
            IService.ServiceState.DISCONNECTED -> resources.getString(R.string.msg_service_disconnected_hint)
        }

        private fun ipMenuItems(context: Context): ArrayList<DynamicMenuActivity.DynamicMenuItem> {
            val menuItems = arrayListOf(
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
                )
            )

            Settings(context).lastScannedServerIp?.takeIf { it.isNotBlank() }?.let { lastIp ->
                menuItems.add(
                    DynamicMenuActivity.DynamicMenuItem(
                        id = 2,
                        text = lastIp,
                        icon = R.drawable.ic_outline_history,
                        tag = lastIp
                    )
                )
            }

            // test IPs
            menuItems.add(
                DynamicMenuActivity.DynamicMenuItem(
                    id = 3,
                    text = "192.168.1.241",
                    icon = R.drawable.ic_save,
                    tag = "192.168.1.241"
                )
            )
            menuItems.add(
                DynamicMenuActivity.DynamicMenuItem(
                    id = 4,
                    text = "192.168.1.180",
                    icon = R.drawable.ic_save,
                    tag = "192.168.1.180"
                )
            )
            return menuItems
        }
    }
}
