package com.qfree.its.iso21177poc.common.app;

public class Rfc8902 {
    static {
        System.loadLibrary("common");
    }

    public native String getOpensslVersion();
    public native int setSecurityEntity(String seEntAddress, int seEntPort);
    public native int setHttpServer(String serverAddress, int serverPort);
    public native int httpGet(String fileUrl);
    public native String httpGetResponse();
}
