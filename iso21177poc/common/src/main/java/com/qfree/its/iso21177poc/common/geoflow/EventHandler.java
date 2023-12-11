package com.qfree.its.iso21177poc.common.geoflow;


import android.car.hardware.CarPropertyValue;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.info.Accelerometer;
import androidx.car.app.hardware.info.CarHardwareLocation;
import androidx.car.app.hardware.info.Compass;
import androidx.car.app.hardware.info.EnergyLevel;
import androidx.car.app.hardware.info.EnergyProfile;
import androidx.car.app.hardware.info.Gyroscope;
import androidx.car.app.hardware.info.Mileage;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.hardware.info.Speed;
import androidx.preference.PreferenceManager;

import com.qfree.geoflow.privatevault.NoTollContextException;
import com.qfree.geoflow.privatevault.NoZoneException;
import com.qfree.geoflow.toll.api.GeoFlowInvoiceSummary;
import com.qfree.geoflow.toll.api.GeoFlowTollingState;
import com.qfree.geoflow.toll.api.GeoFlowUserRecord;
import com.qfree.geoflow.toll.api.GeoFlowUtils;
import com.qfree.geoflow.toll.api.GeoFlowVehicleRecord;
import com.qfree.geoflow.toll.api.GeoFlowWsApiEventTollCostUpdate;
import com.qfree.geoflow.toll.api.GeoFlowZonePackage;
import com.qfree.its.iso21177poc.common.app.MapActivity;
import com.qfree.its.iso21177poc.common.app.TripSummary;
import com.qfree.its.iso21177poc.common.car.CarHardwareUtils;
import com.qfree.its.iso21177poc.common.car.CarPropertyGear;
import com.qfree.its.iso21177poc.common.car.CarPropertyIgnitionState;
import com.qfree.its.iso21177poc.common.car.CarPropertyUtils;
import com.qfree.its.iso21177poc.common.geoflow.thick_client.BillingPeriod;
import com.qfree.its.iso21177poc.common.geoflow.thick_client.CostTableImpl;
import com.qfree.its.iso21177poc.common.geoflow.thick_client.GeoFlowTollingUtils;
import com.qfree.its.iso21177poc.common.geoflow.thick_client.PrivateVaultImpl;
import com.qfree.its.iso21177poc.common.geoflow.thick_client.ThickClientPostEvent;
import com.qfree.its.iso21177poc.common.geoflow.thick_client.TollZoneGetEvent;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogEvents;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogFilePostEvent;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogFilePostThread;
import com.qfree.its.location.Position;
import com.qfree.nvdb.service.NvdbGeoflowZone;

