package com.qfree.its.iso21177poc.common.app;

//
// https://osmdroid.github.io/osmdroid/How-to-use-the-osmdroid-library.html
//

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.qfree.its.iso21177poc.common.R;
import com.qfree.its.iso21177poc.common.geoflow.EventHandler;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();
    public static boolean isActive = false;
    private MapView mMapView;
    private IMapController mMapController;
    private TextView mRow1;
    private TextView mRow2;
    ImageView imageView;
    private static DatexReply datexReply = null;
    private static int datexReplyCnt = 0;
    MapHandler mapThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        FileLogger.setContext(getApplicationContext());
        FileLogger.log("MapActivity.onCreate");
        try {
            super.onCreate(savedInstanceState);

            Context context = getApplicationContext();
            Configuration.getInstance().load(context, android.preference.PreferenceManager.getDefaultSharedPreferences(context));

            mapThreadHandler = new MapHandler(context, Looper.getMainLooper(), this);

            setContentView(R.layout.activity_map);

            Log.d(TAG, "onCreate: finding MAP ");
            mMapView = findViewById(R.id.map);
            OsmdroidUtils.initMap(mMapView);
            mMapController = mMapView.getController();
            OsmdroidUtils.setZoom(mMapController, 14.0);
            Log.d(TAG, "onCreate: MAP done");

            mRow1 = findViewById(R.id.toll_cost);
            mRow2 = findViewById(R.id.toll_distance);
            imageView = findViewById(R.id.imageView);

            FloatingActionButton refreshBtn = findViewById(R.id.refresh_btn);
            refreshBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "refreshBtn");
                    try {
                        populateTripView();
                        new DatexFetchHttp().execute(mapThreadHandler);
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

    private void addSigns() {
        if (datexReply == null || datexReply.signList == null)
            return;

        Log.d(TAG, "addSigns: SignCnt=" + datexReply.signList.size());
        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        for (DatexVmsSign sign : datexReply.signList) {
            if (sign.isBlank)
                continue;
            items.add(new OverlayItem(sign.signId,"Title", "Description " + sign.signId, new GeoPoint(sign.latitude, sign.longitude)));
        }

        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        Log.d(TAG, "onItemSingleTapUp: " + index + "  " + item.getUid());
                        mRow1.setText("");
                        mRow2.setText("");
                        for (DatexVmsSign sign : datexReply.signList) {
                            if (sign.signId.equals(item.getUid())) {
                                mRow1.setText("SignId: " + sign.signId);
                                if (sign.hasSpeed && sign.speedLimitValue > 0)
                                    mRow2.setText("Speed limit " + sign.speedLimitValue + " km/h");

                                if (sign.imageData != null && !sign.imageData.isEmpty()) {
                                    byte[] imageBytes = Base64.decode(sign.imageData, Base64.DEFAULT);
                                    Bitmap decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                    imageView.setImageBitmap(decodedImage);
                                } else {
                                    imageView.setImageBitmap(null);
                                }
                                break;
                            }
                        }
                        return true;
                    }
                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        Log.d(TAG, "onItemLongPress: " + index + "  " + item.getUid());
                        return false;
                    }
                }, getApplicationContext());

        mOverlay.setFocusItemsOnTap(true);
        mMapView.getOverlays().add(mOverlay);
        mMapView.invalidate();
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
        mRow1.setText("");
        mRow2.setText("");
        TripSummary mTripSummary = EventHandler.getTripSummary();
        if (mTripSummary != null) {
            OsmdroidUtils.drawRoute(getApplicationContext(), mMapView, mMapController, mTripSummary.getTripRoute());
        } else {
            OsmdroidUtils.clearAll(mMapView);
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

    public void onDatexOk(DatexResponse obj) {
        Log.d(TAG, "onDatexOk: HttpCode=" + obj.httpResponseCode + "  Status=" + obj.status + "  CertFam:" + obj.certificateFamily);
        Log.d(TAG, "onDatexOk: DatexInfo:" + (obj.datexReply==null?"null":"ok") + "  SignCnt=" + ((obj.datexReply!= null && obj.datexReply.signList != null) ? obj.datexReply.signList.size() : 0));
        datexReply = obj.datexReply;
        datexReplyCnt++;
        addSigns();
    }

    public void onDatexError(DatexResponse obj) {
        Log.d(TAG, "onDatexError: HttpCode=" + obj.httpResponseCode + "  Status=" + obj.status + "  CertFam:" + obj.certificateFamily);
        Log.d(TAG, "onDatexError: ErrorText=" + obj.errorText);
        if (obj.exception != null)
            Log.d(TAG, "onDatexError: Exception=" + obj.exception.getClass().getName() + ": " + obj.exception.getMessage());
        else
            Log.d(TAG, "onDatexError: Exception=null");
        Log.d(TAG, "onDatexError: DatexInfo:" + (obj.datexReply==null?"null":"ok") + "  SignCnt=" + ((obj.datexReply!= null && obj.datexReply.signList != null) ? obj.datexReply.signList.size() : 0));
    }
}
