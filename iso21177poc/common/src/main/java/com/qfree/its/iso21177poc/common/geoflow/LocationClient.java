package com.qfree.its.iso21177poc.common.geoflow;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogEvents;


public class LocationClient {
    private static final String TAG = LocationClient.class.getSimpleName();
    private static final int LOCATION_UPDATE_MIN_INTERVAL_MILLIS = 500;
    private static final long LOCATION_UPDATE_FASTEST_INTERVAL_MILLIS = 100;
    private final Handler mEventHandler;
    private final LocationRequest mLocationRequest;
    private final LocationCallback mLocationCallback;
    private final FusedLocationProviderClient mFusedLocationProviderClient;
    private final Context mContext;

    public LocationClient(Context context, Handler eventHandler) {
        Log.d(TAG, "LocationClient: c'tor (enter)");
        FileLogger.setContext(context);
        FileLogger.logEvent(LogEvents.GPS_INIT, "LocationClient: c'tor (begin)");
        this.mContext = context;
        this.mEventHandler = eventHandler;
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context.getApplicationContext());
        mLocationCallback = getLocationCallback();
        mLocationRequest = createLocationRequest();
        stopLocationUpdates();
        FileLogger.logEvent(LogEvents.GPS_INIT, "LocationClient: c'tor (exit)");
        Log.d(TAG, "LocationClient: c'tor (exit)");
    }

    public void startLocationUpdates() throws Exception {
        Log.d(TAG, "LocationClient: startLocationUpdates (enter)");
        FileLogger.setContext(mContext);
        FileLogger.logEvent(LogEvents.GPS_INIT, "LocationClient: startLocationUpdates (enter)");
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
        {
            Log.d(TAG, "LocationClient: Permissions OK (FINE_LOCATION + COARSE_LOCATION) - requestLocationUpdates");
            FileLogger.logEvent(LogEvents.GPS_INIT, "LocationClient: Permissions OK (FINE_LOCATION + COARSE_LOCATION) - requestLocationUpdates");
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, mEventHandler.getLooper());
        } else {
            Log.d(TAG, "LocationClient: Permissions MISSING (FINE_LOCATION + COARSE_LOCATION");
            FileLogger.logEvent(LogEvents.GPS_INIT, "LocationClient: Permissions MISSING (FINE_LOCATION + COARSE_LOCATION)");
            throw new Exception("Must grant location permission!");
        }
    }

    public void stopLocationUpdates() {
        Log.d(TAG, "LocationClient: stopLocationUpdates");
        FileLogger.setContext(mContext);
        FileLogger.logEvent(LogEvents.GPS_INIT, "LocationClient: stopLocationUpdates");
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_MIN_INTERVAL_MILLIS);
        locationRequest.setFastestInterval(LOCATION_UPDATE_FASTEST_INTERVAL_MILLIS);
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private LocationCallback getLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
//                Log.d(TAG, "onLocationResult: " + locationResult.getLastLocation());
                Location lastLocation = locationResult.getLastLocation();
                mEventHandler.obtainMessage(EventHandler.LOCATION_UPDATE_EVENT_MSG, lastLocation).sendToTarget();
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
//                Log.d(TAG, "onLocationAvailability: " + locationAvailability);
                super.onLocationAvailability(locationAvailability);
            }
        };
    }
}
