package com.qfree.its.iso21177poc.common.geoflow;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.qfree.geoflow.toll.api.GeoFlowUserRecord;
import com.qfree.geoflow.toll.api.GeoFlowVehicleRecord;

public class GeoFlowAccountRecord {
    private final GeoFlowUserRecord geoFlowUserRecord;
    private final GeoFlowVehicleRecord geoFlowVehicleRecord;

    public GeoFlowAccountRecord(GeoFlowUserRecord geoFlowUserRecord, GeoFlowVehicleRecord geoFlowVehicleRecord) {
        this.geoFlowUserRecord = geoFlowUserRecord;
        this.geoFlowVehicleRecord = geoFlowVehicleRecord;
    }

    public void saveAsPreference(SharedPreferences preferences) {
        Gson gson = new Gson();
        preferences.edit().putString(PreferenceKey.ACCOUNT_INFO, gson.toJson(this)).apply();
        preferences.edit().putBoolean(PreferenceKey.ACCOUNT_REGISTERED, true).apply();
    }

    public static GeoFlowAccountRecord retrieveFromPreference(SharedPreferences sharedPreferences) {
        Gson gson = new Gson();
        String str = sharedPreferences.getString(PreferenceKey.ACCOUNT_INFO, null);
        return gson.fromJson(str, GeoFlowAccountRecord.class);
    }

    public static void clearAccountInfoFromPreference(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().remove(PreferenceKey.ACCOUNT_INFO).apply();
    }

    public GeoFlowUserRecord getGeoFlowUserRecord() {
        return geoFlowUserRecord;
    }

    public GeoFlowVehicleRecord getGeoFlowVehicleRecord() {
        return geoFlowVehicleRecord;
    }
}
