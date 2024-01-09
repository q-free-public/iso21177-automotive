package com.qfree.its.iso21177poc.common.app;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class DatexFetchHttp extends AsyncTask<Void, Void, String> {
    final public static String TAG = "DatexFetchHttp";

    static {
       System.loadLibrary("common");
    }

    public native String stringFromJNI();
    public native String unameMachine();
    public native String unameVersion();
    public native String unameRelease();
    public native String testParams(String strParam, long l);
    public native long add(long a, long b);

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "onPreExecute");
    }

    @Override
    protected String doInBackground(Void... params) {
        Log.d(TAG, "doInBackground");

        testJni();

        return do_rfc8902("/datex-all.json");
//        return do_https("its1.q-free.com", "/geoserver/all.json");
//        return do_https("its1.q-free.com", "/geoserver/all.speed");
    }

    private String do_https(String host, String file) {
        try {
            URL url = new URL("https://" + host + file);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Log the server response code
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Server responded with: " + responseCode);

            // And if the code was HTTP_OK then parse the contents
            if (responseCode == HttpURLConnection.HTTP_OK) {

                // Convert request content to string
                InputStream is = connection.getInputStream();
                String content = convertInputStream(is, "UTF-8");
                is.close();

                Log.d(TAG, "content size: " + content.length());
                return content;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "content error");
        return null;
    }

    private String do_rfc8902(String url_file) {
        long tickStart = System.currentTimeMillis();
        Rfc8902 rfc8902 = new Rfc8902();
        Log.d(TAG, "C++ OpensslVersion: " + rfc8902.getOpensslVersion());
        int ret = rfc8902.httpGet(url_file); // 200.text");
        long tickEnd = System.currentTimeMillis();
        Log.d(TAG, String.format("C++ RFC8902  ret=%d  Time used %.3f sec",ret, (tickEnd - tickStart) / 1000.0));
        if (ret == 0)
            return null;
        String responseText = rfc8902.httpGetResponse();
        Log.d(TAG, "C++ RFC8902  text: " + responseText.length() + "\r\n" + responseText.substring(0, Math.min(60, responseText.length())));
        return responseText;
    }

    private void testJni() {
        Log.d(TAG, "Calling C++ code");
        String txt = stringFromJNI();
        Log.d(TAG, "C++ ret: " + txt);
        Log.d(TAG, "C++ uname version: " + unameVersion());
        Log.d(TAG, "C++ uname machine: " + unameMachine());
        Log.d(TAG, "C++ uname release: " + unameRelease());
        Log.d(TAG, "C++ testParams: " + testParams("ola", 42));
        Log.d(TAG, "C++ add(10000,10): " + add(10000, 10));
    }

    private String convertInputStream(InputStream is, String encoding) {
        Scanner scanner = new Scanner(is, encoding).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "onPostExecute");
        if (result == null) {
            Log.d(TAG, "onPostExecute. result was null - do nothing");
        } else {
            try {
                Gson gson = new Gson();
                DatexReply datexReply = gson.fromJson(result, DatexReply.class);
                if (datexReply != null && datexReply.signList != null) {
                    Log.d(TAG, "onPostExecute. datex-II info: " + datexReply.retreiveDate + "    SignCount: " + datexReply.signList.size());
                    MapActivity.datexReply = datexReply;
                    MapActivity.datexReplyCnt++;
                } else {
                    Log.d(TAG, "onPostExecute. JSON failed");
                }
            } catch (JsonSyntaxException e) {
                Log.d(TAG, "onPostExecute. JSON decode error: " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }
}
