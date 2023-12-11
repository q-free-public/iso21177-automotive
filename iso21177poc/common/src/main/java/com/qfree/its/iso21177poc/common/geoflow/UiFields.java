package com.qfree.its.iso21177poc.common.geoflow;

import android.content.SharedPreferences;

import androidx.car.app.hardware.info.EnergyLevel;
import androidx.car.app.hardware.info.EnergyProfile;
import androidx.car.app.hardware.info.Mileage;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.hardware.info.Speed;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.qfree.geoflow.toll.api.GeoFlowInvoiceSummary;
import com.qfree.geoflow.toll.api.GeoFlowWsApiEventTollCostUpdate;
import com.qfree.its.iso21177poc.common.car.CarPropertyGear;
import com.qfree.its.iso21177poc.common.car.CarPropertyIgnitionState;
import com.qfree.its.iso21177poc.common.geoflow.thick_client.BillingPeriod;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogFilePostEvent;
import com.qfree.its.location.Position;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

public class UiFields {
    private QfreePosImpl position;
    private LogFilePostEvent logFilePostEvent;
    private GeoFlowWsApiEventTollCostUpdate tollCostUpdate;
    private String exceptionMsg;
    private Model carModel;
    private EnergyProfile carEnergyProfile;
    private Speed carSpeed;
    private EnergyLevel carEnergyLevel;
    private Mileage carMileage;
    private CarPropertyGear carPropertyGear;
    private CarPropertyIgnitionState carPropertyIgnitionState;
    private GeoFlowAccountRecord geoFlowAccount;
    private String vin;
    private GeoFlowInvoiceSummary invoiceSummary;
    private BillingPeriod billingPeriod;
    private long posCnt = -1;
    private long logFileUploadOkCount = 0;
    private long logFileUploadErrorCount = 0;
    private String logFileUploadErrorText = "";
    private long logFileDownloadOkCount = 0;
    private long logFileDownloadErrorCount = 0;
    private String logFileDownloadErrorText = "";