import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class EventHandler extends Handler {
    private static final String TAG = EventHandler.class.getSimpleName();

    //Topics:
    public static final int LOCATION_UPDATE_EVENT_MSG = 100;
    public static final int FILE_UPLOAD_EVENT_MSG = 200;
    public static final int ZONE_RULES_DOWNLOAD_EVENT_MSG = 300;
    public static final int EXCEPTION_EVENT_MSG = 400;
    public static final int CAR_MODEL_DATA_EVENT_MSG = 500;
    public static final int CAR_ENERGY_PROFILE_DATA_EVENT_MSG = 501;
    public static final int CAR_SPEED_DATA_EVENT_MSG = 502;
    public static final int CAR_ENERGY_LEVEL_DATA_EVENT_MSG = 503;
    public static final int CAR_MILEAGE_DATA_EVENT_MSG = 504;
    public static final int CAR_GYRO_DATA_EVENT_MSG = 600;
    public static final int CAR_COMPASS_DATA_EVENT_MSG = 601;
    public static final int CAR_HW_LOCATION_DATA_EVENT_MSG = 602;
    public static final int CAR_ACCELEROMETER_DATA_EVENT_MSG = 603;
    public static final int ACCOUNT_CREATED_EVENT_MSG = 700;
    public static final int CAR_PROPERTY_IGNITION_STATE_EVENT_MSG = 800;
    public static final int CAR_PROPERTY_GEAR_SELECTION_EVENT_MSG = 801;
    public static final int CAR_PROPERTY_VIN_INFO_EVENT_MSG = 802;
    public static final int CAR_PROPERTY_VEHICLE_SPEED_EVENT_MSG = 803;
    public static final int CAR_PROPERTY_ODOMETER_EVENT_MSG = 804;
    public static final int THICK_CLIENT_UPLOAD_EVENT_MSG = 900;

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;
    private final UiFields mUiFields;
    private final PrivateVaultImpl mPrivateVault;
    private final CostTableImpl mCostTable;
    private final GeoFlowTollingState mTollingStateTotalTrip;
    private final GeoFlowSQLiteDb mDbHelper;
    private BillingPeriod mBillingPeriod;
    private QfreePosImpl mPrevPosition;
    private final GeoFlowTollingState mTollingStateVaultBatch;
    private long posCnt = 0;

    private static ArrayList<NvdbGeoflowZone> mGeoFlowZones;
    private static TripSummary mTripSummary;
    private static TripSummary mTripZero;
    private CarPropertyGear mPrevGearPosition = null;

    public EventHandler(Context context, Looper looper,
                        GeoFlowSQLiteDb dbHelper,
                        PrivateVaultImpl privateVault, CostTableImpl costTable) {
        super(looper);
        this.mContext = context;
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mUiFields = UiFields.retrieveFromPreference(this.mSharedPreferences);
        this.mDbHelper = dbHelper;
        this.mPrivateVault = privateVault;
        this.mCostTable = costTable;
        this.mTollingStateTotalTrip = new GeoFlowTollingState();
        this.mTollingStateVaultBatch = new GeoFlowTollingState();
        try {
            BillingPeriod.parse(Config.INVOICE_INTERVAL);
            this.mBillingPeriod = new BillingPeriod(LocalDateTime.now());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mTripSummary = new TripSummary();
        mGeoFlowZones = costTable.getGeoflowZones();
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

                    try {
                        this.mPrivateVault.append(null, currentPosition);
                    } catch (Exception e) {
                        FileLogger.logException(e);
                        e.printStackTrace();
                    }

                    if (this.mBillingPeriod.periodEnd.isBefore(LocalDateTime.now())) {
                        this.mPrivateVault.flushItems(null);
                        GeoFlowInvoiceSummary invoiceSummary = GeoFlowTollingUtils.createInvoiceSummary(this.mDbHelper, this.mBillingPeriod,
                                this.mPrivateVault);
                        GeoFlowTollingUtils.submitToServer(Config.SERVER_URL+Config.SERVLET_FILEPATH_USER_UPDATE,
                                Config.HTTP_INVOICE_UPLOAD_EVENT, invoiceSummary, this, 5000);
                        this.mUiFields.setBillingPeriod(this.mBillingPeriod);
                        this.mUiFields.setInvoiceSummary(invoiceSummary);
                        this.mBillingPeriod = new BillingPeriod(LocalDateTime.now());

                        //Post thin_client_log file, but not the file created on installation
                        if (!FileLogger.getVehicleIdStr().equals("INIT")){
                            FileLogger.closeLogFile("End of billing period");
                            LogFilePostThread logFilePostThread = new LogFilePostThread(
                                    Config.SERVER_URL + Config.SERVLET_FILEPATH_UPLOAD_LOG_FILE,
                                    this, 5000);
                            logFilePostThread.start();
                        }
                    }

                    if (this.mCostTable != null && this.mPrevPosition != null) {
                        GeoFlowWsApiEventTollCostUpdate tollCostUpdate = calculateToll(mPrevPosition, currentPosition);
                        this.mUiFields.setTollCostUpdate(tollCostUpdate);

                        //Update trip cost
                        mTripSummary.setTripCost(tollCostUpdate.cost);
                        mTripSummary.setCurrency(tollCostUpdate.costCurrency);
                        mTripSummary.setTripDistance(tollCostUpdate.distanceKm);
                    }
                    //Run thin client logger
                    FileLogger.logEvent(LogEvents.GPS, currentPosition);
                    mPrevPosition = currentPosition;
                }
            } else if (msg.what == ZONE_RULES_DOWNLOAD_EVENT_MSG) {
                if (msg.obj instanceof TollZoneGetEvent) {
                    TollZoneGetEvent tollZoneGetEvent = (TollZoneGetEvent) msg.obj;
                    if (tollZoneGetEvent.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        this.mCostTable.updateFromWebAndroid(tollZoneGetEvent.getBody());
                        GeoFlowZonePackage pkg = tollZoneGetEvent.getPkg();
                        FileLogger.logEvent(LogEvents.DOWNLOAD_SUCCESS, String.format(Locale.ROOT,
                                "nvdbDownload: %s, latestChange: %s, nvdbSource: %s, zoneCnt: %d",
                                pkg.nvdbDownload.toString(), pkg.latestChange.toString(), pkg.nvdbSource, mCostTable.getGeoflowZones().size()));
                        mUiFields.setLogFileDownloadOkIncrement();
                    } else {
                        mUiFields.setLogFileDownloadErrorIncrement("Toll zones");
                        FileLogger.logEvent(LogEvents.DOWNLOAD_FAILED, tollZoneGetEvent.getBody().replaceAll("[\r\n]+ [ \t]*", ";") + " code: " + tollZoneGetEvent.getResponseCode());
                    }
                    mGeoFlowZones = this.mCostTable.getGeoflowZones();
                }
            } else if (msg.what == FILE_UPLOAD_EVENT_MSG) {
                if (msg.obj instanceof LogFilePostEvent) {
                    LogFilePostEvent logFilePostEvent = (LogFilePostEvent) msg.obj;
                    int responseCode = logFilePostEvent.getResponseCode();
                    String uploadTopic = logFilePostEvent.getUploadTopic();
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                        FileLogger.deleteUploadedFile(uploadTopic);
                        FileLogger.logEvent(LogEvents.UPLOAD_SUCCESS, uploadTopic);
                        FileLogger.zipAndPostLogFile(Config.SERVER_URL + Config.SERVLET_FILEPATH_UPLOAD_LOG_FILE, this);
                        mUiFields.setLogFileUploadOkIncrement();
                    } else {
                        FileLogger.logEvent(LogEvents.UPLOAD_FAILED, uploadTopic + " code=" + responseCode);
                        mUiFields.setLogFileUploadErrorIncrement("Data upload");
                    }
                    Log.d(TAG, "handleMessage: what=" + msg.what + "  Resp=" + responseCode +  "  Cnt=" + mUiFields.getLogFileUploadErrorCount() + "/" + mUiFields.getLogFileUploadOkCount());
                    mUiFields.setLogFileUploadEvent(logFilePostEvent);
                }
            } else if (msg.what == ACCOUNT_CREATED_EVENT_MSG) {
                if (msg.obj instanceof GeoFlowAccountRecord) {
                    GeoFlowAccountRecord geoFlowAccountRecord = (GeoFlowAccountRecord) msg.obj;
                    GeoFlowUserRecord user = geoFlowAccountRecord.getGeoFlowUserRecord();
                    GeoFlowVehicleRecord vehicle = geoFlowAccountRecord.getGeoFlowVehicleRecord();
                    this.mDbHelper.saveUserRecord(geoFlowAccountRecord.getGeoFlowUserRecord());
                    this.mDbHelper.saveVehicleRecord(geoFlowAccountRecord.getGeoFlowVehicleRecord());
                    FileLogger.logEvent(LogEvents.GEOFLOW_ACCOUNT_CREATED, String.format(Locale.ROOT,
                            "%s;%s", user.name, vehicle.licensePlate));
                    FileLogger.logEvent(LogEvents.PUBLIC_KEY_ENCRYPTION, String.format(Locale.ROOT,
                            "%s", GeoFlowUtils.bin2hex(user.publicKeyForEncryption)));
                    FileLogger.logEvent(LogEvents.PUBLIC_KEY_SIGNING, String.format(Locale.ROOT,
                            "%s", GeoFlowUtils.bin2hex(user.publicKeyForSigning)));
                    FileLogger.setUser(user);
                    FileLogger.setVehicle(vehicle);
                    FileLogger.setVehicleIdStr(vehicle.licensePlate);
                    try {
                        mPrivateVault.initialize(null, user, vehicle);
                    } catch (Exception e) {
                        FileLogger.logException(e);
                        e.printStackTrace();
                    }
                    FileLogger.closeLogFile("Account created");
                    this.mUiFields.setGeoFlowAccount(geoFlowAccountRecord);
                    //TODO: Submit to server
                    GeoFlowAccountUtils.setRegistered(mSharedPreferences, true);
                    GeoFlowAccountRecord.clearAccountInfoFromPreference(mSharedPreferences);
                    GeoFlowAccountUtils.submitToServer(Config.SERVER_URL + Config.SERVLET_FILEPATH_USER_UPDATE,
                            Config.HTTP_USER_UPLOAD_EVENT, Config.HTTP_VEHICLE_UPLOAD_EVENT, user, vehicle,
                            this, 5000);
                }
            } else if (msg.what == THICK_CLIENT_UPLOAD_EVENT_MSG) {
                if (msg.obj instanceof ThickClientPostEvent) {
                    ThickClientPostEvent postEvent = (ThickClientPostEvent) msg.obj;
                    int respCode = postEvent.getResponseCode();
                    if (respCode == HttpURLConnection.HTTP_ACCEPTED || respCode == HttpURLConnection.HTTP_OK) {
                        if (postEvent.getUploadTopic().contains(Config.HTTP_USER_UPLOAD_EVENT)) {

                        } else if (postEvent.getUploadTopic().contains(Config.HTTP_VEHICLE_UPLOAD_EVENT)) {

                        } else if (postEvent.getUploadTopic().contains(Config.HTTP_INVOICE_UPLOAD_EVENT)) {

                        }
                        FileLogger.logEvent(LogEvents.UPLOAD_SUCCESS, postEvent.getUploadTopic());
                        mUiFields.setLogFileUploadOkIncrement();
                    } else {
                        FileLogger.logEvent(LogEvents.UPLOAD_FAILED, String.format(Locale.ROOT, "%s;%s", postEvent.getUploadTopic(), postEvent.getError()));
                        mUiFields.setLogFileUploadErrorIncrement("User/Vehicle/Invoice upload");
                    }
                }
            } else if (msg.what == CAR_MODEL_DATA_EVENT_MSG) {
                if (msg.obj instanceof Model) {
                    Model model = (Model) msg.obj;
                    Log.d(TAG, "handleMessage: model" + model.getName().getValue());
                    FileLogger.logEvent(LogEvents.CAR_MODEL, CarHardwareUtils.carModelToLogString(model));
                    this.mUiFields.setCarModel(model);
                }
            } else if (msg.what == CAR_ENERGY_PROFILE_DATA_EVENT_MSG) {
                if (msg.obj instanceof EnergyProfile) {
                    EnergyProfile energyProfile = (EnergyProfile) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_ENERGY_PROFILE, CarHardwareUtils.carEnergyProfileToLogString(energyProfile));
                    this.mUiFields.setCarEnergyProfile(energyProfile);
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
            } else if (msg.what == CAR_ENERGY_LEVEL_DATA_EVENT_MSG) {
                if (msg.obj instanceof EnergyLevel) {
                    EnergyLevel energyLevel = (EnergyLevel) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_ENERGY_LEVEL, CarHardwareUtils.carEnergyLevelToLogString(energyLevel));
                    this.mUiFields.setCarEnergyLevel(energyLevel);
                }
            } else if (msg.what == CAR_MILEAGE_DATA_EVENT_MSG) {
                if (msg.obj instanceof Mileage) {
                    Mileage mileage = (Mileage) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_MILEAGE, CarHardwareUtils.carMileageToLogString(mileage));
                    this.mUiFields.setCarMileage(mileage);
                }
            } else if (msg.what == CAR_ACCELEROMETER_DATA_EVENT_MSG) {
                if (msg.obj instanceof Accelerometer) {
                    Accelerometer accelerometer = (Accelerometer) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_ACCEL, CarHardwareUtils.carAccelerometerToLogString(accelerometer));
                }
            } else if (msg.what == CAR_COMPASS_DATA_EVENT_MSG) {
                if (msg.obj instanceof Compass) {
                    Compass compass = (Compass) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_COMPASS, CarHardwareUtils.carCompassToLogString(compass));
                }
            } else if (msg.what == CAR_GYRO_DATA_EVENT_MSG) {
                if (msg.obj instanceof Gyroscope) {
                    Gyroscope gyroscope = (Gyroscope) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_GYRO, CarHardwareUtils.carGyroscopeToLogString(gyroscope));
                }
            } else if (msg.what == CAR_HW_LOCATION_DATA_EVENT_MSG) {
                if (msg.obj instanceof CarHardwareLocation) {
                    CarHardwareLocation hwLocation = (CarHardwareLocation) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_HW_LOCATION, CarHardwareUtils.carHwLocationToLogString(hwLocation));
                }
            } else if (msg.what == CAR_PROPERTY_GEAR_SELECTION_EVENT_MSG) {
                if (msg.obj instanceof CarPropertyValue){
                    CarPropertyValue carPropertyValue = (CarPropertyValue) msg.obj;
                    CarPropertyGear gear = CarPropertyUtils.mapGear((Integer) carPropertyValue.getValue());
                    FileLogger.logEvent(LogEvents.GEAR_SELECTION, String.format(Locale.ROOT,
                            "%s;%s", carPropertyValue.getValue(), gear.getShortName()));
                    this.mUiFields.setCarPropertyGear(gear);
                    if (gear.equals(CarPropertyGear.DRIVE) && mPrevGearPosition != CarPropertyGear.DRIVE) {
                        FileLogger.log("Gear moved to DRIVE");
                    }
                    if (gear.equals(CarPropertyGear.PARK) && mPrevGearPosition != CarPropertyGear.PARK) {
                        if (!MapActivity.isActive){
                            FileLogger.log("Launch MapActivity from PARK");
                            Intent launchMapActivityIntent = new Intent(this.mContext, MapActivity.class);
                            launchMapActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
                            this.mContext.startActivity(launchMapActivityIntent);
                        }

                        //Post thin_client_log file, but not the file created on installation
                        if (!FileLogger.getVehicleIdStr().equals("INIT")){
                            FileLogger.closeLogFile("Gear moved to PARK");
                            LogFilePostThread logFilePostThread = new LogFilePostThread(
                                    Config.SERVER_URL + Config.SERVLET_FILEPATH_UPLOAD_LOG_FILE,
                                    this, 5000);
                            logFilePostThread.start();
                        }
                    }
                    mPrevGearPosition = gear;
                } else if (msg.obj instanceof String){
                    FileLogger.logEvent(LogEvents.GEAR_SELECTION, msg.obj);
                }
            } else if (msg.what == CAR_PROPERTY_IGNITION_STATE_EVENT_MSG) {
                if (msg.obj instanceof CarPropertyValue){
                    CarPropertyValue carPropertyValue = (CarPropertyValue) msg.obj;
                    CarPropertyIgnitionState ignitionState = CarPropertyUtils.mapIgnitionState((Integer) carPropertyValue.getValue());
                    FileLogger.logEvent(LogEvents.IGNITION_STATE, String.format(Locale.ROOT,
                            "%s;%s", carPropertyValue.getValue(), ignitionState.getShortName()));
                    this.mUiFields.setCarPropertyIgnitionState(ignitionState);
                } else if (msg.obj instanceof String){
                    FileLogger.logEvent(LogEvents.IGNITION_STATE, msg.obj);
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
            } else if (msg.what == CAR_PROPERTY_ODOMETER_EVENT_MSG) {
                if (msg.obj instanceof CarPropertyValue) {
                    CarPropertyValue<Object> carPropertyValue = (CarPropertyValue) msg.obj;
                    FileLogger.logEvent(LogEvents.CAR_MILEAGE, String.format(Locale.ROOT,
                            "%s", carPropertyValue.getValue()));
                    CarValue odoValue = new CarValue(carPropertyValue.getValue(), new Date().getTime(), carPropertyValue.getStatus());
                    Mileage mileage = new Mileage.Builder().setOdometerMeters(odoValue).build();
                    this.mUiFields.setCarMileage(mileage);
                } else if (msg.obj instanceof String){
                    FileLogger.logEvent(LogEvents.CAR_MILEAGE, msg.obj);
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

    private GeoFlowWsApiEventTollCostUpdate calculateToll(Position prevPos, Position currPos) {
        if (prevPos == null) {
            return null;
        }
        if (currPos == null) {
            return null;
        }
        String curZoneName = "No Zone";
        double curCostPrKm = 0.0;
        String curCurrency = null;
        try {
            curCostPrKm = this.mCostTable.lookupCostPrKm(currPos, LocalDateTime.now());
            curCurrency = this.mCostTable.lookupCurrency(currPos);
            double deltaDistance = prevPos.calcDistance(currPos);
            double deltaCost = deltaDistance * curCostPrKm / 1000.0;
            curZoneName = this.mCostTable.lookupZoneName(currPos);

            this.mTollingStateVaultBatch.accumulatedDistance += deltaDistance;
            this.mTollingStateVaultBatch.accumulatedCost += deltaCost;
            this.mTollingStateVaultBatch.currency = curCurrency;
            this.mTollingStateTotalTrip.accumulatedDistance += deltaDistance;
            this.mTollingStateTotalTrip.accumulatedCost += deltaCost;
            this.mTollingStateTotalTrip.currency = curCurrency;
        } catch (NoZoneException e1) {
            Log.d(TAG, "No zone");
        } catch (NoTollContextException e2) {
            Log.d(TAG, "No toll context");
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.mPrivateVault.updateState(this.mTollingStateVaultBatch);

        return new GeoFlowWsApiEventTollCostUpdate(curZoneName, this.mTollingStateTotalTrip.accumulatedDistance / 1000.0,
                this.mTollingStateTotalTrip.accumulatedCost, curCostPrKm, this.mTollingStateTotalTrip.currency);
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
