package com.qfree.its.iso21177poc.common.car;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import com.qfree.its.iso21177poc.common.app.MapActivity;
import com.qfree.its.iso21177poc.common.geoflow.GeoFlowService;
import com.qfree.its.iso21177poc.common.geoflow.InitServiceBroadcastReceiver;
import com.qfree.its.iso21177poc.common.geoflow.PermissionUtils;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogEvents;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class GeoFlowCarAppService extends CarAppService implements DefaultLifecycleObserver {
    private static final String TAG = GeoFlowCarAppService.class.getSimpleName();
    InitServiceBroadcastReceiver mInitServiceBroadcastReceiver;
    private GeoFlowService mGeoFlowService;
    private ServiceConnection mServiceConnection;
    private boolean mBound;

    public GeoFlowCarAppService() {
        Log.d(TAG, "GeoFlowCarAppService() ");
        logLifecycle(TAG + " GeoFlowCarAppService constructor");
    }

    //TODO: Improve lifecycle startup sequence management: https://developer.android.com/training/cars/apps
    @Override
    public void onCreate() {
        super.onCreate();
        logLifecycle(TAG + " onCreate");
        if (mInitServiceBroadcastReceiver == null) {
            mInitServiceBroadcastReceiver = new InitServiceBroadcastReceiver(this);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
            intentFilter.addAction(InitServiceBroadcastReceiver.START_GEOFLOW_SERVICE_ACTION);
            registerReceiver(mInitServiceBroadcastReceiver, intentFilter);
        }
        sendBroadcast(new Intent(this, InitServiceBroadcastReceiver.class)
                .setAction(InitServiceBroadcastReceiver.START_GEOFLOW_SERVICE_ACTION));
        Intent requestLocationServiceIntent = mInitServiceBroadcastReceiver.getGeoFlowServiceIntent();
        Log.d(TAG, "onStart: " + requestLocationServiceIntent);
        if (requestLocationServiceIntent != null) {
            //bind to service
            mServiceConnection = getServiceConnection();
            bindService(requestLocationServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    //TODO: Check if createHostValidator is ok
    @NonNull
    @Override
    public HostValidator createHostValidator() {
        logLifecycle(TAG + " createHostValidator");
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull
    @Override
    public Session onCreateSession() {
        Session session = null;
        try {
            logLifecycle(TAG + " onCreateSession");
            session = new Session() {
                @NonNull
                @Override
                public Lifecycle getLifecycle() {
                    Lifecycle lifecycle =super.getLifecycle();
                    lifecycle.addObserver(getDefaultLifecycleObserverSession());
                    return super.getLifecycle();
                }

                @NonNull
                @Override
                public Screen onCreateScreen(@NonNull Intent intent) {
                    logLifecycle(TAG + " onCreateScreen");
                    // Default screen
//                  getCarContext().getCarService(ScreenManager.class).push(new TollInfoScreen(getCarContext()));

                    if (true) {
                        FileLogger.log("Launch MapActivity from PARK");
                        Intent launchMapActivityIntent = new Intent(getCarContext(), MapActivity.class);
                        launchMapActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
                        getCarContext().startActivity(launchMapActivityIntent);
                    }

                    //Request permissions
                    FileLogger.logEvent(LogEvents.GPS_INIT, "GeoFlowCarAppService: CheckPermissions ACCESS_BACKGROUND_LOCATION");
                    if (ActivityCompat.checkSelfPermission(getCarContext(),
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        FileLogger.logEvent(LogEvents.GPS_INIT, "GeoFlowCarAppService: CheckPermissions ACCESS_BACKGROUND_LOCATION - Error Not granted");
                        return new RequestPermissionScreen(getCarContext(), new PermissionUtils.PermissionCheckCallback() {
                            @Override
                            public void onPermissionGranted() {
                                Log.d(TAG, "onPermissionGranted: ");
                                FileLogger.logEvent(LogEvents.GPS_INIT, "GeoFlowCarAppService: onPermissionsGranted ACCESS_BACKGROUND_LOCATION");
                                sendBroadcast(new Intent(getCarContext(), InitServiceBroadcastReceiver.class)
                                        .setAction(InitServiceBroadcastReceiver.START_GEOFLOW_SERVICE_ACTION));
                                Intent requestLocationServiceIntent = mInitServiceBroadcastReceiver.getGeoFlowServiceIntent();
                                if (requestLocationServiceIntent != null) {
                                    //bind to service
                                    mServiceConnection = getServiceConnection();
                                    bindService(requestLocationServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                                }
                            }
                        });
                    }
                    FileLogger.logEvent(LogEvents.GPS_INIT, "GeoFlowCarAppService: ChcckPermissions ACCESS_BACKGROUND_LOCATION - Success Granted");
                    getCarContext().getCarService(ScreenManager.class).push(new TollInfoScreen(getCarContext()));
                    return new InfoScreen(getCarContext());
//                    return new TollInfoScreen(getCarContext());
                }
            };
            session.getLifecycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return session;
    }

    private LifecycleObserver getDefaultLifecycleObserverSession() {
        return new DefaultLifecycleObserver() {
            @Override
            public void onCreate(@NonNull LifecycleOwner owner) {
                logLifecycle(TAG + " session onCreate");
                DefaultLifecycleObserver.super.onCreate(owner);
            }

            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                logLifecycle(TAG + " session onStart");
                DefaultLifecycleObserver.super.onStart(owner);
            }

            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                logLifecycle(TAG + " session onResume");
                DefaultLifecycleObserver.super.onResume(owner);
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                logLifecycle(TAG + " session onPause");
                DefaultLifecycleObserver.super.onPause(owner);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                logLifecycle(TAG + " session onStop");
                DefaultLifecycleObserver.super.onStop(owner);
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                logLifecycle(TAG + " session onDestroy");
                DefaultLifecycleObserver.super.onDestroy(owner);
            }
        };
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        logLifecycle(TAG + " onStart");
        DefaultLifecycleObserver.super.onStart(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        logLifecycle(TAG + " onResume");
        DefaultLifecycleObserver.super.onResume(owner);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        logLifecycle(TAG + " onPause");
        DefaultLifecycleObserver.super.onPause(owner);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        logLifecycle(TAG + " onStop");
        DefaultLifecycleObserver.super.onStop(owner);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logLifecycle(TAG + " onDestroy");
        if (mServiceConnection != null){
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    private ServiceConnection getServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                logLifecycle(TAG + " onServiceConnected");
                try {
                    Log.d(TAG, "onServiceConnected: " + className.getClassName());
                    GeoFlowService.RequestLocationBinder requestLocationBinder = (GeoFlowService.RequestLocationBinder) service;
                    mGeoFlowService = requestLocationBinder.getService();
                    mBound = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                Log.d(TAG, "onServiceConnected: " + className.getClassName());
                try {
                    logLifecycle(TAG + " onServiceDisconnected");
                    mBound = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void logLifecycle(String event){
        try {
            Log.d(TAG, "logLifecycle: " + event);
            if (mBound) {
                FileLogger.logEvent(LogEvents.LIFECYCLE_INFO, "logLifecycle: " + event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
