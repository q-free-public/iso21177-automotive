package com.qfree.its.iso21177poc.common.car;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.Accelerometer;
import androidx.car.app.hardware.info.CarHardwareLocation;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.CarSensors;
import androidx.car.app.hardware.info.Compass;
import androidx.car.app.hardware.info.EnergyLevel;
import androidx.car.app.hardware.info.EnergyProfile;
import androidx.car.app.hardware.info.Gyroscope;
import androidx.car.app.hardware.info.Mileage;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.hardware.info.Speed;

import com.qfree.its.iso21177poc.common.geoflow.EventHandler;

import java.util.Locale;

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
        try {
            carInfo.addEnergyLevelListener(carContext.getMainExecutor(), new OnCarDataAvailableListener<EnergyLevel>() {
                @Override
                public void onCarDataAvailable(@NonNull EnergyLevel data) {
    //                Log.d(TAG, "onCarDataAvailable: EnergyLevel " + data);
                    eventHandler.obtainMessage(EventHandler.CAR_ENERGY_LEVEL_DATA_EVENT_MSG, data).sendToTarget();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Not allowed for Play Store apps.
        try {
            carInfo.addMileageListener(carContext.getMainExecutor(), new OnCarDataAvailableListener<Mileage>() {
                @Override
                public void onCarDataAvailable(@NonNull Mileage data) {
//                    Log.d(TAG, "onCarDataAvailable: Mileage " + data);
                    eventHandler.obtainMessage(EventHandler.CAR_MILEAGE_DATA_EVENT_MSG, data).sendToTarget();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void monitorCarSensors(CarContext carContext, EventHandler eventHandler) {
        Log.d(TAG, "monitorCarSensors: ");
        CarHardwareManager carHardwareManager = carContext.getCarService(CarHardwareManager.class);
        CarSensors carSensors = carHardwareManager.getCarSensors();
        try {
            carSensors.addAccelerometerListener(CarSensors.UPDATE_RATE_NORMAL, carContext.getMainExecutor(), new OnCarDataAvailableListener<Accelerometer>() {
                @Override
                public void onCarDataAvailable(@NonNull Accelerometer data) {
                    eventHandler.obtainMessage(EventHandler.CAR_ACCELEROMETER_DATA_EVENT_MSG, data).sendToTarget();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            carSensors.addCarHardwareLocationListener(CarSensors.UPDATE_RATE_NORMAL, carContext.getMainExecutor(), new OnCarDataAvailableListener<CarHardwareLocation>() {
                @Override
                public void onCarDataAvailable(@NonNull CarHardwareLocation data) {
                    eventHandler.obtainMessage(EventHandler.CAR_HW_LOCATION_DATA_EVENT_MSG, data).sendToTarget();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            carSensors.addCompassListener(CarSensors.UPDATE_RATE_NORMAL, carContext.getMainExecutor(), new OnCarDataAvailableListener<Compass>() {
                @Override
                public void onCarDataAvailable(@NonNull Compass data) {
                    eventHandler.obtainMessage(EventHandler.CAR_COMPASS_DATA_EVENT_MSG, data).sendToTarget();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            carSensors.addGyroscopeListener(CarSensors.UPDATE_RATE_NORMAL, carContext.getMainExecutor(), new OnCarDataAvailableListener<Gyroscope>() {
                @Override
                public void onCarDataAvailable(@NonNull Gyroscope data) {
                    eventHandler.obtainMessage(EventHandler.CAR_GYRO_DATA_EVENT_MSG, data).sendToTarget();
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

    public static String carEnergyProfileToLogString(EnergyProfile data) {
        return String.format(Locale.ROOT, "EvConnectorTypes: %s, FuelTypes %s",
                data.getEvConnectorTypes().getValue(), data.getFuelTypes().getValue());
    }

    public static String carSpeedToLogString(Speed data) {
        return String.format(Locale.ROOT, "RawSpeedMps: %s, DisplaySpeedMps: %s, DisplayUnit: %s",
                data.getRawSpeedMetersPerSecond().getValue(), data.getDisplaySpeedMetersPerSecond().getValue(), data.getSpeedDisplayUnit().getValue());
    }

    public static String carEnergyLevelToLogString(EnergyLevel data) {
        return String.format(Locale.ROOT,
                "BatteryPercent: %s, FuelPercent: %s, EnergyIsLow: %s, RangeRemainingM: %s," +
                        "DistanceDisplayUnit: %s, FuelVolumeDisplayUnit: %s",
                data.getBatteryPercent().getValue(), data.getFuelPercent().getValue(), data.getEnergyIsLow().getValue(),
                data.getRangeRemainingMeters().getValue(), data.getDistanceDisplayUnit().getValue(),
                data.getFuelVolumeDisplayUnit().getValue());
    }

    public static String carMileageToLogString(Mileage data) {
        return String.format(Locale.ROOT,
                "OdoMeter: %s, DistanceMeters: %s",
                data.getOdometerMeters().getValue(), data.getDistanceDisplayUnit().getValue());
    }

    public static String carAccelerometerToLogString(Accelerometer data) {
        return String.format(Locale.ROOT,
                "Forces: %s",
                data.getForces().getValue());
    }

    public static String carCompassToLogString(Compass data) {
        return String.format(Locale.ROOT,
                "Orientations: %s",
                data.getOrientations().getValue());
    }

    public static String carGyroscopeToLogString(Gyroscope data) {
        return String.format(Locale.ROOT,
                "Rotations: %s",
                data.getRotations().getValue());
    }

    public static String carHwLocationToLogString(CarHardwareLocation data) {
        return String.format(Locale.ROOT,
                "Location: %s",
                data.getLocation().getValue());
    }
}
