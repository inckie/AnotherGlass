package com.damn.anotherglass.core;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import static android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class Settings {

    private static final String sPreferencesName = "anotherglass";
    public static final String GPS_ENABLED = "gps_enabled";
    public static final String NOTIFICATIONS_ENABLED = "notifications_enabled";
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
        return mPreferences.getBoolean(GPS_ENABLED, false);
    }

    public void setGPSEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(GPS_ENABLED, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return mPreferences.getBoolean(NOTIFICATIONS_ENABLED, false);
    }

    public void setNotificationsEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(NOTIFICATIONS_ENABLED, enabled).apply();
    }
}
