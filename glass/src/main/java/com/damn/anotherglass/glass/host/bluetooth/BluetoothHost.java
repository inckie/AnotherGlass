package com.damn.anotherglass.glass.host.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.damn.anotherglass.glass.host.utility.Closeables;
import com.damn.anotherglass.glass.host.utility.Sleep;
import com.damn.anotherglass.shared.Constants;
import com.damn.anotherglass.shared.RPCMessage;
import com.damn.anotherglass.shared.utility.DisconnectReceiver;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;


public abstract class BluetoothHost {

    private static final int STATE_CONNECTION_STARTED = 0;
    private static final int STATE_CONNECTION_LOST = 1;
    private static final int STATE_WAITING_FOR_CONNECT = 2;
    private static final int MSG_DATA_RECEIVED = 3;

    private static final String NAME = "AnotherGlass";
    private static final String TAG = "GlassHost";

    private final Context mContext;
    private final BluetoothAdapter mBT;
    private final Handler mHandler;

    private volatile WorkerThread mWorkerThread;
    private volatile boolean mActive; // are we still need to run?

    // abstract to avoid creating Listener interface, just override these in-place
    public abstract void onWaiting();

    public abstract void onConnectionStarted(@NonNull String device);

    public abstract void onDataReceived(@NonNull RPCMessage data);

    public abstract void onConnectionLost(@Nullable String error);

    public BluetoothHost(Context context) {
        mContext = context;
        mBT = BluetoothAdapter.getDefaultAdapter();
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (STATE_CONNECTION_STARTED == msg.what) {
                    final String device = msg.obj.toString();
                    Log.d(TAG, "STATE_CONNECTION_STARTED: " + device);
                    onConnectionStarted(device);
                } else if (MSG_DATA_RECEIVED == msg.what) {
                    final RPCMessage data = (RPCMessage) msg.obj;
                    Log.d(TAG, "MSG_DATA_RECEIVED: " + data);
                    onDataReceived(data);
                } else if (STATE_CONNECTION_LOST == msg.what) {
                    final String error = null != msg.obj ? msg.obj.toString() : null;
                    Log.d(TAG, "STATE_CONNECTION_LOST: " + (null != error ? error : " no errors"));
                    onConnectionLost(error);
                } else if (STATE_WAITING_FOR_CONNECT == msg.what) {
                    onWaiting();
                }
            }
        };
    }

    public void start() {
        mActive = true;
        mWorkerThread = new WorkerThread();
        mWorkerThread.start();
    }

    public void stop() {
        mActive = false;
        WorkerThread thread = this.mWorkerThread;
        if (null != thread) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.mWorkerThread = null;
        }
    }

    private class WorkerThread extends Thread {

        public void run() {
            String error = null;
            while (mActive) {
                BluetoothServerSocket serverSocket;
                try {
                    serverSocket = mBT.listenUsingInsecureRfcommWithServiceRecord(NAME, Constants.uuid);
                } catch (IOException e) {
                    error = e.getLocalizedMessage();
                    break;
                }

                mHandler.obtainMessage(STATE_WAITING_FOR_CONNECT).sendToTarget();

                try (BluetoothSocket socket = serverSocket.accept()) {
                    Closeables.close(serverSocket);
                    serverSocket = null;
                    if (socket == null)
                        break;
                    runLoop(socket);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    error = e.getLocalizedMessage();
                    break; // quit on any error for now
                } finally {
                    Closeables.close(serverSocket);
                }
            }
            mWorkerThread = null;
            mActive = false;
            mHandler.obtainMessage(STATE_CONNECTION_LOST, error).sendToTarget();
        }

        private void runLoop(BluetoothSocket socket) throws IOException, ClassNotFoundException {
            Log.d(TAG, "create ConnectedDevice");
            final BluetoothDevice remoteDevice = socket.getRemoteDevice();
            mHandler.obtainMessage(STATE_CONNECTION_STARTED, remoteDevice.getName()).sendToTarget();
            try (DisconnectReceiver ignored = new DisconnectReceiver(mContext, remoteDevice, this::onConnectionLost)) {
                try (InputStream inputStream = socket.getInputStream();
                     OutputStream outputStream = socket.getOutputStream()) {
                    ObjectInputStream in = new ObjectInputStream(inputStream);
                    while (mActive) {
                        while (inputStream.available() > 0) {
                            RPCMessage objectReceived = (RPCMessage) in.readObject();
                            mHandler.obtainMessage(MSG_DATA_RECEIVED, objectReceived).sendToTarget();
                            outputStream.write("OK\n".getBytes());
                        }
                        Sleep.sleep(100);
                    }
                }
            }
        }

        private void onConnectionLost() {
            Log.i(TAG, "Device was disconnected");
            mActive = false;
        }
    }

}