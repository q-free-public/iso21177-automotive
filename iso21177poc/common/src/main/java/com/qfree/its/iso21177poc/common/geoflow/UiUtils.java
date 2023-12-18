package com.qfree.its.iso21177poc.common.geoflow;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.widget.TextView;

import androidx.car.app.CarContext;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Row;

import com.qfree.geoflow.toll.api.GeoFlowInvoiceSummary;
import com.qfree.geoflow.toll.api.GeoFlowWsApiEventTollCostUpdate;
import com.qfree.its.iso21177poc.common.app.MainActivity;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogFilePostEvent;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogFormatStrings;
import com.qfree.its.iso21177poc.common.R;
import com.qfree.its.location.Position;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class UiUtils {
    private static final Locale locale = Locale.ROOT;
    private static final String TAG = UiUtils.class.getSimpleName();

    public static void updateUiFieldsMobile(Activity activity, UiFields uiFields) throws Exception {
        if (activity instanceof MainActivity) {
            //Location
            if (uiFields.getPosition() != null) {
                Position position = uiFields.getPosition();
                TextView time = activity.findViewById(R.id.value_location_timestamp);
                time.setText(LogFormatStrings.dateFormat.format(LocalDateTime.ofEpochSecond(position.getTimestamp() / 1000, 0, ZoneOffset.UTC)));
                TextView lat = activity.findViewById(R.id.value_location_lat);
                lat.setText(String.format(locale, "%.6f", position.getLatitude()));
                TextView lon = activity.findViewById(R.id.value_location_lon);
                lon.setText(String.format(locale, "%.6f", position.getLongitude()));
                TextView alt = activity.findViewById(R.id.value_location_alt);
                alt.setText(String.format(locale, "%.2f", position.getHeight()));
                TextView head = activity.findViewById(R.id.value_location_head);
                head.setText(String.format(locale, "%.2f", position.getHeading()));
                TextView velo = activity.findViewById(R.id.value_location_velo);
                velo.setText(String.format(locale, "%.2f", position.getVelocity()));
                TextView acc = activity.findViewById(R.id.value_location_acc);
                acc.setText(String.format(locale, "%.2f", Math.sqrt(position.getPositionCovarianceMatrix()[0])));
                TextView nSats = activity.findViewById(R.id.value_location_nsats);
                nSats.setText(String.format(locale, "%d", position.getSatelliteCount()));
                TextView hDop = activity.findViewById(R.id.value_location_hdop);
                hDop.setText(String.format(locale, "%.2f", position.getHdop()));
                TextView prot = activity.findViewById(R.id.value_location_prot);
                prot.setText(String.format(locale, "%.2f", position.getHorizontalProtectionLimit()));
            }
        }
    }

    //Limit of two lines in addText
    public static Pane.Builder updateUiFieldsAutomotive(CarContext carContext, UiFields uiFields) throws Exception {
        Row carInfo = new Row.Builder().setTitle("Car Info").build();
        Row location = new Row.Builder().setTitle("Location").build();

        //Car info
        String carName = null;
        double carSpeed = 0.0;

        if (uiFields.getCarModel() != null) {
            carName = uiFields.getCarModel().getName().getValue();
        }
        if (uiFields.getCarSpeed() != null) {
            carSpeed = uiFields.getCarSpeed().getRawSpeedMetersPerSecond().getValue();
        }

        String version = "?";
        try {
            PackageInfo pInfo = carContext.getPackageManager().getPackageInfo(carContext.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Exception e) {
            version = e.getMessage();
        }

        String strCarInfo = "";
        if (carName != null)
            strCarInfo += "; Name: " + carName;
        strCarInfo += "; Speed: " + String.format(Locale.US, "%.2f", carSpeed);
        strCarInfo += "; Version: " + version;
        carInfo = new Row.Builder().setTitle("Car Info")
                .addText(strCarInfo)
                .build();

        //Location
        Position pos = uiFields.getPosition();
        if (pos != null) {
            location = new Row.Builder().setTitle("Location")
                    .addText(String.format(Locale.ROOT,
                            "Timestamp: %s " +
                                    "Lat: %.7f " +
                                    "Lon: %.7f\n" +
                                    "Head: %.0f " +
                                    "Speed: %.1f " +
                                    "Err: %.2f " +
                                    "Cnt: %d",
                            LogFormatStrings.dateFormatShorterTime.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(pos.getTimestamp()), ZoneId.systemDefault())),
                            pos.getLatitude(),
                            pos.getLongitude(),
                            pos.getHeading(),
                            pos.getVelocity(),
                            Math.sqrt(pos.getPositionCovarianceMatrix()[0]),
                            uiFields.getPosCnt())
                    ).build();
        } else {
            // No pos
            location = new Row.Builder().setTitle("Location")
                    .addText(String.format(Locale.ROOT,
                            "No position  " +
                                    "Cnt: %d",
                            uiFields.getPosCnt())
                    ).build();
        }

        //Excluded, exceed info limitations on screen
//        Row locationMeta = new Row.Builder().setTitle("Location Meta").build();
        if (pos != null) {
//            locationMeta = new Row.Builder().setTitle("Location Meta")
//                    .addText(String.format(Locale.ROOT,
//                            "Velo: %.2f " +
//                                    "nSats: %d " +
//                                    "Err: %.2f " +
//                                    "hdop: %.2f " +
//                                    "Prot lim: %.2f",
//                            pos.getVelocity(),
//                            pos.getSatelliteCount(),
//                            Math.sqrt(pos.getPositionCovarianceMatrix()[0]),
//                            pos.getHdop(),
//                            pos.getHorizontalProtectionLimit())).build();
        }

        return new Pane.Builder()
                .addRow(carInfo)
                .addRow(location);
    }
}
