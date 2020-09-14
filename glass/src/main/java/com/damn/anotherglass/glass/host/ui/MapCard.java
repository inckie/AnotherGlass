package com.damn.anotherglass.glass.host.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.widget.CardBuilder;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Locale;

public class MapCard extends ICardViewProvider implements LocationListener {

    private static final String LIVE_CARD_TAG = "MapCard";
    private static final int sIMAGE_TIMEOUT = 5000;
    private static final int UPDATE_FREQ_MS = sIMAGE_TIMEOUT;

    private final LocationManager locationManager;
    private final Context mContext;

    private static abstract class TimedTarget implements Target {
        public final long timeStamp = System.currentTimeMillis();
    }

    private Bitmap mLastMap;
    private String mLastMapUrl = "";
    private TimedTarget mMapLoading;

    private Location mLocation;

    @SuppressLint("MissingPermission")
    public MapCard(@NonNull LiveCard card, Context context) {
        super(card);
        mContext = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        updateMapStatus();
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        checkMap();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void checkMap() {
        if(null != mMapLoading && mMapLoading.timeStamp + sIMAGE_TIMEOUT > System.currentTimeMillis())
            return;

        if(null == mLocation)
            return;

        final String mapUrl = getMapUrl(mLocation);
        if (mLastMapUrl.equals(mapUrl))
            return;

        mMapLoading = new TimedTarget() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                mLastMapUrl = mapUrl;
                mLastMap = bitmap;
                mMapLoading = null;
                onMapUpdated();
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                mMapLoading = null;
                Log.e(LIVE_CARD_TAG, "Failed to load map image " + mapUrl, e);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };
        Picasso.get().load(mapUrl).memoryPolicy(MemoryPolicy.NO_STORE).into(mMapLoading);
    }

    private void updateMapStatus() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if(null != pm && !pm.isScreenOn()) {
            return;
        }
        checkMap();
    }

    private void onMapUpdated() {
        if (mLiveCard == null || !mLiveCard.isPublished())
            return;
        mLiveCard.setViews(buildView());
    }

    private RemoteViews buildView() {
        CardBuilder cardBuilder = new CardBuilder(mContext.getApplicationContext(), CardBuilder.Layout.TEXT);
        if(null != mLastMap)
            cardBuilder.addImage(mLastMap);
        return cardBuilder.getRemoteViews();
    }

    private String getMapUrl(Location location) {
        return String.format(
                Locale.getDefault(),
                "https://static-maps.yandex.ru/1.x/?lang=en_US" +
                        "&size=640,360&z=15&l=map&pt=" +
                        "%f,%f" +
                        ",pm2rdl",
                location.getLongitude(), location.getLatitude());
    }

}
