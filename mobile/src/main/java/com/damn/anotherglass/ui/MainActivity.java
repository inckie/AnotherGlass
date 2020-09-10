package com.damn.anotherglass.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.damn.anotherglass.R;
import com.damn.anotherglass.core.GlassService;
import com.damn.anotherglass.core.Settings;
import com.damn.anotherglass.extensions.notifications.NotificationService;
import com.damn.anotherglass.shared.RPCMessage;
import com.damn.anotherglass.shared.wifi.WiFiAPI;
import com.damn.anotherglass.shared.wifi.WiFiConfiguration;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;


public class MainActivity extends AppCompatActivity {

    // Code is a bit ugly, since we cant use AndroidX and JetPack
    // due to Glass SDK limitations on Gradle version

    private static final String LOG_TAG = "MainActivity";

    private Settings mSettings;

    private final GlassServiceConnection mConnection = new GlassServiceConnection();

    private Switch mSwService;

    private View mCntControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSettings = new Settings(this);

        mCntControls = findViewById(R.id.cnt_controls);

        // Start/Stop
        mSwService = findViewById(R.id.toggle_service);
        mSwService.setChecked(GlassService.isRunning(this));
        mSwService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked == GlassService.isRunning(this))
                return;
            if (isChecked)
                start();
            else
                stop();
            // Tiny hack to avoid real checks and subscriptions to service lifecycle.
            // We rely on a fact that, if service will start,
            // we will be able to bind and then receive onServiceDisconnected
            // even if it will stop right away due to missing BT connection or something else.
            buttonView.post(() -> buttonView.setChecked(GlassService.isRunning(this)));
        });

        // GPS
        Switch gps = findViewById(R.id.toggle_gps);
        gps.setChecked(mSettings.isGPSEnabled());
        gps.setOnCheckedChangeListener((buttonView, isChecked) -> mSettings.setGPSEnabled(isChecked));

        // Notifications
        Switch notifications = findViewById(R.id.toggle_notifications);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            notifications.setVisibility(View.GONE);
        } else {
            notifications.setChecked(mSettings.isNotificationsEnabled() && NotificationService.isEnabled(this));
            CompoundButton.OnCheckedChangeListener changeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    boolean changed = MainActivity.this.toggleNotifications(isChecked);
                    if (changed)
                        return;
                    notifications.setOnCheckedChangeListener(null);
                    notifications.setChecked(!isChecked);
                    notifications.setOnCheckedChangeListener(this);
                }
            };
            notifications.setOnCheckedChangeListener(changeListener);
        }

        //WiFi
        findViewById(R.id.btn_connect_wifi).setOnClickListener(view -> connectWiFi());
        updateUI();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private boolean toggleNotifications(boolean enabled) {
        if(enabled && !NotificationService.isEnabled(this)) {
            askEnableNotificationService();
            return false;
        }
        mSettings.setNotificationsEnabled(enabled);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (GlassService.isRunning(this))
            mConnection.bindGlassService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mConnection.unbindGlassService();
    }

    private void updateUI() {
        boolean running = GlassService.isRunning(this);
        mCntControls.setVisibility(running ? View.VISIBLE : View.GONE);
    }

    private void start() {
        // don't bother and always require all permissions
        if (!hasGeoPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }
        if(!GlassService.isRunning(this)) {
            startService(new Intent(MainActivity.this, GlassService.class));
        }
        mConnection.bindGlassService();
    }

    private void stop() {
        stopService(new Intent(MainActivity.this, GlassService.class));
    }

    private void connectWiFi() {
        View view = getLayoutInflater().inflate(R.layout.view_wifi_dialog, null);
        EditText ssid = view.findViewById(R.id.ed_ssid);
        EditText pass = view.findViewById(R.id.ed_password);
        new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> connectToWiFi(ssid.getText(), pass.getText()))
                .setNegativeButton(android.R.string.cancel, null)
                .setView(view)
                .show();
    }

    private void connectToWiFi(@NonNull CharSequence ssid,
                               @NonNull CharSequence pass) {
        if (TextUtils.isEmpty(ssid))
            return;
        // pass can be empty
        GlassService service = mConnection.getService();
        if(null == service)
            return;
        service.send(new RPCMessage(
                WiFiAPI.ID,
                new WiFiConfiguration(ssid.toString(), pass.toString())));
    }

    private boolean hasGeoPermission() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!hasGeoPermission())
            return;
        startService(new Intent(MainActivity.this, GlassService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void askEnableNotificationService() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.msg_notification_listener_service_title)
                .setMessage(R.string.notification_listener_service_message)
                .setPositiveButton(android.R.string.yes, (dialog, id) ->
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)))
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private class GlassServiceConnection implements ServiceConnection {

        private GlassService mGlassService;

        private boolean mBound;

        @Nullable
        public GlassService getService() {
            return mGlassService;
        }

        public void bindGlassService() {
            try {
                if (mBound) {
                    unbindService(mConnection);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mBound = bindService(new Intent(MainActivity.this, GlassService.class), mConnection, 0);
        }

        public void unbindGlassService() {
            if (!mBound)
                return;
            mBound = false;
            mGlassService = null;
            unbindService(mConnection);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGlassService = ((GlassService.LocalBinder) service).getService();
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mGlassService = null;
            if(mBound) {
                // service stopped on its own, unbind from it (it won't restart on its own)
                mSwService.setChecked(false);
                unbindGlassService();
                updateUI();
            }
        }
    }
}
