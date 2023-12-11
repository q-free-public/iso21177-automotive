package com.qfree.its.iso21177poc.common.car;

import android.content.SharedPreferences;

import com.qfree.its.iso21177poc.common.geoflow.PreferenceKey;
import com.qfree.its.iso21177poc.common.geoflow.UiFields;
import com.qfree.its.iso21177poc.common.geoflow.UiUtils;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Template;
import androidx.preference.PreferenceManager;

public class TollInfoScreen extends Screen implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = TollInfoScreen.class.getSimpleName();

    private Pane.Builder mTollInfo;

    public TollInfoScreen(CarContext carContext) {
        super(carContext);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(carContext);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        if (mTollInfo != null){
            return new PaneTemplate.Builder(mTollInfo.build())
                    .setTitle("ISO 21177 POC")
                    .setHeaderAction(Action.APP_ICON)
                    .build();
        }
        return new PaneTemplate.Builder(new Pane.Builder().setLoading(true).build())
                .setTitle("ISO 21177 POC")
                .setHeaderAction(Action.APP_ICON)
                .build();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        try {
//            Log.d(TAG, "onSharedPreferenceChanged: " + s);
            if (s.equals(PreferenceKey.UI_FIELDS)) {
                String str = sharedPreferences.getString(s, null);
//            Log.d(TAG, "onSharedPreferenceChanged: " + " " +  s + " " +  str);
                UiFields uiFields = UiFields.retrieveFromPreference(sharedPreferences);
                updateUi(uiFields);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUi(UiFields uiFields) throws Exception {
        mTollInfo = UiUtils.updateUiFieldsAutomotive(getCarContext(), uiFields);
        invalidate();
    }
}
