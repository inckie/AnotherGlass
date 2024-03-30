package com.damn.anotherglass.glass.host.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.damn.anotherglass.shared.Constants;
import com.damn.anotherglass.shared.RPCHandler;
import com.damn.anotherglass.shared.RPCMessage;
import com.damn.anotherglass.shared.RPCMessageListener;
import com.damn.anotherglass.shared.utility.DisconnectReceiver;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class BluetoothClient {

    private static final String TAG = "BluetoothClient";

    private Connection mConnection;

    private class Connection extends Thread {

        private final Context mContext;

        private final BlockingQueue<RPCMessage> mQueue = new LinkedBlockingDeque<>();

        private boolean mConnected = false; // are we are actually connected

        private final RPCHandler mHandler;

        public Connection(Context context, RPCMessageListener listener) {
            mContext = context;
            mHandler = new RPCHandler(listener);
        }

        @Override
        public void run() {
            try {
                BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
//                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    Log.e(TAG, "Missing permission, aborting the connection");
//                    return;
//                }
                Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
                if (null == pairedDevices || pairedDevices.isEmpty()) {
                    Log.e(TAG, "No paired devices found, aborting the connection");
                    return;
                }
                for (BluetoothDevice device : pairedDevices) {
                    final String deviceName = device.getName();
                    if (deviceName.contains("S10")) {
                        runLoop(device);
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Connection exception", e);
            } finally {
                Log.i(TAG, "Client has stopped");
                mConnected = false;
                mConnection = null;
                mHandler.onConnectionLost("Connection was shut down");
            }
        }

        public void send(@NonNull RPCMessage message) {
            mQueue.offer(message);
        }

        public void shutdown() {
            Log.i(TAG, "Connection shutdown requested");
            // send empty message to notify host we are shutting down
            mQueue.add(new RPCMessage(null, null));
            try {
                join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Connection was shut down");
        }

        @SuppressLint("MissingPermission")
        private void runLoop(@NonNull BluetoothDevice device) throws IOException, InterruptedException, ClassNotFoundException {
            try (BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(Constants.uuid)) {
                socket.connect();
                Log.i(TAG, "Client has connected to " + device.getName());
                try (DisconnectReceiver ignored = new DisconnectReceiver(mContext, device, this::shutdown)) {
                    try (OutputStream outputStream = socket.getOutputStream();
                         InputStream inputStream = socket.getInputStream()) {
                        ObjectOutputStream os = new ObjectOutputStream(outputStream);
                        ObjectInputStream in = new ObjectInputStream(inputStream);
                        mConnected = true;
                        mHandler.onConnectionStarted(device.getName());
                        while (true) {
                            while(null != mQueue.peek()){
                                RPCMessage message = mQueue.take();
                                os.writeObject(message);
                                Log.v(TAG, "Message " + message.service + "/" + message.type + " was sent");
                                if(null == message.service) {
                                    os.flush();
                                    return; // shutdown
                                }
                            }
                            while (inputStream.available() > 0) {
                                RPCMessage objectReceived = (RPCMessage) in.readObject();
                                mHandler.onDataReceived(objectReceived);
                                Log.v(TAG, "Message " + objectReceived.service + "/" + objectReceived.type + " was received");
                            }
                        }
                    }
                }
            }
        }

        public boolean isConnected() {
            return mConnected;
        }

    }

    public void start(Context context, RPCMessageListener listener) {
        if (null != mConnection) {
            return;
        }
        mConnection = new Connection(context, listener);
        mConnection.start();
    }

    public void send(@NonNull RPCMessage message) {
        Connection connection = mConnection;
        if (null == connection || !connection.isConnected()) {
            return;
        }
        connection.send(message);
    }

    public void stop() {
        Connection connection = mConnection;
        mConnection = null;
        if (null != connection) {
            connection.shutdown();
        }
    }

}
