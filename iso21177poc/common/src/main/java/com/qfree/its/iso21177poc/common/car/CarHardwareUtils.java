package com.qfree.its.iso21177poc.common.car;

import android.util.Log;

import com.qfree.its.iso21177poc.common.geoflow.EventHandler;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.CarHardwareLocation;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.EnergyProfile;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.hardware.info.Speed;

/*Implements androidx.car.app. Currently not in use for CarInfo*/

public class CarHardwareUtils {
    private static final String TAG = CarHardwareUtils.class.getSimpleName();

    public static void monitorCarInfoSpeed(CarContext carContext, EventHandler eventHandler){
        try {
            CarHardwareManager carHardwareManager = carContext.getCarService(CarHardwareManager.class);
            CarInfo carInfo = carHardwareManager.getCarInfo();
            carInfo.addSpeedListener(carContext.getMainExecutor(), new OnCarDataAvailableListener<Speed>() {
                @Override
                public void onCarDataAvailable(@NonNull Speed data) {
                    //                Log.d(TAG, "onCarDataAvailable: Speed " + data);
                    eventHandler.obtainMessage(EventHandler.CAR_SPEED_DATA_EVENT_MSG, data).sendToTarget();
                }
            });
        } catch (Exception e) {
            eventHandler.obtainMessage(EventHandler.CAR_SPEED_DATA_EVENT_MSG, e.getMessage()).sendToTarget();
            e.printStackTrace();
        }
    }


    public static void monitorCarInfo(CarContext carContext, EventHandler eventHandler) {
        Log.d(TAG, "monitorCarHardware: ");
        CarHardwareManager carHardwareManager = carContext.getCarService(CarHardwareManager.class);
        CarInfo carInfo = carHardwareManager.getCarInfo();
        try {
            carInfo.fetchModel(carContext.getMainExecutor(), new OnCarDataAvailableListener<Model>() {
                @Override
                public void onCarDataAvailable(@NonNull Model data) {
                    Log.d(TAG, "onCarDataAvailable: Model " + data);
                    eventHandler.obtainMessage(EventHandler.CAR_MODEL_DATA_EVENT_MSG, data).sendToTarget();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            carInfo.fetchEnergyProfile(carContext.getMainExecutor(), new OnCarDataAvailableListener<EnergyProfile>() {
                @Override
                public void onCarDataAvailable(@NonNull EnergyProfile data) {
                    Log.d(TAG, "onCarDataAvailable: EnergyProfile " + data);
                    eventHandler.obtainMessage(EventHandler.CAR_ENERGY_PROFILE_DATA_EVENT_MSG, data).sendToTarget();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            carInfo.addSpeedListener(carContext.getMainExecutor(), new OnCarDataAvailableListener<Speed>() {
                @Override
                public void onCarDataAvailable(@NonNull Speed data) {
    //                Log.d(TAG, "onCarDataAvailable: Speed " + data);
                    eventHandler.obtainMessage(EventHandler.CAR_SPEED_DATA_EVENT_MSG, data).sendToTarget();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String carModelToLogString(Model data) {
        return String.format(Locale.ROOT, "%s;%s;%s",
                data.getManufacturer().getValue(), data.getName().getValue(), data.getYear().getValue());
    }

    public static String carSpeedToLogString(Speed data) {
        return String.format(Locale.ROOT, "RawSpeedMps: %s, DisplaySpeedMps: %s, DisplayUnit: %s",
                data.getRawSpeedMetersPerSecond().getValue(), data.getDisplaySpeedMetersPerSecond().getValue(), data.getSpeedDisplayUnit().getValue());
    }

    public static String carHwLocationToLogString(CarHardwareLocation data) {
        return String.format(Locale.ROOT,
                "Location: %s",
                data.getLocation().getValue());
    }
}
