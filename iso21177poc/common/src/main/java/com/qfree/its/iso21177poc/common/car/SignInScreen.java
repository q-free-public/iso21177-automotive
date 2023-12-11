package com.qfree.its.iso21177poc.common.car;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.InputCallback;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.model.signin.InputSignInMethod;
import androidx.car.app.model.signin.SignInTemplate;
import androidx.preference.PreferenceManager;

import com.qfree.its.iso21177poc.common.geoflow.GeoFlowAccountUtils;
import com.qfree.its.iso21177poc.common.geoflow.PreferenceKey;

public class SignInScreen extends Screen implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = SignInScreen.class.getSimpleName();
    private final GeoFlowAccountUtils.LicensePlateNumberCallback mLicensePlateNumberCallback;

    private boolean mSubmitted = false;

    public SignInScreen(CarContext carContext, GeoFlowAccountUtils.LicensePlateNumberCallback licensePlateNumberCallback) {
        super(carContext);
        this.mLicensePlateNumberCallback = licensePlateNumberCallback;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(carContext);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        InputSignInMethod.Builder inputSigningBuilder = new InputSignInMethod.Builder(new InputCallback() {
            @Override
            public void onInputSubmitted(@NonNull String text) {
                Log.d(TAG, "onInputSubmitted: " + text);
                mLicensePlateNumberCallback.onLicensePlateNumberProvided(text.toUpperCase().replaceAll("[\\'\";\\.,%\\?+&\\*\\\\\\(\\)!#=:/´`]", "-"));
                mSubmitted = true;
                invalidate();
            }
        }).setHint("License Plate Number");

        if (!mSubmitted) {
            return new SignInTemplate.Builder(inputSigningBuilder.build())
                    .setTitle("Road Pricing")
                    .setHeaderAction(Action.APP_ICON)
                    .setInstructions("Enter license plate number")
//                    .setAdditionalText("Explanatory text")
                    .build();
        } else {
            return new SignInTemplate.Builder(inputSigningBuilder.build())
                    .setTitle("Road Pricing")
                    .setHeaderAction(Action.APP_ICON)
                    .setLoading(true)
                    .build();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
//        Log.d(TAG, "onSharedPreferenceChanged: " + s);
        if (s.equals(PreferenceKey.ACCOUNT_REGISTERED) && sharedPreferences.getBoolean(s, false)){
            Log.d(TAG, "onSharedPreferenceChanged: " + PreferenceKey.ACCOUNT_REGISTERED + ": " + sharedPreferences.getBoolean(s, false));
            finish();
        }
    }
}
