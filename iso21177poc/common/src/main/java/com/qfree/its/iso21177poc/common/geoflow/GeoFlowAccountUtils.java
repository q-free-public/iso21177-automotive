package com.qfree.its.iso21177poc.common.geoflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.Model;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.qfree.geoflow.toll.api.GeoFlowDatabaseObject;
import com.qfree.geoflow.toll.api.GeoFlowFactory;
import com.qfree.geoflow.toll.api.GeoFlowInvoiceSummary;
import com.qfree.geoflow.toll.api.GeoFlowUserRecord;
import com.qfree.geoflow.toll.api.GeoFlowUtils;
import com.qfree.geoflow.toll.api.GeoFlowVehicleRecord;
import com.qfree.geoflow.toll.api.HmiOption;
import com.qfree.its.iso21177poc.common.geoflow.thick_client.ThickClientPostThread;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;

public class GeoFlowAccountUtils {
    private static final String TAG = GeoFlowAccountUtils.class.getSimpleName();

    public static final String polestarObuId = "AAOS";

    public static boolean hasRegistered(SharedPreferences preferences){
        return preferences.getBoolean(PreferenceKey.ACCOUNT_REGISTERED, false);
    }

    public static void setRegistered(SharedPreferences preferences, boolean registerd){
        preferences.edit().putBoolean(PreferenceKey.ACCOUNT_REGISTERED, registerd).apply();
    }

    public static void getCarInfoModel(CarContext carContext, String licensePlateNumber, EventHandler eventHandler) throws Exception {
        CarHardwareManager carHardwareManager = carContext.getCarService(CarHardwareManager.class);
        CarInfo carInfo = carHardwareManager.getCarInfo();
        carInfo.fetchModel(carContext.getMainExecutor(), new OnCarDataAvailableListener<Model>() {
            @Override
            public void onCarDataAvailable(@NonNull Model data) {
                Log.d(TAG, "onCarDataAvailable: Model " + data);
                try {
                    GeoFlowAccountRecord geoFlowAccountRecord = createAccount(carContext, data, licensePlateNumber);
                    eventHandler.obtainMessage(EventHandler.CAR_MODEL_DATA_EVENT_MSG, data).sendToTarget();
                    eventHandler.obtainMessage(EventHandler.ACCOUNT_CREATED_EVENT_MSG, geoFlowAccountRecord).sendToTarget();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static GeoFlowAccountRecord createAccount(CarContext carContext, Model model, String licencePlateNumber) throws Exception {
        Log.d(TAG, "createVehicle: " + model.getName() + " " + licencePlateNumber);
        GeoFlowUserRecord geoFlowUserRecord = createGeoFlowUserRecord(carContext);
        GeoFlowVehicleRecord geoFlowVehicleRecord = createGeoFlowVehicleRecord(model, licencePlateNumber, geoFlowUserRecord.userId);
        return new GeoFlowAccountRecord(geoFlowUserRecord, geoFlowVehicleRecord);
//        accountRecord.saveAsPreference(PreferenceManager.getDefaultSharedPreferences(carContext));
    }

    private static GeoFlowUserRecord createGeoFlowUserRecord(Context context) throws Exception {
        GeoFlowUserRecord userRecord = new GeoFlowUserRecord();
        userRecord.userId = GeoFlowDatabaseObject.generateUniqueId("U");
        userRecord.email = "geoflow_" + (new Date().getTime()) + "@gmail.com";
        userRecord.suspended = false;
        userRecord.name = "Polestar AAOS";
        userRecord.createDate = LocalDateTime.now();
        userRecord.hmiMode = HmiOption.ANDROID_AUTO;

        //KeyPair for encryption
        KeyPair encryptionKeyPair = EncryptionKeyUtils.generateRsaKeyPair();
        EncryptionKeyUtils.storePrivateKeyEncrypted(context, "private_key", "1234", encryptionKeyPair.getPrivate());
        userRecord.publicKeyForEncryption = encryptionKeyPair.getPublic().getEncoded();

        //KeyPair for signing
        KeyPair signingKeyPair = EncryptionKeyUtils.generateECKeyPair();
        userRecord.privateKeyForSigning = signingKeyPair.getPrivate().getEncoded();
        userRecord.publicKeyForSigning = signingKeyPair.getPublic().getEncoded();

        return userRecord;
    }

    private static GeoFlowVehicleRecord createGeoFlowVehicleRecord(Model model, String licencePlateNumber, String userId) {
        GeoFlowVehicleRecord vehicleRecord = new GeoFlowVehicleRecord();
        vehicleRecord.vehicleId = GeoFlowDatabaseObject.generateUniqueId("V");
        if (model.getName().getValue() != null) {
            vehicleRecord.carModel = model.getName().getValue();
        } else {
            vehicleRecord.carModel = "Polestar-default";
        }
        vehicleRecord.licensePlate = licencePlateNumber;
        vehicleRecord.licensePlateCountry = "NO";
        vehicleRecord.obuId = polestarObuId;
        vehicleRecord.createDate = LocalDateTime.now();
        vehicleRecord.createdByUserId = userId;
        return vehicleRecord;
    }

    public interface LicensePlateNumberCallback {
        void onLicensePlateNumberProvided(String licencePlateNumber);
    }

    public static void submitToServer(String url, String userTopic, String vehicleTopic,
                                      GeoFlowUserRecord userRecord, GeoFlowVehicleRecord vehicleRecord,
                                      Handler handler, long timeout) {

        HashMap<String, String> requestProps = new HashMap<>();
        requestProps.put("content-type", "application/json");
        requestProps.put("X-Qfree-ItsStation", vehicleRecord.licensePlate);
        requestProps.put("X-Qfree-Hostname", userRecord.name);

        Gson gson = GeoFlowFactory.createGson();

        byte[] userContent = gson.toJson(userRecord).getBytes(StandardCharsets.UTF_8);
        userTopic += "/" + GeoFlowUtils.bin2hex(userRecord.userId.getBytes(StandardCharsets.UTF_8));
        ThickClientPostThread userPostThread = new ThickClientPostThread(url, userContent,
                requestProps, userTopic, handler, timeout);
        userPostThread.start();

        byte[] vehicleContent = gson.toJson(vehicleRecord).getBytes(StandardCharsets.UTF_8);
        vehicleTopic += "/" + GeoFlowUtils.bin2hex(vehicleRecord.vehicleId.getBytes(StandardCharsets.UTF_8));
        ThickClientPostThread vehiclePostThread = new ThickClientPostThread(url, vehicleContent,
                requestProps, vehicleTopic, handler, timeout);
        vehiclePostThread.start();
    }

}
