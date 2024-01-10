package com.qfree.its.iso21177poc.common.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.qfree.its.iso21177poc.common.geoflow.UiFields;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class MapHandler extends Handler {
    private final Context mContext;

    public static final int MSG_DATEX_OK = 100;
    public static final int MSG_DATEX_ERROR = 101;
    private final MapActivity mapActivity;


    public MapHandler(Context context, Looper looper, MapActivity mapActivity) {
        super(looper);
        this.mContext = context;
        this.mapActivity = mapActivity;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        try {
            super.handleMessage(msg);
            if (msg.what == MSG_DATEX_OK) {
                mapActivity.onDatexOk((DatexResponse) msg.obj);
            } else if (msg.what == MSG_DATEX_ERROR) {
                mapActivity.onDatexError((DatexResponse) msg.obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
