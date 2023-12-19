package com.qfree.its.iso21177poc.common.app;

public class DatexVmsSign {
    public String error;
    public double latitude;
    public double longitude;
    public String pubTime;
    public String id;
    public String signId;
    public int vmsIndex;
    public boolean isBlank;
    public String imageData;
    public String imageFormat;
    public boolean hasSpeed;
    public int speedLimitValue;

    public String toString() {
        String ret = "";
        ret += String.format("%9.6f,%9.6f", longitude, latitude);
        ret += "  " + pubTime;
        ret += "  " + id;
        ret += "  " + signId;
        ret += "  " + vmsIndex;
        ret += "  isBlank:" + isBlank;
        if (imageData != null) {
            ret += "  Image:" + imageData.length();
            ret += "(" + imageFormat + ")";
        } else {
            ret += " no-image    ";
        }
        ret += "  hasSpeed:" + hasSpeed;
        ret += "  " + speedLimitValue;
        ret += "  err:" + error;
        return ret;
    }
}
