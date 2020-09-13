package com.damn.anotherglass.glass.host;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = new Intent(StartActivity.this, HostService.class);
        startService(intent);
        finish();
    }
}
