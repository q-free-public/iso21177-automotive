package com.qfree.its.iso21177poc.common.geoflow;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.qfree.its.location.Position;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

import androidx.car.app.hardware.info.Model;
import androidx.car.app.hardware.info.Speed;

public class UiFields {
    private QfreePosImpl position;
    private String exceptionMsg;
    private Model carModel;
    private Speed carSpeed;
    private String vin;
    private long posCnt = -1;

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

    public Position getPosition() {
        return position;
    }

    public void setPosition(QfreePosImpl position, long cnt) {
        this.position = position;
        this.posCnt = cnt;
    }

    public long getPosCnt() {
        return posCnt;
    }

    public String getExceptionMsg() {
        return exceptionMsg;
    }

    public Model getCarModel() {
        return carModel;
    }

    public void setCarModel(Model carModel) {
        this.carModel = carModel;
    }

    public Speed getCarSpeed() {
        return carSpeed;
    }

    public void setCarSpeed(Speed carSpeed) {
        this.carSpeed = carSpeed;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
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
