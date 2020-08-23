package com.damn.anotherglass;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;
import android.util.Log;

import com.damn.shared.Constants;
import com.damn.shared.RPCMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class BluetoothClient {

    private static final String TAG = "BluetoothClient";

    // part of the BT name, should be picked from list
    private static final String GLASS_BT_NAME_MARKER = "Glass";

    private Connection mConnection;

    private class Connection extends Thread {

        private final BlockingQueue<RPCMessage> mQueue = new LinkedBlockingDeque<>();

        private boolean mActive = true;

        @Override
        public void run() {
            try {
                BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
                Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
                if(null == pairedDevices)
                    return;
                for (BluetoothDevice device : pairedDevices) {
                    final String deviceName = device.getName();
                    if (deviceName.contains(GLASS_BT_NAME_MARKER))
                        runLoop(device);
                }
            } catch (Exception e) {
                Log.e(TAG, "Connection exception", e);
            }
            finally {
                mConnection = null;
                onStopped();
            }
        }

        public void send(@NonNull RPCMessage message) {
            mQueue.offer(message);
        }

        public void shutdown() {
            Log.i(TAG, "Connection shutdown requested");
            mActive = false;
            try {
                mQueue.add(new RPCMessage(null, null)); // send empty message to stop the thread
                join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Connection was shut down");
        }

        private void runLoop(@NonNull BluetoothDevice device) throws IOException, InterruptedException {
            try (BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(Constants.uuid)) {
                socket.connect();
                try (OutputStream outputStream = socket.getOutputStream();
                     InputStream inputStream = socket.getInputStream()) {

                    ObjectOutputStream os = new ObjectOutputStream(outputStream);
                    Scanner scanner = new Scanner(inputStream);

                    while (mActive) {
                        RPCMessage message = mQueue.take();
                        if(null == message.service)
                            return; // shutdown

                        os.writeObject(message);
                        if(scanner.hasNext()){
                            String response = scanner.nextLine();
                            Log.d(TAG, "Got response" + response);
                        }
                    }
                }
            }
        }
    }

    public void onStopped() {

    }

    public void start() {
        if(null != mConnection){
            return;
        }
        mConnection = new Connection();
        mConnection.start();
    }

    public void send(@NonNull RPCMessage message) {
        Connection connection = mConnection;
        if(null == connection) {
            return;
        }
        connection.send(message);
    }

    public void stop() {
        Connection connection = mConnection;
        mConnection = null;
        if(null != connection) {
            connection.shutdown();
        }
    }
}
