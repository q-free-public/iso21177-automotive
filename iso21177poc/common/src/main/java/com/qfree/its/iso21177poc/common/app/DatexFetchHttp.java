package com.qfree.its.iso21177poc.common.app;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.security.auth.x500.X500Principal;

public class DatexFetchHttp extends AsyncTask<Handler, Void, String> {
    final public static String TAG = "DatexFetchHttp";
    public static enum Protocol {RFC8902, ISO21177, HTTP, HTTPS};

    public static long           optPsid = 36;
    public static byte[]         optSsp = new byte[0];
    public static String         optSecEntHost = "46.43.3.150";
    public static int            optSecEntPort = 3999;
    public static String         optHttpServerHost = "46.43.3.150";
    public static int            optHttpServerPort = 8877;
    public static Protocol       optProtocol = Protocol.HTTPS;

    private Handler mapHandler;
    private DatexResponse datexResponse;

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
    protected String doInBackground(Handler ... mapHandler) {
        Log.d(TAG, "doInBackground");

        this.mapHandler = mapHandler[0];
        this.datexResponse = new DatexResponse();

        testJni();

        switch (optProtocol) {
            case HTTP:
                return do_http("its1.q-free.com", "/geoserver/all.json");
            case HTTPS:
                return do_https("its1.q-free.com", "/geoserver/all.json");
                // return do_https("its1.q-free.com", "/geoserver/all.speed");
            case RFC8902:
                return do_rfc8902("/datex-all.json");
            case ISO21177:
                return do_iso21177("/datex-all.json");
        }

        datexResponse.protocol = "Unknown: " + optProtocol;
        datexResponse.errorText = "Protocol not selected";
        return null;
    }

    private String do_iso21177(String file) {
        datexResponse.url = file;
        datexResponse.protocol = "ISO21177";
        datexResponse.certificateFamily = "IEEE1609";
        datexResponse.tickStart = System.currentTimeMillis();
        datexResponse.tickEnd = System.currentTimeMillis();
        datexResponse.status = null;
        datexResponse.errorText = "Not implemented";

        return null;
    }

    private String do_http(String host, String file) {
        try {
            URL url = new URL("http://" + host + file);
            datexResponse.url = url.toString();
            datexResponse.protocol = "HTTP";
            datexResponse.certificateFamily = "Unknown";
            datexResponse.tickStart = System.currentTimeMillis();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Log the server response code
            int responseCode = connection.getResponseCode();
            datexResponse.httpResponseCode = responseCode;
            Log.d(TAG, "Server responded with: " + responseCode);

            // And if the code was HTTP_OK then parse the contents
            if (responseCode == HttpURLConnection.HTTP_OK) {

                // Convert request content to string
                InputStream is = connection.getInputStream();
                String content = convertInputStream(is, "UTF-8");
                is.close();
                datexResponse.tickEnd = System.currentTimeMillis();
                datexResponse.status = DatexResponse.Status.HTTP_COMPLETE;
                datexResponse.certificateFamily = "No security";

                Log.d(TAG, "content size: " + content.length());
                return content;
            } else {
                datexResponse.status = DatexResponse.Status.HTTP_FAILURE;
                return null;
            }

        } catch (Exception e) {
            Log.d(TAG, "content error");
            datexResponse.status = DatexResponse.Status.HTTP_EXCEPTION;
            datexResponse.exception = e;
            e.printStackTrace();
            return null;
        }
    }

    private String do_https(String host, String file) {
        try {
            URL url = new URL("https://" + host + file);
            datexResponse.url = url.toString();
            datexResponse.protocol = "HTTPS";
            datexResponse.certificateFamily = "Unknown";
            datexResponse.tickStart = System.currentTimeMillis();

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            // Log the server response code
            int responseCode = connection.getResponseCode();
            datexResponse.httpResponseCode = responseCode;
            Log.d(TAG, "Server responded with: " + responseCode);

            // And if the code was HTTP_OK then parse the contents
            if (responseCode == HttpURLConnection.HTTP_OK) {

                datexResponse.x509CertList = connection.getServerCertificates();
                if (datexResponse.x509CertList != null) {
                    for (Certificate currCert : datexResponse.x509CertList) {
                        Log.d(TAG,"Certificate Type : " + currCert.getType());
                        Log.d(TAG,"Certificate Public Key Algorithm : " + currCert.getPublicKey().getAlgorithm());
                        Log.d(TAG,"Certificate Public Key Format : " + currCert.getPublicKey().getFormat());
                        if (currCert instanceof X509Certificate) {
                            X509Certificate x509cert = (X509Certificate) currCert;
                            Log.d(TAG,"Certificate Issuer:  " + x509cert.getIssuerDN());
                            Log.d(TAG,"Certificate Subject: " + x509cert.getSubjectDN());
                            datexResponse.certificateFamily = "X.509";
                        }
                    }
                }

                // Convert request content to string
                InputStream is = connection.getInputStream();
                String content = convertInputStream(is, "UTF-8");
                is.close();
                datexResponse.tickEnd = System.currentTimeMillis();
                datexResponse.status = DatexResponse.Status.HTTP_COMPLETE;

                Log.d(TAG, "content size: " + content.length());
                return content;
            } else {
                datexResponse.status = DatexResponse.Status.HTTP_FAILURE;
                return null;
            }

        } catch (Exception e) {
            Log.d(TAG, "content error");
            datexResponse.status = DatexResponse.Status.HTTP_EXCEPTION;
            datexResponse.exception = e;
            e.printStackTrace();
            return null;
        }
    }

