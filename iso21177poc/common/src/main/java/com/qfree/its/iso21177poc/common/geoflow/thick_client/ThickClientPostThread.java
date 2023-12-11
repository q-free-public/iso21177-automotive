package com.qfree.its.iso21177poc.common.geoflow.thick_client;

import android.os.Handler;
import android.util.Log;

import com.qfree.its.iso21177poc.common.geoflow.EventHandler;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;

public class ThickClientPostThread extends Thread {
    private static final String TAG = ThickClientPostThread.class.getSimpleName();

    Thread mLogFileThread;

    public ThickClientPostThread(String uploadPath, byte[] content, HashMap<String, String> requestProperties,
                                 String uploadTopic, Handler handler, long timeout) {
        this.mLogFileThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(timeout);
                        Log.d(TAG, "Try to post Invoice summary");
                        doPost(uploadPath, content, requestProperties, uploadTopic, handler);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static void doPost(String urlStr, byte[] content, HashMap<String, String> requestProperties,
                              String curUploadTopic, Handler handler) throws Exception {
        Log.d(TAG, "doPost: URL: " + urlStr + "  size=" + content.length);
        ThickClientPostEvent postEvent = new ThickClientPostEvent();
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

                // This must be called, even if nothing is read, without it, a resource is leaked inside the HTTP library
                try (InputStream inStream = httpURLConnection.getInputStream()) {
                    Log.d(TAG, "doPost: " + "inputStream open");
                }

                postEvent.setDate(LocalDateTime.now());
                postEvent.setResponseCode(respCode);
                postEvent.setUploadTopic(curUploadTopic);
                postEvent.setContentLength(content.length);
            } finally {
                httpURLConnection.disconnect();
            }
        } catch (Exception e) {
            postEvent.setDate(LocalDateTime.now());
            postEvent.setUploadTopic(curUploadTopic);
            postEvent.setError(e.getMessage().replaceAll("[\r\n]+ [ \t]*", ";"));
            e.printStackTrace();
            throw new Exception("HTTP error", e);
        } finally {
            handler.obtainMessage(EventHandler.THICK_CLIENT_UPLOAD_EVENT_MSG, postEvent).sendToTarget();
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
