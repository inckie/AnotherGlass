package com.damn.anotherglass.core;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.applicaster.xray.android.adapters.ALog;
import com.damn.anotherglass.shared.Constants;
import com.damn.anotherglass.shared.rpc.IRPCHost;
import com.damn.anotherglass.shared.rpc.IMessageSerializer;
import com.damn.anotherglass.shared.rpc.RPCHandler;
import com.damn.anotherglass.shared.rpc.RPCMessage;
import com.damn.anotherglass.shared.rpc.RPCMessageListener;
import com.damn.anotherglass.shared.rpc.SerializerProvider;
import com.damn.anotherglass.shared.utility.Closeables;
import com.damn.anotherglass.shared.utility.DisconnectReceiver;
import com.damn.anotherglass.shared.utility.Sleep;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;


public class BluetoothHost implements IRPCHost {

    private static final String NAME = "AnotherGlass";
    private static final String TAG = "GlassHostBt";

    private final RPCHandler mHandler;

    private volatile WorkerThread mWorkerThread;
    private volatile boolean mActive; // are we still need to run?

    public BluetoothHost(RPCMessageListener listener) {
        mHandler = new RPCHandler(listener);
    }

    @Override
    public void start(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            mHandler.onShutdown();
            return;
        }
        mActive = true;
        mWorkerThread = new WorkerThread(context);
        mWorkerThread.start();
    }

    @Override
    public void stop() {
        mActive = false;
        WorkerThread thread = this.mWorkerThread;
        if (null != thread) {
            mWorkerThread = null;
            thread.shutdown();
        }
    }

    @Override
    public void send(RPCMessage message) {
        if (!mActive) {
            ALog.e(TAG, "send() called when not active");
            return;
        }
        WorkerThread thread = this.mWorkerThread;
        if (null != thread) {
            thread.send(message);
        } else {
            ALog.e(TAG, "send() failed to queue message, no connection thread available");
        }
    }

    private class WorkerThread extends Thread {
        private final Context mContext;
        private BluetoothServerSocket serverSocket; // should use atomic reference, but it's not that critical

        private final BlockingQueue<RPCMessage> mQueue = new LinkedBlockingDeque<>();

        public WorkerThread(Context context) {
            mContext = context;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            final BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
            while (mActive) {
                // we recreate listening socket on each iteration, not perfect, but it's ok for now
                try {
                    serverSocket = bt.listenUsingInsecureRfcommWithServiceRecord(NAME, Constants.uuid);
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
                } catch (Exception e) {
                    ALog.e(TAG, "Exception in runLoop: " + e, e);
                    mHandler.onConnectionLost(e.getLocalizedMessage());
                } finally {
                    Closeables.close(serverSocket);
                    serverSocket = null;
                    mHandler.onConnectionLost(null); // will be called twice on error
                }
            }
            mWorkerThread = null;
            mActive = false;
            mHandler.onShutdown();
        }

        public void send(RPCMessage message) {
            mQueue.add(message);
        }

        public void shutdown() {
            mActive = false;
            try {
                BluetoothServerSocket socket = serverSocket;
                serverSocket = null;
                Closeables.close(socket);
                join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @SuppressLint("MissingPermission")
        private void runLoop(BluetoothSocket socket) throws Exception {
            final BluetoothDevice remoteDevice = socket.getRemoteDevice();
            ALog.d(TAG, "Connected to " + remoteDevice.getName());
            mHandler.onConnectionStarted(remoteDevice.getName());
            try (DisconnectReceiver ignored = new DisconnectReceiver(mContext, remoteDevice, this::onConnectionLost)) {

                try (InputStream inputStream = socket.getInputStream();
                     OutputStream outputStream = socket.getOutputStream()) {
                    IMessageSerializer serializer = SerializerProvider.getSerializer(inputStream, outputStream);
                    while (mActive) {
                        while (inputStream.available() > 0) {
                            RPCMessage objectReceived = serializer.readMessage();
                            if (null == objectReceived.service)
                                return; // shutdown requested
                            mHandler.onDataReceived(objectReceived);
                        }
                        while (null != mQueue.peek()) {
                            RPCMessage message = mQueue.take();
                            serializer.writeMessage(message);
                        }
                        Sleep.sleep(100);
                    }
                }
            }
        }

        private void onConnectionLost() {
            ALog.i(TAG, "Device was disconnected");
            mActive = false;
        }
    }

}