    public void saveAsPreference(SharedPreferences sharedPreferences) {
        Gson gson = new GsonBuilder().
                registerTypeAdapter(Date.class, new DateSerializer()).
                registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer()).
                create();
        sharedPreferences.edit().putString(PreferenceKey.UI_FIELDS, gson.toJson(this)).apply();
    }

    public static UiFields retrieveFromPreference(SharedPreferences sharedPreferences) {
        Gson gson = new GsonBuilder().
                registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer()).
                registerTypeAdapter(Date.class, new DateDeserializer()).
                create();
        String str = sharedPreferences.getString(PreferenceKey.UI_FIELDS, null);
        UiFields uiFields = gson.fromJson(str, UiFields.class);
        if (uiFields == null){
            uiFields = new UiFields();
        }
        return uiFields;
    }

    public static void clearUiFieldsFromPreference(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().remove(PreferenceKey.UI_FIELDS).apply();
    }

    public LogFilePostEvent getLogFileUploadEvent() {
        return logFilePostEvent;
    }

    public void setLogFileUploadErrorIncrement(String lastError) {
        logFileUploadErrorCount++;
        logFileUploadErrorText = lastError;
    }

    public void setLogFileUploadOkIncrement() {
        logFileUploadOkCount++;
        logFileUploadErrorText = "";
    }

    public void setLogFileDownloadErrorIncrement(String lastError) {
        logFileDownloadErrorCount++;
        logFileDownloadErrorText = lastError;
    }

    public void setLogFileDownloadOkIncrement() {
        logFileDownloadOkCount++;
        logFileDownloadErrorText = "";
    }

    public long getLogFileUploadOkCount() {
        return logFileUploadOkCount;
    }

    public long getLogFileUploadErrorCount() {
        return logFileUploadErrorCount;
    }

    public String getLogFileUploadErrorText() {
        return logFileUploadErrorText;
    }

    public long getLogFileDownloadOkCount() {
        return logFileDownloadOkCount;
    }

    public long getLogFileDownloadErrorCount() {
        return logFileDownloadErrorCount;
    }

    public String getLogFileDownloadErrorText() {
        return logFileDownloadErrorText;
    }

    public Position getPosition() {
        return position;
    }

    public GeoFlowWsApiEventTollCostUpdate getTollCostUpdate() {
        return tollCostUpdate;
    }

    public void setLogFileUploadEvent(LogFilePostEvent logFilePostEvent) {
        this.logFilePostEvent = logFilePostEvent;
    }

    public void setPosition(QfreePosImpl position, long cnt) {
        this.position = position;
        this.posCnt = cnt;
    }

    public long getPosCnt() {
        return posCnt;
    }

    public void setTollCostUpdate(GeoFlowWsApiEventTollCostUpdate tollCostUpdate) {
        this.tollCostUpdate = tollCostUpdate;
    }

    public String getExceptionMsg() {
        return exceptionMsg;
    }

    public void setExceptionMsg(String exceptionMsg) {
        this.exceptionMsg = exceptionMsg;
    }

    public Model getCarModel() {
        return carModel;
    }

    public void setCarModel(Model carModel) {
        this.carModel = carModel;
    }

    public EnergyProfile getCarEnergyProfile() {
        return carEnergyProfile;
    }

    public void setCarEnergyProfile(EnergyProfile carEnergyProfile) {
        this.carEnergyProfile = carEnergyProfile;
    }

    public Speed getCarSpeed() {
        return carSpeed;
    }

    public void setCarSpeed(Speed carSpeed) {
        this.carSpeed = carSpeed;
    }

    public EnergyLevel getCarEnergyLevel() {
        return carEnergyLevel;
    }

    public void setCarEnergyLevel(EnergyLevel carEnergyLevel) {
        this.carEnergyLevel = carEnergyLevel;
    }

    public Mileage getCarMileage() {
        return carMileage;
    }

    public void setCarMileage(Mileage carMileage) {
        this.carMileage = carMileage;
    }

    public CarPropertyGear getCarPropertyGear() {
        return carPropertyGear;
    }

    public void setCarPropertyGear(CarPropertyGear carPropertyGear) {
        this.carPropertyGear = carPropertyGear;
    }

    public CarPropertyIgnitionState getCarPropertyIgnitionState() {
        return carPropertyIgnitionState;
    }

    public void setCarPropertyIgnitionState(CarPropertyIgnitionState carPropertyIgnitionState) {
        this.carPropertyIgnitionState = carPropertyIgnitionState;
    }

    public GeoFlowAccountRecord getGeoFlowAccount() {
        return geoFlowAccount;
    }

    public void setGeoFlowAccount(GeoFlowAccountRecord geoFlowAccountRecord) {
        this.geoFlowAccount = geoFlowAccountRecord;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public void setInvoiceSummary(GeoFlowInvoiceSummary invoiceSummary) {
        this.invoiceSummary = invoiceSummary;
    }

    public GeoFlowInvoiceSummary getInvoiceSummary() {
        return invoiceSummary;
    }

    public void setBillingPeriod(BillingPeriod billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public BillingPeriod getBillingPeriod() {
        return billingPeriod;
    }

    private static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }

    private static SimpleDateFormat getDateFormatLong() {
        SimpleDateFormat dateFormatLong;
        dateFormatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormatLong.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormatLong;
    }

    private static SimpleDateFormat getDateFormatStd() {
        SimpleDateFormat dateFormatStd;
        dateFormatStd = new SimpleDateFormat("MMM dd',' yyyy HH:mm:ss");
        dateFormatStd.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormatStd;
    }

    private static class DateSerializer implements JsonSerializer<Date> {
        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(getDateFormatLong().format(src));
        }
    }

    private static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime>{
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String str = json.getAsString();
            //Log.d("UiFields", "deserialize LocalDateTime: " + str);
            try {
                // This is the normal case
                return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e1) {
                try {
                    return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                } catch (Exception e) {
                    throw new JsonParseException(str, e);
                }
            }
        }
    }

    private static class DateDeserializer implements JsonDeserializer<Date>{
        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            //Log.d("UiFields", "deserialize Date: " + json.getAsString());
            String str = json.getAsString();
            try {
                if (str.matches("^[A-Z].*")) {
                    // Month first "Jan 31, 2023 10:46:45 AM"
                    return getDateFormatStd().parse(str);
                } else {
                    // ISO "2023-01-31 17:00:00"
                    return getDateFormatLong().parse(str);
                }
            } catch (Exception e) {
                throw new JsonParseException(str, e);
            }
        }
    }
}
