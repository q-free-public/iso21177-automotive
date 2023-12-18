package com.qfree.its.iso21177poc.common.geoflow;


import android.car.hardware.CarPropertyValue;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.qfree.geoflow.toll.api.GeoFlowTollingState;
import com.qfree.its.iso21177poc.common.app.TripSummary;
import com.qfree.its.iso21177poc.common.car.CarHardwareUtils;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogEvents;
import com.qfree.nvdb.service.NvdbGeoflowZone;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.info.CarHardwareLocation;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.hardware.info.Speed;
import androidx.preference.PreferenceManager;

public class EventHandler extends Handler {
    private static final String TAG = EventHandler.class.getSimpleName();

    //Topics:
    public static final int LOCATION_UPDATE_EVENT_MSG = 100;
    public static final int FILE_UPLOAD_EVENT_MSG = 200;
    public static final int EXCEPTION_EVENT_MSG = 400;
    public static final int CAR_MODEL_DATA_EVENT_MSG = 500;
    public static final int CAR_ENERGY_PROFILE_DATA_EVENT_MSG = 501;
    public static final int CAR_SPEED_DATA_EVENT_MSG = 502;
    public static final int CAR_HW_LOCATION_DATA_EVENT_MSG = 602;
    public static final int CAR_PROPERTY_VIN_INFO_EVENT_MSG = 802;
    public static final int CAR_PROPERTY_VEHICLE_SPEED_EVENT_MSG = 803;

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;
    private final UiFields mUiFields;
    private final GeoFlowTollingState mTollingStateTotalTrip;
    private QfreePosImpl mPrevPosition;
    private final GeoFlowTollingState mTollingStateVaultBatch;
    private long posCnt = 0;

    private static ArrayList<NvdbGeoflowZone> mGeoFlowZones;
    private static TripSummary mTripSummary;
    private static TripSummary mTripZero;

    public EventHandler(Context context, Looper looper) {
        super(looper);
        this.mContext = context;
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mUiFields = UiFields.retrieveFromPreference(this.mSharedPreferences);
        this.mTollingStateTotalTrip = new GeoFlowTollingState();
        this.mTollingStateVaultBatch = new GeoFlowTollingState();
        mTripSummary = new TripSummary();
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        try {
            super.handleMessage(msg);
            if (msg.what == LOCATION_UPDATE_EVENT_MSG) {
                if (msg.obj instanceof Location) {
                    Location location = (Location) msg.obj;
                    QfreePosImpl currentPosition = LocationUtils.androidLocationToQfreePos(location);
                    posCnt++;
                    this.mUiFields.setPosition(currentPosition, posCnt);

                    //Save pos to trip
                    mTripSummary.addPosition(currentPosition);

                    //Run thin client logger
                    FileLogger.logEvent(LogEvents.GPS, currentPosition);
                    mPrevPosition = currentPosition;
                }
            } else if (msg.what == CAR_MODEL_DATA_EVENT_MSG) {
                if (msg.obj instanceof Model) {
                    Model model = (Model) msg.obj;
                    Log.d(TAG, "handleMessage: model" + model.getName().getValue());
                    FileLogger.logEvent(LogEvents.CAR_MODEL, CarHardwareUtils.carModelToLogString(model));
                    this.mUiFields.setCarModel(model);
                }
            } else if (msg.what == CAR_SPEED_DATA_EVENT_MSG) {
                //TODO: only log on one sec interval. Done for CAR_PROPERTY_VEHICLE_SPEED.
                if (msg.obj instanceof Speed) {
                    Speed speed = (Speed) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_SPEED, CarHardwareUtils.carSpeedToLogString(speed));
                    this.mUiFields.setCarSpeed(speed);
                } else if (msg.obj instanceof String){
                    FileLogger.logEvent(LogEvents.CAR_SPEED, msg.obj);
                }
            } else if (msg.what == CAR_HW_LOCATION_DATA_EVENT_MSG) {
                if (msg.obj instanceof CarHardwareLocation) {
                    CarHardwareLocation hwLocation = (CarHardwareLocation) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_HW_LOCATION, CarHardwareUtils.carHwLocationToLogString(hwLocation));
                }
            } else if (msg.what == CAR_PROPERTY_VIN_INFO_EVENT_MSG) {
                if (msg.obj instanceof CarPropertyValue){
                    CarPropertyValue<String> carPropertyValue = (CarPropertyValue) msg.obj;
                    FileLogger.logEvent(LogEvents.VIN, carPropertyValue.getValue().toString());
                    this.mUiFields.setVin(carPropertyValue.getValue().toString());
                } else {
                    FileLogger.logEvent(LogEvents.VIN, msg.obj);
                }
            } else if (msg.what == CAR_PROPERTY_VEHICLE_SPEED_EVENT_MSG) {
                if (msg.obj instanceof CarPropertyValue) {
                    CarPropertyValue<Object> carPropertyValue = (CarPropertyValue) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_SPEED, String.format(Locale.ROOT,
                            "%s", carPropertyValue.getValue()));
                    CarValue speedValue = new CarValue(carPropertyValue.getValue(), new Date().getTime(), carPropertyValue.getStatus());
                    Speed speed = new Speed.Builder().setRawSpeedMetersPerSecond(speedValue).build();
                    this.mUiFields.setCarSpeed(speed);
                } else if (msg.obj instanceof String){
                    FileLogger.logEvent(LogEvents.CAR_SPEED, msg.obj);
                }
            } else if (msg.what == EXCEPTION_EVENT_MSG) {
                FileLogger.logException((Exception)msg.obj);
            }
            this.mUiFields.saveAsPreference(mSharedPreferences);
        } catch (Exception e) {
            try {
                FileLogger.logException(e);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    public static ArrayList<NvdbGeoflowZone> getGeoFlowZones() {
        return mGeoFlowZones;
    }

    public static TripSummary getTripSummary() {
        return mTripSummary.minus(mTripZero);
    }

    public static void clearTripSummary() {
        mTripZero = mTripSummary.clone();
        mTripSummary.clearRoute();
    }
}
