package com.qfree.its.iso21177poc.common.geoflow.thick_client;

import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.qfree.geoflow.toll.api.GeoFlowFactory;
import com.qfree.geoflow.toll.api.GeoFlowInvoiceItem;
import com.qfree.geoflow.toll.api.GeoFlowInvoiceSummary;
import com.qfree.geoflow.toll.api.GeoFlowUtils;
import com.qfree.its.iso21177poc.common.geoflow.GeoFlowSQLiteDb;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogEvents;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class GeoFlowTollingUtils {
    private final static String TAG = GeoFlowInvoiceSummary.class.getSimpleName();


    public static GeoFlowInvoiceSummary createInvoiceSummary(GeoFlowSQLiteDb sqLiteDb, BillingPeriod period,
                                                             PrivateVaultImpl vault) throws Exception{
        Log.d(TAG, "createInvoiceSummary: ");
        byte[] firstHash = null;
        double sum = 0.0;
        String currency = "currency";
        int itemCnt = 0;
        ArrayList<GeoFlowInvoiceItem> items = sqLiteDb.getInvoiceItems();
        for (GeoFlowInvoiceItem item : items) {
            if (firstHash == null){
                firstHash = item.hash;
            }
            currency = item.currency;
            sum += item.amount;
            itemCnt++;
        }

        GeoFlowInvoiceSummary invoiceSummary = new GeoFlowInvoiceSummary();
        if (firstHash == null){
            //Nothing to do
        } else {
            invoiceSummary.itsStationId = vault.getVehicle().licensePlate;
            invoiceSummary.userName = vault.getUser().name;
            invoiceSummary.hash = firstHash;
            invoiceSummary.fromDate = period.periodStart;
            invoiceSummary.toDate = period.periodEnd;
            invoiceSummary.amount = sum;
            invoiceSummary.currency = currency;
            invoiceSummary.signature = vault.createSignature(invoiceSummary.getBinary());
            invoiceSummary.seqNum = sqLiteDb.saveInvoiceSummary(invoiceSummary);
//            submitToServer(invoiceSummary);
            sqLiteDb.deleteInvoiceItems();
            FileLogger.logEvent(LogEvents.INVOICE_SUMMARY, String.format(Locale.ROOT, "%d;%s;%s;%.2f;%s",
                    invoiceSummary.seqNum, invoiceSummary.fromDate, invoiceSummary.toDate,
                    invoiceSummary.amount, invoiceSummary.currency));
        }
        return invoiceSummary;
    }

    public static void submitToServer(String url, String uploadTopic, GeoFlowInvoiceSummary invoiceSummary,
                                      Handler handler, long timeout) {
        Gson gson = GeoFlowFactory.createGson();
        byte[] content = gson.toJson(invoiceSummary).getBytes(StandardCharsets.UTF_8);
        uploadTopic += "/"+ GeoFlowUtils.bin2hex(invoiceSummary.hash);
        HashMap<String, String> requestProps = new HashMap<>();
        requestProps.put("content-type", "application/json");
        requestProps.put("X-Qfree-ItsStation", invoiceSummary.itsStationId);
        requestProps.put("X-Qfree-Hostname", invoiceSummary.userName);
        ThickClientPostThread thickClientPostThread = new ThickClientPostThread(url, content,
                requestProps, uploadTopic, handler, timeout);
        thickClientPostThread.start();
    }
}
