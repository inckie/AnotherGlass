package com.damn.anotherglass;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import static android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class Settings {

    private static final String sPreferencesName = "anotherglass";
    public static final String GPS_ENABLED = "gps_enabled";
    private final SharedPreferences mPreferences;

    public Settings(Context context) {
        mPreferences = context.getSharedPreferences(sPreferencesName, Context.MODE_PRIVATE);
    }

    public void registerListener(@NonNull OnSharedPreferenceChangeListener listener) {
        mPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterListener(@NonNull OnSharedPreferenceChangeListener listener) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public boolean isGPSEnabled() {
        return mPreferences.getBoolean(GPS_ENABLED, true);
    }

    public void setGPSEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(GPS_ENABLED, enabled).apply();
    }
}
