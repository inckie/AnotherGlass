package com.damn.anotherglass.core;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.applicaster.xray.android.adapters.ALog;
import com.damn.anotherglass.shared.Constants;
import com.damn.anotherglass.shared.RPCHandler;
import com.damn.anotherglass.shared.RPCMessage;
import com.damn.anotherglass.shared.RPCMessageListener;
import com.damn.anotherglass.shared.utility.Closeables;
import com.damn.anotherglass.shared.utility.DisconnectReceiver;
import com.damn.anotherglass.shared.utility.Sleep;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;


public abstract class BluetoothHost {

    private static final String NAME = "AnotherGlass";
    private static final String TAG = "GlassHost";

    private final Context mContext;
    private final BluetoothAdapter mBT;
    private final RPCHandler mHandler;

    private final BlockingQueue<RPCMessage> mQueue = new LinkedBlockingDeque<>();

    private volatile WorkerThread mWorkerThread;
    private volatile boolean mActive; // are we still need to run?

    // abstract to avoid creating Listener interface, just override these in-place


    public BluetoothHost(Context context, RPCMessageListener listener) {
        mContext = context;
        mBT = BluetoothAdapter.getDefaultAdapter();
        mHandler = new RPCHandler(listener);
    }

    public void start() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            onStopped();
            return;
        }
        mActive = true;
        mWorkerThread = new WorkerThread();
        mWorkerThread.start();
    }

    public void stop() {
        mActive = false;
        WorkerThread thread = this.mWorkerThread;
        if (null != thread) {
            thread.shutdown();
            this.mWorkerThread = null;
        }
        onStopped();
    }

    public abstract void onStopped();

    public void send(RPCMessage message) {
        if (!mActive) {
            ALog.e(TAG, "send() called when not active");
            return;
        }
        if(!mQueue.offer(message)){
            ALog.e(TAG, "send() failed to queue message");
        }
    }

    private class WorkerThread extends Thread {
        BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public void run() {
            while (mActive) {
                try {
                    serverSocket = mBT.listenUsingInsecureRfcommWithServiceRecord(NAME, Constants.uuid);
                } catch (IOException e) {
                    String error = e.getLocalizedMessage();
                    mHandler.onConnectionLost(error);
                    break;
                }

                mHandler.onWaiting();

                try (BluetoothSocket socket = serverSocket.accept()) {
                    Closeables.close(serverSocket);
                    serverSocket = null;
                    if (socket == null)
                        continue;
                    runLoop(socket);
                } catch (IOException | ClassNotFoundException | InterruptedException e) {
                    Log.e(TAG, "Exception in runLoop: " + e, e);
                    String error = e.getLocalizedMessage();
                    mHandler.onConnectionLost(error);
                } finally {
                    Closeables.close(serverSocket);
                }
                mHandler.onConnectionLost(null);
            }
            mWorkerThread = null;
            mActive = false;
            mHandler.post(BluetoothHost.this::onStopped);
        }

        public void shutdown() {
            mActive = false;
            try {
                if (null != serverSocket) {
                    serverSocket.close();
                    serverSocket = null;
                }
                join();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressLint("MissingPermission")
        private void runLoop(BluetoothSocket socket) throws IOException, ClassNotFoundException, InterruptedException {
            ALog.d(TAG, "create ConnectedDevice");
            final BluetoothDevice remoteDevice = socket.getRemoteDevice();
            mHandler.onConnectionStarted(remoteDevice.getName());
            try (DisconnectReceiver ignored = new DisconnectReceiver(mContext, remoteDevice, this::onConnectionLost)) {
                try (InputStream inputStream = socket.getInputStream();
                     OutputStream outputStream = socket.getOutputStream()) {
                    ObjectInputStream in = new ObjectInputStream(inputStream);
                    ObjectOutputStream os = new ObjectOutputStream(outputStream);
                    while (mActive) {
                        while (inputStream.available() > 0) {
                            RPCMessage objectReceived = (RPCMessage) in.readObject();
                            if (null == objectReceived.service)
                                return; // shutdown
                            mHandler.onDataReceived(objectReceived);
                        }
                        while (null != mQueue.peek()) {
                            RPCMessage message = mQueue.take();
                            os.writeObject(message);
                        }
                        Sleep.sleep(100);
                    }
                }
            } finally {
                mHandler.onConnectionLost(null);
            }
        }

        private void onConnectionLost() {
            ALog.i(TAG, "Device was disconnected");
            mActive = false;
        }
    }

}