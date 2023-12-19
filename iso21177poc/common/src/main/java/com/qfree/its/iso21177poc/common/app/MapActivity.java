package com.qfree.its.iso21177poc.common.app;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.qfree.its.iso21177poc.common.R;
import com.qfree.its.iso21177poc.common.geoflow.EventHandler;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;

import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();

    public static boolean isActive = false;
    private MapView mMapView;
    private IMapController mMapController;
    private TextView mTollCost;
    private TextView mTollDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        FileLogger.setContext(getApplicationContext());
        FileLogger.log("MapActivity.onCreate");
        try {
            super.onCreate(savedInstanceState);

            Context context = getApplicationContext();
            Configuration.getInstance().load(context, android.preference.PreferenceManager.getDefaultSharedPreferences(context));
            setContentView(R.layout.activity_map);

            mMapView = findViewById(R.id.map);
            OsmdroidUtils.initMap(mMapView);
            mMapController = mMapView.getController();
            OsmdroidUtils.setZoom(mMapController, 14.0);

            mTollCost = findViewById(R.id.toll_cost);
            mTollDistance = findViewById(R.id.toll_distance);

            FloatingActionButton refreshBtn = findViewById(R.id.refresh_btn);
            refreshBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "refreshBtn");
                    try {
                        populateTripView();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            FloatingActionButton resetBtn = findViewById(R.id.reset_btn);
            resetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "resetBtn");
                    try {
                        EventHandler.clearTripSummary();
                        populateTripView();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            FloatingActionButton closeBtn = findViewById(R.id.close_btn);
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "closeBtn");
                    finish();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        try {
            Log.d(TAG, "onStart: ");
            super.onStart();
            isActive = true;
            populateTripView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateTripView() {
        Log.d(TAG, "populateTripView");
        TripSummary mTripSummary = EventHandler.getTripSummary();
        if (mTripSummary != null) {
            OsmdroidUtils.drawRoute(getApplicationContext(), mMapView, mMapController, mTripSummary.getTripRoute());
            mTollCost.setText("X" + getString(R.string.toll_cost, mTripSummary.getTripCost(), mTripSummary.getCurrency()));
            mTollDistance.setText(getString(R.string.toll_distance, mTripSummary.getTripDistance(), "km"));
        } else {
            OsmdroidUtils.clearAll(mMapView);
            mTollCost.setText("abc");
            mTollDistance.setText("");
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        isActive = false;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
