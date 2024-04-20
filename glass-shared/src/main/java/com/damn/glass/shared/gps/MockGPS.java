package com.damn.glass.shared.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import android.util.Log;

// Requires MOCK_LOCATION permission on Android 6+
// On most devices it's enabled in Developer Options
// On Glass use adb shell to grant permission:
// "adb shell appops set com.damn.anotherglass.glass.ee android:mock_location allow"
public class MockGPS {

    private static final String LOG_TAG = "MockGPS";

    private final LocationManager locationManager;

    private boolean installed;

    public MockGPS(@NonNull Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void start() throws java.lang.SecurityException {
        if (null == locationManager) {
            return;
        }
        remove();
        locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
        );
        locationManager.setTestProviderEnabled(
                LocationManager.GPS_PROVIDER,
                true
        );
        locationManager.setTestProviderStatus(
                LocationManager.GPS_PROVIDER,
                LocationProvider.AVAILABLE,
                null,
                System.currentTimeMillis()
        );
        installed = true;
    }

    public void remove() {
        if (null == locationManager) {
            return;
        }
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        installed = false;
    }

    public void publish(@NonNull com.damn.anotherglass.shared.gps.Location location) {
        if (null == locationManager || !installed) {
            return;
        }
        try {
            Location location1 = new Location(LocationManager.GPS_PROVIDER);
            location1.setLatitude(location.latitude);
            location1.setLongitude(location.longitude);
            location1.setAccuracy(location.accuracy);
            location1.setAltitude(location.altitude);
            location1.setElapsedRealtimeNanos(SystemClock.elapsedRealtime() * 1000);
            location1.setSpeed(location.speed);
            location1.setBearing(location.bearing);
            location1.setTime(System.currentTimeMillis());
            Log.d(LOG_TAG, location1.toString());
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location1);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to post mock GPS", e);
        }
    }

    public boolean isInstalled() {
        return installed;
    }
}
