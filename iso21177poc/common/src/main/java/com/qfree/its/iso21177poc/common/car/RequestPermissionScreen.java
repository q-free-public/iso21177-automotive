package com.qfree.its.iso21177poc.common.car;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.OnRequestPermissionsListener;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;

import com.qfree.its.iso21177poc.common.geoflow.GeoFlowService;
import com.qfree.its.iso21177poc.common.geoflow.PermissionUtils;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogEvents;

import java.util.ArrayList;
import java.util.List;

public class RequestPermissionScreen extends Screen {
    private static final String TAG = RequestPermissionScreen.class.getSimpleName();

    private PermissionUtils.PermissionCheckCallback mPermissionCheckCallback;

    protected RequestPermissionScreen(@NonNull CarContext carContext,  PermissionUtils.PermissionCheckCallback permissionCheckCallback) {
        super(carContext);
        this.mPermissionCheckCallback = permissionCheckCallback;
    }

    //Must first request fine location, then background.
    @NonNull
    @Override
    public Template onGetTemplate() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add("android.car.permission.CAR_SPEED");
        permissions.add("android.car.permission.CAR_ENERGY");
        permissions.add("android.car.permission.CAR_IDENTIFICATION");
        permissions.add("android.car.permission.CAR_POWERTRAIN");
        permissions.add("android.car.permission.CAR_MILEAGE");

        OnClickListener onClickListener = ParkedOnlyOnClickListener.create(new OnClickListener() {
            @Override
            public void onClick() {
                FileLogger.logEvent(LogEvents.GPS_INIT, "RequestPermissionScreen: create.onClick Request " + String.join(" ", permissions));

                getCarContext().requestPermissions(permissions, new OnRequestPermissionsListener() {
                    @Override
                    public void onRequestPermissionsResult(@NonNull List<String> grantedPermissions,
                                                           @NonNull List<String> rejectedPermissions)
                    {
                        if(!grantedPermissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                            FileLogger.logEvent(LogEvents.GPS_INIT, "RequestPermissionScreen: onRequestPermissionsResult/onGetTemplate. granted:" + String.join(" ", grantedPermissions) +  "  rejected:" + String.join(" ", rejectedPermissions));
                            onGetTemplate();
                        } else {
                            FileLogger.logEvent(LogEvents.GPS_INIT, "RequestPermissionScreen: onRequestPermissionsResult/granted. granted:" + String.join(" ", grantedPermissions) +  "  rejected:" + String.join(" ", rejectedPermissions));
                            mPermissionCheckCallback.onPermissionGranted();
                            finish();
                        }
                    }
                });
            }
        });

        Action action = new Action.Builder()
                .setTitle("Continue")
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(onClickListener)
                .build();

        return new MessageTemplate.Builder("The Road Pricing application needs access to location data and vehicle sensors. Please grant full permissions on the next pages.")
                .setTitle("Road Pricing")
                .setHeaderAction(Action.APP_ICON)
                .addAction(action)
                .build();
    }
}
