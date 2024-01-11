package com.qfree.its.iso21177poc.common.app;

//
// https://osmdroid.github.io/osmdroid/How-to-use-the-osmdroid-library.html
//

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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

public class MapActivity extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();
    public static boolean isActive = false;
    private MapView mMapView;
    private IMapController mMapController;
    private TextView mRow1;
    private TextView mRow2;
    private TextView mTextDatexStatus;
    private EditText mTextRequestedPsid;
    private EditText mTextRequestedSsp;
    ImageView imageView;
    private static DatexReply datexReply = null;
    private static int datexReplyCnt = 0;
    MapHandler mapThreadHandler;
    ItemizedOverlayWithFocus<OverlayItem> mOverlay;
    String currSignId = "none";

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

            mRow1 = findViewById(R.id.vms_signid);
            mRow2 = findViewById(R.id.vms_details);
            mTextDatexStatus = findViewById(R.id.datex_status);
            mTextRequestedPsid = (EditText)findViewById(R.id.psid_req);
            mTextRequestedSsp = (EditText)findViewById(R.id.ssp_req);
            imageView = findViewById(R.id.imageView);

            FloatingActionButton refreshBtn = findViewById(R.id.refresh_btn);
            refreshBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "refreshBtn");
                    try {
                        populateTripView();
                        try {
                            DatexFetchHttp.optPsid = Long.parseLong(mTextRequestedPsid.getText().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            mTextDatexStatus.setText("Illegal PSID entered");
                            return;
                        }
                        try {
                            DatexFetchHttp.setSsp(mTextRequestedSsp.getText().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            mTextDatexStatus.setText("Illegal SSP entered");
                            return;
                        }
                        mTextDatexStatus.setText("Requesting VMS information from server...");
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

        ArrayList<OverlayItem> noItems = new ArrayList<OverlayItem>();
        mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(noItems,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        Log.d(TAG, "onItemSingleTapUp: " + index + "  " + item.getUid());
                        mRow1.setText("");
                        mRow2.setText("");
                        updateSignFromId(item.getUid());
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
    }

    private void updateSignFromId(String signId) {
        if (signId == null || signId.isEmpty())
            return;

        for (DatexVmsSign sign : datexReply.signList) {
            if (sign.signId.equals(signId)) {
                currSignId = signId;
                updateSign(sign);
                break;
            }
        }
    }

    private void updateSign(DatexVmsSign sign) {
        Log.d(TAG, "updateSign: sign:" + sign.signId);
        mRow1.setText(sign.signId);
        if (sign.hasSpeed && sign.speedLimitValue > 0) {
            mRow2.setVisibility(View.VISIBLE);
            mRow2.setText("Speed limit " + sign.speedLimitValue + " km/h");
        } else {
            mRow2.setVisibility(View.INVISIBLE);
            mRow2.setText("");
        }

        if (sign.imageData != null && !sign.imageData.isEmpty()) {
            byte[] imageBytes = Base64.decode(sign.imageData, Base64.DEFAULT);
            Bitmap decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            imageView.setImageBitmap(decodedImage);
        } else {
            imageView.setImageBitmap(null);
        }
    }

    private void addSigns() {
        if (datexReply == null || datexReply.signList == null)
            return;

        // Pop down old?  How?
        for (OverlayItem m : mOverlay.getDisplayedItems()) {
//            Drawable dr = m.getMarker(0);
//            Marker m;
//            m.closeInfoWindow();
//            m.setInfoWindow(null);
        }

        Log.d(TAG, "addSigns: SignCnt=" + datexReply.signList.size());
        Log.d(TAG, "addSigns: overlay.cnt=" + mMapView.getOverlays().size());
        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        for (DatexVmsSign sign : datexReply.signList) {
            if (sign.isBlank)
                continue;
            items.add(new OverlayItem(sign.signId,"VMS sign", sign.signId, new GeoPoint(sign.latitude, sign.longitude)));
        }

        mOverlay.removeAllItems();
        mOverlay.addItems(items);
        updateSignFromId(currSignId);
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

        String sInfo = "Success:";
        sInfo += " Reply number " + datexReplyCnt + ". ";
        if (obj.datexReply !=  null && obj.datexReply.signList != null) {
            sInfo += obj.datexReply.signList.size() + " signs found";
        }
        sInfo += " using " + obj.protocol;
        sInfo += ".\r\n";

        sInfo += "Certs:" + obj.certificateFamily;
        if (obj.peerCertHash != null && !obj.peerCertHash.isEmpty()) {
            sInfo += " PeerCert:" + obj.peerCertHash;
            sInfo += " PSID:" + obj.peerCertPsid;
            sInfo += " SSP:" + obj.peerCertSsp;
        }
        mTextDatexStatus.setText(sInfo);

        addSigns();
    }

    public void onDatexError(DatexResponse obj) {
        Log.d(TAG, "onDatexError: HttpCode=" + obj.httpResponseCode + "  Status=" + obj.status + "  CertFam:" + obj.certificateFamily);
        Log.d(TAG, "onDatexError: ErrorText=" + obj.errorText);
        if (obj.exception != null) {
            Log.d(TAG, "onDatexError: Exception=" + obj.exception.getClass().getName() + ": " + obj.exception.getMessage());
        } else {
            Log.d(TAG, "onDatexError: Exception=null");
        }
        Log.d(TAG, "onDatexError: DatexInfo:" + (obj.datexReply==null?"null":"ok") + "  SignCnt=" + ((obj.datexReply!= null && obj.datexReply.signList != null) ? obj.datexReply.signList.size() : 0));

        String sInfo = "Failure:";
        if (obj.httpResponseCode > 0)
            sInfo += " HTTP code:" + obj.httpResponseCode;
        sInfo += " Status:" + obj.status;
        if (obj.errorText != null && !obj.errorText.isEmpty())
            sInfo += " ErrorText=" + obj.errorText;
        if (obj.exception != null) {
            sInfo += " Exception=" + obj.exception.getClass().getName() + ": " + obj.exception.getMessage();
        }
        sInfo += "\r\n";
        sInfo += "Certs:" + obj.certificateFamily;
        if (obj.peerCertHash != null && !obj.peerCertHash.isEmpty()) {
            sInfo += " PeerCert:" + obj.peerCertHash;
            sInfo += " PSID:" + obj.peerCertPsid;
            sInfo += " SSP:" + obj.peerCertSsp;
        }
        mTextDatexStatus.setText(sInfo);
    }
}
