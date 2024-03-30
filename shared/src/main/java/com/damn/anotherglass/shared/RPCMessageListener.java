package com.damn.anotherglass.shared;

public interface RPCMessageListener {
    void onWaiting();

    void onConnectionStarted(/*@NonNull*/ String device);

    void onDataReceived(/*@NonNull*/ RPCMessage data);

    void onConnectionLost(/*@Nullable*/ String error);
}
