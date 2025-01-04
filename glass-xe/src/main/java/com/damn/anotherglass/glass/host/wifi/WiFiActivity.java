package com.damn.anotherglass.glass.host.wifi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.damn.anotherglass.glass.host.R;
import com.damn.anotherglass.shared.wifi.WiFiConfiguration;
import com.google.android.glass.widget.CardBuilder;

public class WiFiActivity extends Activity {

    private static final String ARG_WIFI_SSID = "ssid";
    private static final String ARG_WIFI_PASS = "pass";
    private static final String TAG = "WiFiActivity";

    private WiFiConnector mConnector;

    public static void start(@NonNull Context context,
                             @NonNull WiFiConfiguration wifiConfiguration) {
        Intent intent = new Intent(context, WiFiActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(ARG_WIFI_SSID, wifiConfiguration.ssid)
                .putExtra(ARG_WIFI_PASS, wifiConfiguration.password);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String ssid = getIntent().getStringExtra(ARG_WIFI_SSID);
        String pass = getIntent().getStringExtra(ARG_WIFI_PASS);
        mConnector = new WiFiConnector(this) {
            @Override
            protected void onConnected() {
                super.onConnected();
                Log.d(TAG, "WiFi connected");
                Toast.makeText(WiFiActivity.this, getString(R.string.msg_connected_to_s, ssid), Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            protected void onFailed() {
                super.onFailed();
                Toast.makeText(WiFiActivity.this, R.string.msg_failed_to_connect, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to connect to WiFi");
                finish();
            }
        };
        if (!mConnector.startConnecting(ssid, pass)) {
            Log.e(TAG, "Failed to initiate connection to WiFi");
            Toast.makeText(WiFiActivity.this, R.string.msg_failed_to_connect, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (mConnector.isConnected()) {
            Log.d(TAG, "WiFi connected");
            Toast.makeText(this, getString(R.string.msg_connected_to_s, ssid), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        View view = new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(getString(R.string.msg_connecting_to_s, ssid))
                .getView();
        setContentView(view);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mConnector.stop();
    }

}
