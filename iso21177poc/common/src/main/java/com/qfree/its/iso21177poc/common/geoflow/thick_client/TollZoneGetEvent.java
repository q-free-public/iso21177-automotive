package com.qfree.its.iso21177poc.common.geoflow.thick_client;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.qfree.geoflow.toll.api.GeoFlowFactory;
import com.qfree.geoflow.toll.api.GeoFlowZonePackage;

import java.time.LocalDateTime;

public class TollZoneGetEvent {
    private LocalDateTime date;
    private int responseCode;
    private String body;
    private GeoFlowZonePackage pkg;

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void convertToGeoFlowZonePackage(){
        try {
            Gson gson = GeoFlowFactory.createGson();
            this.pkg = gson.fromJson(this.body, GeoFlowZonePackage.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    public GeoFlowZonePackage getPkg() {
        return pkg;
    }
}
