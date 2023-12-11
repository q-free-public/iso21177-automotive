package com.qfree.its.iso21177poc.common.car;

import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.LongMessageTemplate;
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

        String infoText = "This application is a part of the Norwegian Public Roads Administration (NPRA) METR project. " +
                "It processes location data while applying Variable Message Signs (VMS) information from Datex-II " +
                "to display information on a map. " +
                "No user information, location information is stored or uploaded or otherwise retained. " +
                "Application version " + version +
                "\r\n\r\n" +
                "Please contact its-pilot@vegvesen.no for more information.";

        return new LongMessageTemplate.Builder(infoText)
                .setTitle("ISO 21177 POC")
                .setHeaderAction(Action.APP_ICON)
                .addAction(action)
                .build();
    }
}
