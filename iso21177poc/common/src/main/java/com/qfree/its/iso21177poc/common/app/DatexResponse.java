package com.qfree.its.iso21177poc.common.app;

import java.net.MalformedURLException;
import java.security.cert.Certificate;

public class DatexResponse {


    public static enum Status {HTTP_COMPLETE, SUCCESS, JSON_PARSE_ERROR, HTTP_EXCEPTION, HTTP_FAILURE};

    public String errorText;
    public String peerCertHash;
    public long peerCertPsid;
    public String peerCertSsp;
    public String peerCertChain;
    public DatexReply datexReply;
    public long tickStart;
    public long tickEnd;
    public int httpResponseCode;
    public String certificateFamily;
    public String url;
    public String protocol;
    public Exception exception;
    public Status status;
    Certificate[] x509CertList;
}
