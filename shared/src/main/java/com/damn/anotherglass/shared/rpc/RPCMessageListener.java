package com.damn.anotherglass.shared.rpc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// not actually RPC, since it missing RPC (remote *function* calls) part
public interface RPCMessageListener {
    void onWaiting();

    void onConnectionStarted(@NonNull String device);

    void onDataReceived(@NonNull RPCMessage data);

    void onConnectionLost(@Nullable String error);

    void onShutdown();
}