    private String do_rfc8902(String url_file) {
        datexResponse.url = url_file;
        datexResponse.protocol = "RFC8902";
        datexResponse.certificateFamily = "IEEE1609";
        datexResponse.tickStart = System.currentTimeMillis();

        Rfc8902 rfc8902 = new Rfc8902();
        Log.d(TAG, "C++ OpensslVersion: " + rfc8902.getOpensslVersion());
        rfc8902.setSecurityEntity(optSecEntHost, optSecEntPort);
        rfc8902.setHttpServer(optHttpServerHost, optHttpServerPort);
        rfc8902.setPsid(optPsid);
        rfc8902.setSsp(optSsp);
        int ret = rfc8902.httpGet(url_file); // 200.text");
        datexResponse.tickEnd = System.currentTimeMillis();
        datexResponse.httpResponseCode = rfc8902.getHttpResultCode();
        int errorCode = rfc8902.getErrorCode();
        datexResponse.errorText = rfc8902.getErrorCodeStr(errorCode);
        datexResponse.peerCertHash = rfc8902.getPeerCertHash();
        datexResponse.peerCertPsid = rfc8902.getPeerCertPsid();
        datexResponse.peerCertSsp = rfc8902.getPeerCertSsp();
        datexResponse.peerCertChain = rfc8902.getPeerCertChain();
        Log.d(TAG, String.format("C++ RFC8902  ret=%d  Time used %.3f sec   HttpResultCode=%d  ErrorCode=%d=%s", ret, (datexResponse.tickEnd - datexResponse.tickStart) / 1000.0, datexResponse.httpResponseCode, errorCode, datexResponse.errorText));
        Log.d(TAG, String.format("C++ RFC8902  Peer Cert hash: %s", datexResponse.peerCertHash));
        Log.d(TAG, String.format("C++ RFC8902  Peer Cert PSID: %d", datexResponse.peerCertPsid));
        Log.d(TAG, String.format("C++ RFC8902  Peer Cert SSP:  %s", datexResponse.peerCertSsp));
        if (ret == 0) {
            datexResponse.status = DatexResponse.Status.HTTP_FAILURE;
            return null;
        }
        datexResponse.status = DatexResponse.Status.HTTP_COMPLETE;
        String responseText = rfc8902.httpGetResponse();
        Log.d(TAG, "C++ RFC8902  text.len: " + responseText.length() + "\r\n" + responseText.substring(0, Math.min(100, responseText.length())));
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
            Log.d(TAG, "onPostExecute. result from HTTP was null - do nothing");
            datexResponse.status = DatexResponse.Status.HTTP_FAILURE;
        } else {
            try {
                Gson gson = new Gson();
                DatexReply datexReply = gson.fromJson(result, DatexReply.class);
                if (datexReply != null && datexReply.signList != null) {
                    Log.d(TAG, "onPostExecute. datex-II info: " + datexReply.retreiveDate + "    SignCount: " + datexReply.signList.size());
                    datexResponse.status = DatexResponse.Status.SUCCESS;
                    datexResponse.datexReply = datexReply;
                    mapHandler.sendMessage(Message.obtain(mapHandler, MapHandler.MSG_DATEX_OK, datexResponse));
                    return;
                } else {
                    datexResponse.status = DatexResponse.Status.JSON_PARSE_ERROR;
                    Log.d(TAG, "onPostExecute. JSON failed");
                }
            } catch (JsonSyntaxException e) {
                datexResponse.status = DatexResponse.Status.JSON_PARSE_ERROR;
                datexResponse.exception = e;
                Log.d(TAG, "onPostExecute. JSON decode error: " + e.getClass().getName() + ": " + e.getMessage());
            }
        }

        mapHandler.sendMessage(Message.obtain(mapHandler, MapHandler.MSG_DATEX_ERROR, datexResponse));
    }

    public static void setSsp(String hex) throws Exception {
        int len = hex.length();
        if (len % 2 != 0)
            throw new Exception("Odd number of chars: len=" + len + "  str='" + hex + "'");

        optSsp = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            optSsp[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
        }
    }

}
