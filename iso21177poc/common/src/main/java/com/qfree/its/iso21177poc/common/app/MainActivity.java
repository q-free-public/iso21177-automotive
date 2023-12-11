package com.qfree.its.iso21177poc.common.app;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.qfree.its.iso21177poc.common.geoflow.GeoFlowService;
import com.qfree.its.iso21177poc.common.geoflow.InitServiceBroadcastReceiver;
import com.qfree.its.iso21177poc.common.geoflow.UiFields;
import com.qfree.its.iso21177poc.common.geoflow.UiUtils;
import com.qfree.its.iso21177poc.common.R;

public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ServiceConnection mServiceConnection;
    private boolean mBound = false;
    private GeoFlowService mGeoFlowService;
    private InitServiceBroadcastReceiver mInitServiceBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);

            //Register for boot events, so service can start on boot
            if (mInitServiceBroadcastReceiver == null) {
                mInitServiceBroadcastReceiver = new InitServiceBroadcastReceiver(this);
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
                intentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);
                intentFilter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
                intentFilter.addAction(InitServiceBroadcastReceiver.START_GEOFLOW_SERVICE_ACTION);
                intentFilter.addAction(Intent.ACTION_SHUTDOWN);
                intentFilter.addAction(Intent.ACTION_REBOOT);
                registerReceiver(mInitServiceBroadcastReceiver, intentFilter);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
        try {
            //Request location permissions
            if (checkPermissions(this)) {
                //Start service (should be called from broadcastReceiver, send custom on setup/installation)
                //Send custom intent to broadcastreceiver
                sendBroadcast(new Intent(this, InitServiceBroadcastReceiver.class).setAction(InitServiceBroadcastReceiver.START_GEOFLOW_SERVICE_ACTION));
                Intent requestLocationServiceIntent = mInitServiceBroadcastReceiver.getGeoFlowServiceIntent();
                Log.d(TAG, "onStart: " + requestLocationServiceIntent);
                if (requestLocationServiceIntent != null) {
                    //bind to service
                    mServiceConnection = getServiceConnection();
                    bindService(requestLocationServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection getServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                try {
                    Log.d(TAG, "onServiceConnected: " + className.getClassName());
                    GeoFlowService.RequestLocationBinder requestLocationBinder = (GeoFlowService.RequestLocationBinder) service;
                    mGeoFlowService = requestLocationBinder.getService();
                    mBound = true;
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                Log.d(TAG, "onServiceConnected: " + className.getClassName());
                mBound = false;
            }
        };
    }

    public boolean checkPermissions(Activity activity) throws Exception {
        Log.d(TAG, "checkPermissions: ");
        //From Android 11, must be done twice, first foreground location, then background
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkPermissions: foreground");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return false;
        }
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkPermissions: background");
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: ");
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            for (String permission : permissions) {
                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)
                        || permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    } else {
                        Toast.makeText(this, "Must allow all the time", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                    }
                } else if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "checkPermissions: background");
                        requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
                    } else {
                        Intent intent = new Intent(this, GeoFlowService.class);
                        startForegroundService(intent);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        try {
//            Log.d(TAG, "onSharedPreferenceChanged: " + s);
            String str = sharedPreferences.getString(s, null);
//            Log.d(TAG, "onSharedPreferenceChanged: " + " " +  s + " " +  str);
            UiFields uiFields = UiFields.retrieveFromPreference(sharedPreferences);
            UiUtils.updateUiFieldsMobile(this, uiFields);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

}