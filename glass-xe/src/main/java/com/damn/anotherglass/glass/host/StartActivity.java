package com.damn.anotherglass.glass.host;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.damn.anotherglass.glass.host.barcode.BarcodeScannerActivity;
import com.damn.glass.shared.rpc.ConnectionUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

public class StartActivity extends Activity {

    private static final int REQUEST_SCAN_BARCODE = 501;

    private static final String CONNECTION_ACTION_SCAN_BARCODE = "scan_barcode";

    private static final String PREFS_NAME = "start_activity";
    private static final String PREF_LAST_SCANNED_IP = "last_scanned_ip";

    private CardScrollView mCardScroller;

    private List<ConnectionOption> mOptions;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mOptions = buildOptions();

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return mOptions.size();
            }

            @Override
            public Object getItem(int position) {
                return mOptions.get(position);
            }

            @Override
            public int getPosition(Object item) {
                if (!(item instanceof ConnectionOption)) {
                    return AdapterView.INVALID_POSITION;
                }
                for (int i = 0; i < mOptions.size(); i++) {
                    if (mOptions.get(i) == item) {
                        return i;
                    }
                }
                return AdapterView.INVALID_POSITION;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ConnectionOption option = mOptions.get(position);
                CardBuilder builder = new CardBuilder(StartActivity.this, CardBuilder.Layout.MENU)
                        .setText(option.titleText != null ? option.titleText : getString(option.titleRes));
                if (option.connectionType.equals(HostService.CONNECTION_TYPE_WIFI)) {
                    String gatewayIp = ConnectionUtils.getHostIPAddress(StartActivity.this);
                    builder.setFootnote(gatewayIp != null ? gatewayIp : getString(R.string.subtitle_connection_wifi));
                } else if (option.footnoteRes != null) {
                    builder.setFootnote(option.footnoteRes);
                }
                return builder.getView(convertView, parent);
            }
        });

        mCardScroller.setOnItemClickListener((parent, view, position, id) -> {
            ConnectionOption option = mOptions.get(position);
            if (CONNECTION_ACTION_SCAN_BARCODE.equals(option.connectionType)) {
                startActivityForResult(new Intent(this, BarcodeScannerActivity.class), REQUEST_SCAN_BARCODE);
                return;
            }
            Intent intent = new Intent(StartActivity.this, HostService.class)
                    .putExtra(HostService.EXTRA_CONNECTION_TYPE, option.connectionType);
            if (option.hostIp != null) {
                intent.putExtra(HostService.EXTRA_IP, option.hostIp);
            }
            startService(intent);
            finish();
        });

        setContentView(mCardScroller);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SCAN_BARCODE || resultCode != RESULT_OK || data == null) {
            return;
        }

        String scanResult = data.getStringExtra(BarcodeScannerActivity.EXTRA_SCAN_RESULT);
        if (TextUtils.isEmpty(scanResult)) {
            Toast.makeText(this, R.string.msg_invalid_barcode, Toast.LENGTH_SHORT).show();
            return;
        }

        String ip = scanResult.split("\\|", 2)[0].trim();
        if (!Patterns.IP_ADDRESS.matcher(ip).matches()) {
            Toast.makeText(this, R.string.msg_invalid_barcode, Toast.LENGTH_SHORT).show();
            return;
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_LAST_SCANNED_IP, ip)
                .apply();

        Intent intent = new Intent(this, HostService.class)
                .putExtra(HostService.EXTRA_CONNECTION_TYPE, HostService.CONNECTION_TYPE_WIFI)
                .putExtra(HostService.EXTRA_IP, ip);
        startService(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    private List<ConnectionOption> buildOptions() {
        List<ConnectionOption> options = new ArrayList<>();

        options.add(new ConnectionOption(
                R.string.title_connection_bluetooth,
                null,
                HostService.CONNECTION_TYPE_BLUETOOTH
        ));
        options.add(new ConnectionOption(
                R.string.title_connection_wifi,
                R.string.subtitle_connection_wifi,
                HostService.CONNECTION_TYPE_WIFI
        ));

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastScannedIp = prefs.getString(PREF_LAST_SCANNED_IP, null);
        if (!TextUtils.isEmpty(lastScannedIp) && Patterns.IP_ADDRESS.matcher(lastScannedIp).matches()) {
            options.add(new ConnectionOption(
                    lastScannedIp,
                    R.string.subtitle_connection_last_scanned,
                    HostService.CONNECTION_TYPE_WIFI,
                    lastScannedIp
            ));
        }

        options.add(new ConnectionOption(
                R.string.title_connection_barcode,
                R.string.subtitle_connection_barcode,
                CONNECTION_ACTION_SCAN_BARCODE
        ));
        return options;
    }

    private static class ConnectionOption {
        final String titleText;
        final int titleRes;
        final Integer footnoteRes;
        final String connectionType;
        final String hostIp;

        ConnectionOption(int titleRes, Integer footnoteRes, String connectionType) {
            this(null, titleRes, footnoteRes, connectionType, null);
        }

        ConnectionOption(String titleText, Integer footnoteRes, String connectionType, String hostIp) {
            this(titleText, 0, footnoteRes, connectionType, hostIp);
        }

        private ConnectionOption(String titleText, int titleRes, Integer footnoteRes, String connectionType, String hostIp) {
            this.titleText = titleText;
            this.titleRes = titleRes;
            this.footnoteRes = footnoteRes;
            this.connectionType = connectionType;
            this.hostIp = hostIp;
        }
    }
}
