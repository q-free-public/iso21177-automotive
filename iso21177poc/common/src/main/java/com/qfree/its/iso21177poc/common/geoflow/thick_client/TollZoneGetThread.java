package com.qfree.its.iso21177poc.common.geoflow.thick_client;

import android.net.TrafficStats;
import android.os.Handler;
import android.util.Log;

import com.qfree.geoflow.toll.api.GeoFlowUserRecord;
import com.qfree.its.iso21177poc.common.geoflow.EventHandler;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;

public class TollZoneGetThread extends Thread {
    private static final String TAG = TollZoneGetThread.class.getSimpleName();

    Thread mTollZoneThread;

    public TollZoneGetThread(String serverUrl, String vehicleIdStr, String user, CostTableImpl costTable, Handler handler, long timeout) {
        this.mTollZoneThread = new Thread(new Runnable() {
            @Override
            public void run() {
                TrafficStats.setThreadStatsTag(0);
                while (true) {
                    try {
                        Thread.sleep(timeout);
                        Log.d(TAG, "Try to download zones");
                        costTable.loadZonesFromServerAndroid(vehicleIdStr, user, serverUrl, handler);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static void doGet(String urlStr, HashMap<String, String> requestProperties, Handler handler) throws Exception {
        TollZoneGetEvent tollZoneGetEvent = new TollZoneGetEvent();
        try {
            URL url = new URL(urlStr);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            for (String key : requestProperties.keySet()) {
                httpURLConnection.setRequestProperty(key, requestProperties.get(key));
            }
            tollZoneGetEvent.setDate(LocalDateTime.now());
            tollZoneGetEvent.setResponseCode(httpURLConnection.getResponseCode());
            FileLogger.log("loadZonesFromServerAndroid: ResponseCode " + tollZoneGetEvent.getResponseCode());
            try {
                InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;// br.lines().collect(Collectors.joining());
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }
                // printResponse(sb.toString());
                FileLogger.log("loadZonesFromServerAndroid: Content length " + sb.length());
                tollZoneGetEvent.setBody(sb.toString());
                tollZoneGetEvent.convertToGeoFlowZonePackage();
            } finally {
                httpURLConnection.disconnect();
            }
        } catch (Exception e) {
            tollZoneGetEvent.setDate(LocalDateTime.now());
            tollZoneGetEvent.setBody(e.getMessage());
            e.printStackTrace();
            throw new Exception("HTTP error", e);
        } finally {
            handler.obtainMessage(EventHandler.ZONE_RULES_DOWNLOAD_EVENT_MSG, tollZoneGetEvent).sendToTarget();
        }
    }

    @Override
    public synchronized void start() {
        this.mTollZoneThread.start();
    }

    @Override
    public void interrupt() {
        this.mTollZoneThread.interrupt();
    }

    private static void printResponse(String stringResponse){
        if (stringResponse.length() > 4000) {
            Log.v(TAG, "stringResponse.length = " + stringResponse.length());
            int chunkCount = stringResponse.length() / 4000;     // integer division
            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if (max >= stringResponse.length()) {
                    Log.v(TAG, "chunk " + i + " of " + chunkCount + ":" + stringResponse.substring(4000 * i));
                } else {
                    Log.v(TAG, "chunk " + i + " of " + chunkCount + ":" + stringResponse.substring(4000 * i, max));
                }
            }
        } else {
            Log.v(TAG, stringResponse);
        }
    }
}
