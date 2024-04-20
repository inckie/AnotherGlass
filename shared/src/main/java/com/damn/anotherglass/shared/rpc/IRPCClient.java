package com.damn.anotherglass.shared.rpc;

import android.content.Context;

import androidx.annotation.NonNull;

public interface IRPCClient {
    void start(Context context, RPCMessageListener listener);

    void send(@NonNull RPCMessage message);

    void stop();
}
