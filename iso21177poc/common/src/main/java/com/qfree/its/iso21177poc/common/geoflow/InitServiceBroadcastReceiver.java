package com.qfree.its.iso21177poc.common.geoflow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;

public class InitServiceBroadcastReceiver extends BroadcastReceiver {
    public static final String START_GEOFLOW_SERVICE_ACTION = "com.qfree.its.iso21177poc.START_LOCATION_ACTION";
    private static final String TAG = InitServiceBroadcastReceiver.class.getSimpleName();

    Intent mGeoFlowServiceIntent;
    private String action;

    public InitServiceBroadcastReceiver() {

    }

    public InitServiceBroadcastReceiver(Context context) {
        if (this.mGeoFlowServiceIntent == null) {
            this.mGeoFlowServiceIntent = new Intent(context, GeoFlowService.class);
        }
    }

    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());
        FileLogger.setContext(context);
        FileLogger.log("InitServiceBroadcastReceiver.onReceive: " + intent.getAction());
        try {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
                    || intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)
                    || intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)
                    || intent.getAction().equals(START_GEOFLOW_SERVICE_ACTION)
                    || intent.getAction().equals(Intent.ACTION_DREAMING_STOPPED)
                    || intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                //Start GeoFlowService
                if (this.mGeoFlowServiceIntent == null) {
                    this.mGeoFlowServiceIntent = new Intent(context, GeoFlowService.class);
                }
                this.mGeoFlowServiceIntent.putExtra("intent", intent.getAction());
                context.startForegroundService(mGeoFlowServiceIntent);
                //TODO: Log system start and system stop
            } else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)
                    || intent.getAction().equals(Intent.ACTION_REBOOT)
                    || intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (this.mGeoFlowServiceIntent != null){
                    this.mGeoFlowServiceIntent.putExtra("intent", action);
//                    context.stopService(mGeoFlowServiceIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Intent getGeoFlowServiceIntent() {
        return mGeoFlowServiceIntent;
    }

}
