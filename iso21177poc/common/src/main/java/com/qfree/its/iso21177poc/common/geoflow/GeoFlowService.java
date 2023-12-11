package com.qfree.its.iso21177poc.common.geoflow;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;

import com.qfree.its.iso21177poc.common.BuildConfig;
import com.qfree.geoflow.toll.api.GeoFlowUserRecord;
import com.qfree.geoflow.toll.api.GeoFlowVehicleRecord;
import com.qfree.geoflow.toll.api.HmiOption;
import com.qfree.its.iso21177poc.common.car.CarPropertyClient;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogEvents;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogFilePostThread;

public class GeoFlowService extends Service implements DefaultLifecycleObserver {
    private final static String TAG = GeoFlowService.class.getSimpleName();

    private SQLiteDatabase mSqLiteDatabase;
    private final IBinder binder = new RequestLocationBinder();
    private LocationClient mLocationClient;
    private EventHandler mEventHandler;
    private Looper mServiceLooper;
    private PackageManager mPackageManager;

    @Override
    public void onCreate() {
        Log.d(TAG, "GeoFlowService.onCreate: ");
        FileLogger.setContext(getApplicationContext());
        FileLogger.log("GeoFlowService.onCreate");
        try {
            if (BuildConfig.DEBUG) {
                StrictMode.enableDefaults();
                StrictMode.allowThreadDiskReads();
                StrictMode.allowThreadDiskWrites();
            }

            HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            mServiceLooper = thread.getLooper();

            //Init packageManager
            mPackageManager = getApplicationContext().getPackageManager();
            String appVersion = getAppVersion();

            //Init eventHandler
            mEventHandler = new EventHandler(getApplicationContext(), mServiceLooper);

            //Init locationClient
            mLocationClient = new LocationClient(getApplicationContext(), mEventHandler);
        } catch (Exception e) {
            try {
                if (mEventHandler != null)
                    mEventHandler.obtainMessage(EventHandler.EXCEPTION_EVENT_MSG, e).sendToTarget();
                if (mEventHandler == null) throw new Exception("mEventHandler == null", e);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent.getExtras().getString("intent"));
        FileLogger.log("GeoFlowService.onStartCommand: " + intent.getExtras().getString("intent"));
        try {
            //Notify user of foreground service
            Notification notification = GeoFlowServiceNotification.createNotificationChannel(getApplicationContext());
            startForeground(1, notification);

            FileLogger.logEvent(LogEvents.SYSTEM_START, TAG + " onStartCommand: " + intent.getExtras().getString("intent"));

            //Start location updates here
            if (mLocationClient != null) {
                mLocationClient.startLocationUpdates();
                FileLogger.log("GeoFlowService.onStartCommand - Call startLocationUpdates");
            } else {
                FileLogger.log("GeoFlowService.onStartCommand - mLocationClient is null");
                throw new Exception("mLocationClient is null");
            }
            Log.d(TAG, "onStartCommand: mLocationClient.startLocationUpdates completed");
            FileLogger.logEvent(LogEvents.GPS_INIT, "onStartCommand: mLocationClient.startLocationUpdates completed");

            //Listen on carPropertyClient
            if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
                CarPropertyClient.init(getApplicationContext(), mEventHandler);
                CarPropertyClient.getVehicleIdentificationNumber();
                CarPropertyClient.registerVehicleSpeedCallback();
            }
        } catch (Exception e) {
            try {
                FileLogger.logException(e);
                mEventHandler.obtainMessage(EventHandler.EXCEPTION_EVENT_MSG, e).sendToTarget();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
        return START_STICKY;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        try {
            FileLogger.logEvent(LogEvents.LIFECYCLE_INFO, TAG + " onTrimMemory level: " + level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        try {
            FileLogger.logEvent(LogEvents.LIFECYCLE_INFO, TAG + " onLowMemory");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        try {
            FileLogger.logEvent(LogEvents.LIFECYCLE_INFO, TAG + " onTaskRemoved: " + rootIntent.getAction());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        FileLogger.logEvent(LogEvents.LIFECYCLE_INFO, TAG + " onDestroy");
        try {
            //Stop location updates here
            if (mLocationClient != null) {
                mLocationClient.stopLocationUpdates();
            }
            FileLogger.logEvent(LogEvents.SYSTEM_STOP, TAG + " onDestroy");
            FileLogger.closeLogFile("GeoFlowService.onDestroy");
            if (mSqLiteDatabase != null){
                mSqLiteDatabase.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: " + intent);
        try {
            FileLogger.logEvent(LogEvents.HMI, HmiOption.ANDROID_AUTO.name());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {
            FileLogger.logEvent(LogEvents.HMI, HmiOption.NONE.name());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onUnbind(intent);
    }

    private String getAppVersion() {
        try {
            String version = String.valueOf(mPackageManager.getPackageInfo(this.getPackageName(), 0).getLongVersionCode());
            Log.d(TAG, "getAppVersion: " + version);
            return version;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public EventHandler getEventHandler() {
        return this.mEventHandler;
    }

    public class RequestLocationBinder extends Binder {
        public GeoFlowService getService() {
            return GeoFlowService.this;
        }
    }
}
