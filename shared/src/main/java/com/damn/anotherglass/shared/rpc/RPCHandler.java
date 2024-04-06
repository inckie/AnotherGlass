package com.damn.anotherglass.shared.rpc;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;


public class RPCHandler extends Handler implements RPCMessageListener {
    private final RPCMessageListener listener;

    private static final int STATE_CONNECTION_STARTED = 0;
    private static final int STATE_CONNECTION_LOST = 1;
    private static final int STATE_WAITING_FOR_CONNECT = 2;
    private static final int MSG_DATA_RECEIVED = 3;

    private static final String TAG = "RPCHandler";


    public RPCHandler(RPCMessageListener listener) {
        super(Looper.getMainLooper());
        this.listener = listener;
    }

    @Override
    public void handleMessage(Message msg) {
        if (STATE_CONNECTION_STARTED == msg.what) {
            final String device = msg.obj.toString();
            Log.d(TAG, "STATE_CONNECTION_STARTED: " + device);
            listener.onConnectionStarted(device);
        } else if (MSG_DATA_RECEIVED == msg.what) {
            final RPCMessage data = (RPCMessage) msg.obj;
            Log.d(TAG, "MSG_DATA_RECEIVED: " + data);
            listener.onDataReceived(data);
        } else if (STATE_CONNECTION_LOST == msg.what) {
            final String error = null != msg.obj ? msg.obj.toString() : null;
            Log.d(TAG, "STATE_CONNECTION_LOST: " + (null != error ? error : " no errors"));
            listener.onConnectionLost(error);
        } else if (STATE_WAITING_FOR_CONNECT == msg.what) {
            listener.onWaiting();
        }
    }

    @Override
    public void onWaiting() {
        obtainMessage(RPCHandler.STATE_WAITING_FOR_CONNECT).sendToTarget();
    }

    @Override
    public void onConnectionStarted(@NonNull String device) {
        obtainMessage(RPCHandler.STATE_CONNECTION_STARTED, device).sendToTarget();
    }

    @Override
    public void onDataReceived(@NonNull RPCMessage data) {
        obtainMessage(RPCHandler.MSG_DATA_RECEIVED, data).sendToTarget();
    }

    @Override
    public void onConnectionLost(/*@Nullable*/ String error) {
        obtainMessage(RPCHandler.STATE_CONNECTION_LOST, error).sendToTarget();
    }
}
