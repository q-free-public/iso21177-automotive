package com.qfree.its.iso21177poc.common.app;

import java.util.ArrayList;

public class DatexReply {
    public String                   retreiveDate;
    public String                   url;
    public ArrayList<DatexVmsSign> signList;

    public String toString() {
        String ret = "";
        ret += "retreiveDate: " + retreiveDate + "\r\n";
        ret += "url:          " + url + "\r\n";
        ret += "signList.cnt: " + signList.size() + "\r\n";
        for (DatexVmsSign s : signList) {
            ret += s + "\r\n";
        }
        return ret;
    }
}
