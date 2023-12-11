package com.qfree.its.iso21177poc.common.geoflow.thin_client;

import android.net.TrafficStats;
import android.os.Handler;
import android.util.Log;

import com.qfree.its.iso21177poc.common.geoflow.EventHandler;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;

public class LogFilePostThread extends Thread {
    private static final String TAG = LogFilePostThread.class.getSimpleName();

    Thread mLogFileThread;

    public LogFilePostThread(String uploadPath, Handler handler, long timeout) {
        this.mLogFileThread = new Thread(new Runnable() {
            @Override
            public void run() {
                TrafficStats.setThreadStatsTag(0);
                while (true) {
                    try {
                        Thread.sleep(timeout);
                        Log.d(TAG, "Try to zip and post log file tmo=" + timeout);
                        FileLogger.zipAndPostLogFile(uploadPath, handler);
                        break;
                    } catch (Exception e) {
                        FileLogger.log("FileUploadError: " + e.getClass().getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static void doPost(String urlStr, byte[] content, HashMap<String, String> requestProperties,
                              String curUploadTopic, Handler handler) throws Exception {
        Log.d(TAG, "doPost: URL: " + urlStr + "  size=" + content.length);
        LogFilePostEvent logFilePostEvent = new LogFilePostEvent();
        try {
            URL url = new URL(urlStr);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            try {
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
                for (String key : requestProperties.keySet()) {
                    httpURLConnection.setRequestProperty(key, requestProperties.get(key));
                }

                httpURLConnection.setFixedLengthStreamingMode(content.length);
                try (OutputStream outputStream = new BufferedOutputStream(httpURLConnection.getOutputStream())) {
                    outputStream.write(content);
                }

                int respCode = httpURLConnection.getResponseCode();
                String respMessage = httpURLConnection.getResponseMessage();
                Log.d(TAG, "doPost: " + "code: " + respCode + " msg: " + respMessage);

                // This must be called, even if nothing is read, without it, a resource is leaked  inside the HTTP library
                try (InputStream inStream = httpURLConnection.getInputStream()) {
                    Log.d(TAG, "doPost: " + "inputStream open");
                }

                logFilePostEvent.setDate(LocalDateTime.now());
                logFilePostEvent.setResponseCode(respCode);
                logFilePostEvent.setUploadTopic(curUploadTopic);
                logFilePostEvent.setContentLength(content.length);
            } finally {
                httpURLConnection.disconnect();
            }
        } catch (Exception e) {
            logFilePostEvent.setDate(LocalDateTime.now());
            logFilePostEvent.setUploadTopic(e.getMessage());
            e.printStackTrace();
            throw new Exception("HTTP error", e);
        } finally {
            handler.obtainMessage(EventHandler.FILE_UPLOAD_EVENT_MSG, logFilePostEvent).sendToTarget();
        }
    }

    @Override
    public synchronized void start() {
        this.mLogFileThread.start();
    }

    @Override
    public void interrupt() {
        this.mLogFileThread.interrupt();
    }
}
