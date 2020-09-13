package com.damn.anotherglass.shared.utility;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.Closeable;

public class DisconnectReceiver
        extends BroadcastReceiver implements Closeable {

    private static final String TAG = "DisconnectedBroadcastReceiver";
    private final Context context;
    private final BluetoothDevice device;
    private final IListener listener;

    // I use interface just to be able to use lambda and have much cleaner client code
    public interface IListener {
        void onDeviceDisconnected();
    }

    public DisconnectReceiver(Context context,
                              BluetoothDevice device,
                              IListener listener) {
        this.context = context;
        this.device = device;
        this.listener = listener;
        context.registerReceiver(this, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            if (device.equals(d)) {
                Log.i(TAG, "Device was disconnected");
                listener.onDeviceDisconnected();
            }
        }
    }

    @Override
    public void close() {
        context.unregisterReceiver(this);
    }
}
