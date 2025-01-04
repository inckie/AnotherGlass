package com.damn.anotherglass.glass.host.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import java.util.List;

public class WiFiConnector {

    private String mSSID;
    private String mPass;

    private boolean mRegistered;
    private boolean mConnected;

    private final Context mCtx;

    private final WifiManager mWiFi;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        private int mState = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (null == action)
                return;

            //noinspection IfCanBeSwitch
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if(intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false) && isSSIDConnected()) {
                    onConnected();
                }
            } else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                if (!isSSIDConnected()) {
                    connectToAP(mWiFi.getScanResults());
                } else {
                    onConnected();
                }
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                if (mState == state)
                    return;
                mState = state;
                switch (state) {
                    case WifiManager.WIFI_STATE_DISABLED:
                    case WifiManager.WIFI_STATE_DISABLING:
                        mWiFi.setWifiEnabled(true);
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        if (!isSSIDConnected()) {
                            mWiFi.startScan();
                        } else {
                            onConnected();
                        }
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                    case WifiManager.WIFI_STATE_UNKNOWN:
                        break;
                }
            }
        }
    };

    private static final String[] sSecurityModes = {"EAP", "PSK", "WEP"};

    private static final String TAG = "WiFiConnector";

    public WiFiConnector(Context ctx) {
        mCtx = ctx;
        mWiFi = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public boolean startConnecting(String ssid, String passkey) {
        if (null == mWiFi)
            return false;
        if (mRegistered && ssid.equals(mSSID))
            return true;
        mSSID = ssid;
        mPass = passkey;
        mRegistered = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mCtx.registerReceiver(mReceiver, intentFilter);
        return true;
    }

    public void stop() {
        mConnected = false;
        if (!mRegistered)
            return;
        mCtx.unregisterReceiver(mReceiver);
        mRegistered = false;
    }

    public boolean isConnected() {
        return mConnected;
    }

    private boolean isSSIDConnected() {
        String wifi = getWiFiSSID();
        return null != wifi && wifi.equals(mSSID);
    }

    //region WiFi
    private void connectToAP(List<ScanResult> scanResultList) {

        for (ScanResult result : scanResultList) {

            if (!result.SSID.equals(mSSID))
                continue;

            String securityMode = getScanResultSecurity(result);

            WifiConfiguration wifiConfiguration;
            if (securityMode.equalsIgnoreCase("OPEN"))
                wifiConfiguration = getOpenConfiguration();
            else if (securityMode.equalsIgnoreCase("WEP"))
                wifiConfiguration = getWEPConfiguration();
            else
                wifiConfiguration = getPSKConfiguration();

            int res = mWiFi.addNetwork(wifiConfiguration);
            mWiFi.enableNetwork(res, true);
            boolean changeHappen = mWiFi.saveConfiguration();
            if (res != -1 && changeHappen) {
                onConnected();
            } else {
                Log.d(TAG, "Could not connect to " + mSSID);
                onFailed();
            }

            mWiFi.setWifiEnabled(true);
            break;
        }
    }

    @CallSuper
    protected void onFailed() {

    }

    @CallSuper
    protected void onConnected() {
        mConnected = true;
    }

    private String getScanResultSecurity(ScanResult scanResult) {
        for (String sm : sSecurityModes) {
            if (scanResult.capabilities.contains(sm))
                return sm;
        }
        return "OPEN";
    }

    @Nullable
    private String getWiFiSSID() {
        WifiInfo wifiInfo = mWiFi.getConnectionInfo();
        if (null != wifiInfo) {
            String ssid = wifiInfo.getSSID();
            if (null != ssid && !ssid.equalsIgnoreCase("<unknown ssid>"))
                return ssid.replace("\"", "");
        }
        return null;
    }

    @NonNull
    private WifiConfiguration getOpenConfiguration() {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + mSSID + "\"";
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        return wifiConfiguration;
    }

    @NonNull
    private WifiConfiguration getWEPConfiguration() {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + mSSID + "\"";
        wifiConfiguration.wepKeys[0] = "\"" + mPass + "\"";
        wifiConfiguration.wepTxKeyIndex = 0;
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        return wifiConfiguration;
    }

    @NonNull
    private WifiConfiguration getPSKConfiguration() {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + mSSID + "\"";
        wifiConfiguration.preSharedKey = "\"" + mPass + "\"";
        wifiConfiguration.hiddenSSID = true;
        wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        return wifiConfiguration;
    }
    //endregion
}
