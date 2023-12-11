package com.qfree.its.iso21177poc.common.car;

import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.LongMessageTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;

public class InfoScreen extends Screen {

    public InfoScreen(CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        OnClickListener onClickListener = ParkedOnlyOnClickListener.create(new OnClickListener() {
            @Override
            public void onClick() {
                finish();
            }
        });

        Action action = new Action.Builder()
                .setTitle("Continue")
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(onClickListener)
                .build();

        String version = "?";
        try {
            PackageInfo pInfo = getCarContext().getPackageManager().getPackageInfo(getCarContext().getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Exception e) {
            version = e.getMessage();
        }

        String infoText = "This application is a part of the GeoFlow research project. " +
                "It collects location data while applying geofence information from NVDB to calculate a Road User Charging fee based on distance driven, time of day, and zone. " +
                "Your data is cryptographically protected and only available for research purposes in the project. " +
                "The data collection starts then the gear selector is put in Drive and stopped when the gear is in Park. " +
                "Application version " + version +
                "\r\n\r\n" +
                "Please contact its-pilot@vegvesen.no for more information.";

        return new LongMessageTemplate.Builder(infoText)
                .setTitle("Road Pricing")
                .setHeaderAction(Action.APP_ICON)
                .addAction(action)
                .build();
    }
}
