package com.damn.anotherglass;

import android.app.Application;

import com.applicaster.xray.android.sinks.ADBSink;
import com.applicaster.xray.core.Core;
import com.applicaster.xray.ui.sinks.InMemoryLogSink;

public class App extends Application {

    public static final String memory_sink_name = "memory_sink";

    @Override
    public void onCreate() {
        super.onCreate();
        Core.get()
                .addSink("adb", new ADBSink())
                .addSink(memory_sink_name, new InMemoryLogSink());
    }
}
