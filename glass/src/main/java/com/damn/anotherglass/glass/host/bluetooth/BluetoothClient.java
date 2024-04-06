package com.damn.anotherglass.glass.host.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.damn.anotherglass.shared.Constants;
import com.damn.anotherglass.shared.rpc.IRPCClient;
import com.damn.anotherglass.shared.rpc.RPCHandler;
import com.damn.anotherglass.shared.rpc.RPCMessage;
import com.damn.anotherglass.shared.rpc.RPCMessageListener;
import com.damn.anotherglass.shared.utility.DisconnectReceiver;
import com.damn.anotherglass.shared.utility.Sleep;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothClient implements IRPCClient {

    private static final String TAG = "BluetoothClient";

    private volatile Connection mConnection; // should be atomic

    private class Connection extends Thread {

        private final Context mContext;

        private final BlockingQueue<RPCMessage> mQueue = new LinkedBlockingDeque<>();

        private final RPCHandler mHandler;

        private volatile boolean mConnected = false; // are we are actually connected

        public Connection(Context context, RPCMessageListener listener) {
            mContext = context;
            mHandler = new RPCHandler(listener);
        }

        @Override
        public void run() {
            try {
//                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    Log.e(TAG, "Missing permission, aborting the connection");
//                    return;
//                }
                BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
                Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
                if (null == pairedDevices || pairedDevices.isEmpty()) {
                    Log.e(TAG, "No paired devices found, aborting the connection");
                    return;
                }
                for (BluetoothDevice device : pairedDevices) {
                    // connect to the first phone we find
                    if (BluetoothClass.Device.PHONE_SMART == device.getBluetoothClass().getDeviceClass()) {
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
            if(!mQueue.offer(message))
                Log.e(TAG, "Failed to queue message");
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
                AtomicBoolean active = new AtomicBoolean(true);
                try (DisconnectReceiver ignored = new DisconnectReceiver(mContext, device, () -> active.getAndSet(false))) {
                    try (OutputStream outputStream = socket.getOutputStream();
                         InputStream inputStream = socket.getInputStream()) {
                        ObjectOutputStream os = new ObjectOutputStream(outputStream);
                        ObjectInputStream in = new ObjectInputStream(inputStream);
                        mConnected = true;
                        mHandler.onConnectionStarted(device.getName());
                        while (active.get()) {
                            while(null != mQueue.peek()){
                                RPCMessage message = mQueue.take();
                                os.writeObject(message);
                                Log.v(TAG, "Message " + message.service + "/" + message.type + " was sent");
                                if(null == message.service) {
                                    Log.d(TAG, "Shutdown requested");
                                    os.flush();
                                    return;
                                }
                            }
                            while (inputStream.available() > 0) {
                                RPCMessage objectReceived = (RPCMessage) in.readObject();
                                mHandler.onDataReceived(objectReceived);
                                Log.v(TAG, "Message " + objectReceived.service + "/" + objectReceived.type + " was received");
                            }
                            Sleep.sleep(100);
                        }
                    }
                }
            }
        }

        public boolean isConnected() {
            return mConnected;
        }
    }

    @Override
    public void start(Context context, RPCMessageListener listener) {
        if (null != mConnection) {
            Log.d(TAG, "Connection is already present");
            return;
        }
        mConnection = new Connection(context, listener);
        mConnection.start();
    }

    @Override
    public void send(@NonNull RPCMessage message) {
        Connection connection = mConnection;
        if (null == connection || !connection.isConnected()) {
            Log.d(TAG, "Connection is not active, message was not sent");
            return;
        }
        connection.send(message);
    }

    @Override
    public void stop() {
        Connection connection = mConnection;
        mConnection = null;
        if (null != connection) {
            connection.shutdown();
        }
    }

}
