package com.damn.anotherglass.shared.rpc;

import android.content.Context;

import androidx.annotation.CallSuper;

public interface IRPCHost {
    void start(Context context);
    void send(RPCMessage message);
    void stop();
    @CallSuper
    void onStopped();
}
