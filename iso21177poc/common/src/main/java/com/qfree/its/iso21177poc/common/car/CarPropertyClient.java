package com.qfree.its.iso21177poc.common.car;

import android.car.Car;
import android.car.VehicleAreaType;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.util.Log;

import com.qfree.its.iso21177poc.common.geoflow.EventHandler;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;

public class CarPropertyClient {
    private static final String TAG = CarPropertyClient.class.getSimpleName();

    private static CarPropertyManager mCarPropertyManager;
    private static EventHandler       mEventHandler;
    private static CarPropertyManager.CarPropertyEventCallback gearSelectionEventCallback;
    private static CarPropertyManager.CarPropertyEventCallback ignitionStatEventCallback;
    private static CarPropertyManager.CarPropertyEventCallback vehicleSpeedEventCallback;
    private static CarPropertyManager.CarPropertyEventCallback odometerEventCallback;

    public static void init(Context context, EventHandler eventHandler) {
        FileLogger.log("CarPropertyClient.init: create callbacks");
        if (gearSelectionEventCallback != null && mCarPropertyManager != null) {
            FileLogger.log("CarPropertyClient.init: callbacks was initialized - doing unregisterCallback!!");
            mCarPropertyManager.unregisterCallback(gearSelectionEventCallback);
            mCarPropertyManager.unregisterCallback(ignitionStatEventCallback);
            mCarPropertyManager.unregisterCallback(vehicleSpeedEventCallback);
            mCarPropertyManager.unregisterCallback(odometerEventCallback);

            gearSelectionEventCallback = null;
            ignitionStatEventCallback = null;
            vehicleSpeedEventCallback = null;
            odometerEventCallback = null;
        }

        Car car = Car.createCar(context);
        mCarPropertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
        mEventHandler = eventHandler;

        if (gearSelectionEventCallback == null) {
            gearSelectionEventCallback = new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue carPropertyValue) {
                    mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_GEAR_SELECTION_EVENT_MSG, carPropertyValue).sendToTarget();
                }

                @Override
                public void onErrorEvent(int i, int i1) {
                    mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_GEAR_SELECTION_EVENT_MSG, "onErrorEvent").sendToTarget();
                }
            };
        }

        if (ignitionStatEventCallback == null) {
            ignitionStatEventCallback = new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue carPropertyValue) {
                    mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_IGNITION_STATE_EVENT_MSG, carPropertyValue).sendToTarget();
                }

                @Override
                public void onErrorEvent(int i, int i1) {
                    mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_IGNITION_STATE_EVENT_MSG, "onErrorEvent").sendToTarget();
                }
            };
        }

        if (vehicleSpeedEventCallback == null) {
            vehicleSpeedEventCallback = new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue carPropertyValue) {
                    mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_VEHICLE_SPEED_EVENT_MSG, carPropertyValue).sendToTarget();
                }

                @Override
                public void onErrorEvent(int i, int i1) {
                    mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_VEHICLE_SPEED_EVENT_MSG, "onErrorEvent").sendToTarget();
                }
            };
        }

        if (odometerEventCallback == null) {
            odometerEventCallback = new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue carPropertyValue) {
                    mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_ODOMETER_EVENT_MSG, carPropertyValue).sendToTarget();
                }

                @Override
                public void onErrorEvent(int i, int i1) {
                    mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_ODOMETER_EVENT_MSG, "onErrorEvent").sendToTarget();
                }
            };
        }
    }

    public static void getVehicleIdentificationNumber() {
        try {
            CarPropertyValue<Object> vinProp = mCarPropertyManager.getProperty(VehiclePropertyIds.INFO_VIN, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
            Log.d(TAG, "CarPropertyClient: VIN : " + vinProp);
            FileLogger.log("getVehicleIdentificationNumber: Success: " + vinProp);
            mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_VIN_INFO_EVENT_MSG, vinProp).sendToTarget();
        } catch (Exception e) {
            FileLogger.log("getVehicleIdentificationNumber: Exception: " + e.getClass().getName() + ": " + e.getMessage());
            mEventHandler.obtainMessage(EventHandler.EXCEPTION_EVENT_MSG, e).sendToTarget();
            Log.d(TAG, "CarPropertyClient: VIN Error");
            e.printStackTrace();
        }

        try {
            Log.d(TAG, "VIN Register callback  id = " + VehiclePropertyIds.INFO_VIN);
            CarPropertyManager.CarPropertyEventCallback vinEventCallback = new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue vinProp) {
                    Log.d(TAG, "VIN.onChangeEvent: VIN " + vinProp);
                    mEventHandler.obtainMessage(EventHandler.CAR_PROPERTY_VIN_INFO_EVENT_MSG, vinProp).sendToTarget();
                }

                @Override
                public void onErrorEvent(int i, int i1) {
                    Log.d(TAG, "VIN.onErrorEvent: " + i + " " + i1);
                }
            };
            mCarPropertyManager.registerCallback(vinEventCallback, VehiclePropertyIds.INFO_VIN, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
        } catch (Exception e) {
            Log.d(TAG, "VIN.Exception: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            // mEventHandler.obtainMessage(EventHandler.EXCEPTION_EVENT_MSG, e).sendToTarget();
            e.printStackTrace();
        }
    }

    public static void registerGearSelectionCallback(){
        try {
            mCarPropertyManager.registerCallback(gearSelectionEventCallback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_NORMAL);
        } catch (Exception e) {
            mEventHandler.obtainMessage(EventHandler.EXCEPTION_EVENT_MSG, e).sendToTarget();
            e.printStackTrace();
        }
    }

    public static void registerIgnitionStateCallback(){
        try {
            mCarPropertyManager.registerCallback(ignitionStatEventCallback, VehiclePropertyIds.IGNITION_STATE, CarPropertyManager.SENSOR_RATE_NORMAL);
        } catch (Exception e) {
            mEventHandler.obtainMessage(EventHandler.EXCEPTION_EVENT_MSG, e).sendToTarget();
            e.printStackTrace();
        }
    }

    public static void registerVehicleSpeedCallback() {
        try {
            mCarPropertyManager.registerCallback(vehicleSpeedEventCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL);
        } catch (Exception e) {
            mEventHandler.obtainMessage(EventHandler.EXCEPTION_EVENT_MSG, e).sendToTarget();
            e.printStackTrace();
        }
    }

    public static void registerOdometerCallback(){
        try {
            mCarPropertyManager.registerCallback(odometerEventCallback, VehiclePropertyIds.PERF_ODOMETER, CarPropertyManager.SENSOR_RATE_NORMAL);
        } catch (Exception e) {
            mEventHandler.obtainMessage(EventHandler.EXCEPTION_EVENT_MSG, e).sendToTarget();
            e.printStackTrace();
        }
    }

/*
    TODO: Implement
    public void unregister(){
        this.mCarPropertyManager.unregisterCallback();
    }
*/
}
