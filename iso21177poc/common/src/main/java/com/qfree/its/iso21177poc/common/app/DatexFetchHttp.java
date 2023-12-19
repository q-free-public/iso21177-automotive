package com.qfree.its.iso21177poc.common.app;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class DatexFetchHttp extends AsyncTask<Void, Void, String> {
    final public static String TAG = "DatexFetchHttp";

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "onPreExecute");
    }

    @Override
    protected String doInBackground(Void... params) {
        Log.d(TAG, "doInBackground");

        try {
            URL url = new URL("https://its1.q-free.com/geoserver/all.json");
            //URL url = new URL("https://its1.q-free.com/geoserver/speed.json");

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

    private String convertInputStream(InputStream is, String encoding) {
        Scanner scanner = new Scanner(is, encoding).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "onPostExecute");
        Gson gson = new Gson();
        DatexReply datexReply = gson.fromJson(result, DatexReply.class);
        if (datexReply != null && datexReply.signList != null) {
            Log.d(TAG, "onPostExecute. datex-II info: " + datexReply.retreiveDate + "    SignCount: " + datexReply.signList.size());
            MapActivity.datexReply = datexReply;
            MapActivity.datexReplyCnt++;
        }
    }
}
