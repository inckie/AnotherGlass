package com.damn.anotherglass.extensions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.applicaster.xray.core.Logger;
import com.damn.anotherglass.core.GlassService;
import com.damn.anotherglass.logging.ALog;
import com.damn.anotherglass.shared.RPCMessage;
import com.damn.anotherglass.shared.gps.GPSServiceAPI;

@SuppressLint("MissingPermission")
public class GPSExtension implements LocationListener {

    private static final String TAG = "GPSExtension";

    private final GlassService service;
    private final LocationManager locationManager;
    private final ALog log = new ALog(Logger.get(TAG));

    public GPSExtension(final GlassService service) {
        this.service = service;
        locationManager = (LocationManager) service.getSystemService(Context.LOCATION_SERVICE);
    }

    public void start() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        log.i(TAG, "GPS extension started");
    }

    public void stop() {
        locationManager.removeUpdates(this);
        log.i(TAG, "GPS extension stopped");
    }

    @Override
    public void onLocationChanged(Location location) {
        log.d(TAG, "GPS extension received location update");
        com.damn.anotherglass.shared.gps.Location loc = new com.damn.anotherglass.shared.gps.Location();
        loc.accuracy = location.getAccuracy();
        loc.latitude = location.getLatitude();
        loc.longitude = location.getLongitude();
        loc.altitude = location.getAltitude();
        loc.bearing = location.getBearing();
        loc.speed = location.getSpeed();
        RPCMessage rpcMessage = new RPCMessage(GPSServiceAPI.ID, loc);
        service.send(rpcMessage);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
