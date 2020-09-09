package com.damn.anotherglass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.damn.anotherglass.shared.gps.GPSServiceAPI;
import com.damn.anotherglass.shared.RPCMessage;

@SuppressLint("MissingPermission")
public class GPSService implements LocationListener {

    private final GlassService service;
    private final LocationManager locationManager;

    public GPSService(final GlassService service) {
        this.service = service;
        locationManager = (LocationManager) service.getSystemService(Context.LOCATION_SERVICE);
    }

    public void start() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    public void stop() {
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
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
