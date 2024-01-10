package com.qfree.its.iso21177poc.common.app;

public class Rfc8902 {
    static {
        System.loadLibrary("common");
    }

    public native String getOpensslVersion();
    public native int setSecurityEntity(String seEntAddress, int seEntPort);
    public native int setHttpServer(String serverAddress, int serverPort);
    public native int setPsid(long psid);
    public native int httpGet(String fileUrl);
    public native String httpGetResponse();
    public native int getHttpResultCode();
    public native int getErrorCode();
    public native String getPeerCertHash();
    public native String getPeerCertSsp();
    public native long getPeerCertPsid();

    // Constants are aligned with rfc8902-client.cpp
    public static final int ERR_RFC8902_PSID_MISMATCH = 1000;
    public static final int  ERR_SSL_READ_ERROR = 1001;
    public static final int  ERR_SSL_SHUTDOWN_1_ERROR = 1002;
    public static final int  ERR_SSL_SHUTDOWN_2_ERROR = 1003;
    public static final int  ERR_RFC8902_SET_SEC_ENT_ERROR = 1004;
    public static final int  ERR_RFC8902_PEER_PSID_NOT_FOUND = 1005;
    public static final int  ERR_RFC8902_PEER_CERT_INVALID = 1006;
    public static final int  ERR_HTTP_SOCKET_CREATE = 1007;
    public static final int  ERR_HTTP_HOST_DNS_ERROR = 1008;
    public static final int  ERR_HTTP_CONNECT_ERROR = 1009;
    public static final int  ERR_RFC8902_NEW = 1010;
    public static final int  ERR_RFC8902_SET_TLS_V13 = 1011;
    public static final int  ERR_RFC8902_SET_VERIFY = 1012;
    public static final int  ERR_RFC8902_SET_VALUES = 1013;
    public static final int  ERR_RFC8902_SET_FD = 1014;
    public static final int  ERR_RFC8902_SSL_CONNECT = 1015;
    public static final int  ERR_RFC8902_PRINT_1609 = 1016;
    public static final int  ERR_SSL_RECV_MSG = 1017;
    public static final int  ERR_SSL_SEND_MSG = 1018;


    public String getErrorCodeStr(int errno) {
        switch (errno) {
            case 0:                                 return "no error";
            case ERR_RFC8902_PSID_MISMATCH:         return "ERR_RFC8902_PSID_MISMATCH";
            case ERR_SSL_READ_ERROR:                return "ERR_SSL_READ_ERROR";
            case ERR_SSL_SHUTDOWN_1_ERROR:          return "ERR_SSL_SHUTDOWN_1_ERROR";
            case ERR_SSL_SHUTDOWN_2_ERROR:          return "ERR_SSL_SHUTDOWN_2_ERROR";
            case ERR_RFC8902_SET_SEC_ENT_ERROR:     return "ERR_RFC8902_SET_SEC_ENT_ERROR";
            case ERR_RFC8902_PEER_PSID_NOT_FOUND:   return "ERR_RFC8902_PEER_PSID_NOT_FOUND";
            case ERR_RFC8902_PEER_CERT_INVALID:     return "ERR_RFC8902_PEER_CERT_INVALID";
            case ERR_HTTP_SOCKET_CREATE:            return "ERR_HTTP_SOCKET_CREATE";
            case ERR_HTTP_HOST_DNS_ERROR:           return "ERR_HTTP_HOST_DNS_ERROR";
            case ERR_HTTP_CONNECT_ERROR:            return "ERR_HTTP_CONNECT_ERROR";
            case ERR_RFC8902_NEW:                   return "ERR_RFC8902_NEW";
            case ERR_RFC8902_SET_TLS_V13:           return "ERR_RFC8902_SET_TLS_V13";
            case ERR_RFC8902_SET_VERIFY:            return "ERR_RFC8902_SET_VERIFY";
            case ERR_RFC8902_SET_VALUES:            return "ERR_RFC8902_SET_VALUES";
            case ERR_RFC8902_SET_FD:                return "ERR_RFC8902_SET_FD";
            case ERR_RFC8902_SSL_CONNECT:           return "ERR_RFC8902_SSL_CONNECT";
            case ERR_RFC8902_PRINT_1609:            return "ERR_RFC8902_PRINT_1609";
            case ERR_SSL_RECV_MSG:                  return "ERR_SSL_RECV_MSG";
            case ERR_SSL_SEND_MSG:                  return "ERR_SSL_SEND_MSG";
        }
        return "unknown";
    }
}
