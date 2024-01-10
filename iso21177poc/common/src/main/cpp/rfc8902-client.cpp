//
// Created by OlaL on 21. des. 2023.
//

#include <jni.h>
#include <string>
#include <android/log.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <errno.h>
#include <unistd.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/opensslv.h>
#include <openssl/crypto.h>

#include "http-headers.h"

static const char APPNAME[] = "rfc8902-client";

/* Managed by IANA */
enum CertificateType {
    CertificateTypeX509 = 0,
    CertificateTypeRawPublicKey = 2,
    CertificateType1609Dot2 = 3
};

#define CERT_HASH_LEN 8

#define ERR_RFC8902_PSID_MISMATCH        1000
#define ERR_SSL_READ_ERROR               1001
#define ERR_SSL_SHUTDOWN_1_ERROR         1002
#define ERR_SSL_SHUTDOWN_2_ERROR         1003
#define ERR_RFC8902_SET_SEC_ENT_ERROR    1004
#define ERR_RFC8902_PEER_PSID_NOT_FOUND  1005
#define ERR_RFC8902_PEER_CERT_INVALID    1006
#define ERR_HTTP_SOCKET_CREATE           1007
#define ERR_HTTP_HOST_DNS_ERROR          1008
#define ERR_HTTP_CONNECT_ERROR           1009
#define ERR_RFC8902_NEW                  1010
#define ERR_RFC8902_SET_TLS_V13          1011
#define ERR_RFC8902_SET_VERIFY           1012
#define ERR_RFC8902_SET_VALUES           1013
#define ERR_RFC8902_SET_FD               1014
#define ERR_RFC8902_SSL_CONNECT          1015
#define ERR_RFC8902_PRINT_1609           1016
#define ERR_SSL_RECV_MSG                 1017
#define ERR_SSL_SEND_MSG                 1018


static unsigned char      optAtOrEcCertHash[CERT_HASH_LEN] = { 0xC4, 0x3B, 0x88, 0xB2, 0x35, 0x81, 0xDD, 0x3B };
static uint64_t           optPsid = 36;
static int                optSetCertPsid = 0;
static bool               optUseAtCert = true;
static char               optSecEntHost[200] = "46.43.3.150";
static short unsigned int optSecEntPort = 3999;
static char               optServerHost[200] = "46.43.3.150";
static short unsigned int optServerPort = 8877;
static std::string        result_http_response;
static int                result_http_result_code;
static int                result_error_code;
static std::string        result_peer_cert_hash;
static long               result_peer_cert_psid;
static std::string        result_peer_cert_ssp;

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_getOpensslVersion(
        JNIEnv* env,
        jobject /* this */) {
    const char *vsn = OpenSSL_version(OPENSSL_FULL_VERSION_STRING);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "openssl lib version %s", vsn);
    return env->NewStringUTF(vsn);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_setSecurityEntity(JNIEnv* env, jobject clazz, jstring seAddress, jint sePort) {
    const char *pSeAddress = env->GetStringUTFChars (seAddress, 0);
    strncpy(optSecEntHost, pSeAddress, sizeof(optSecEntHost));
    env->ReleaseStringUTFChars (seAddress, pSeAddress);
    optSecEntPort = sePort;
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Rfc8902_setSecurityEntity %s  %d", optSecEntHost, optSecEntPort);
    return 1;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_setHttpServer(JNIEnv* env, jobject clazz, jstring serverAddress, jint serverPort) {
    const char *pServerAddress = env->GetStringUTFChars (serverAddress, 0);
    strncpy(optServerHost, pServerAddress, sizeof(optServerHost));
    env->ReleaseStringUTFChars (serverAddress, pServerAddress);
    optServerPort = serverPort;
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Rfc8902_setProxyServer %s  %d", optServerHost, optServerPort);
    return 1;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_setPsid(JNIEnv* env, jobject clazz, jlong psid) {
    optPsid = psid;
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Rfc8902_setPsid %ld", (long)optPsid);
    return 1;
}

extern "C" void ola(const char*fn,int ln,const char*txt) {
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "File %s   line %d   %s", fn, ln, txt);
}

static std::string print_hex_array(int len, const unsigned char * ptr) {
    std::string str;
    for (int i = 0; i < len; i++) {
        char sz[10];
        sprintf(sz,"%02X", ptr[i]);
        str += sz;
    }
    return str;
}

static int ssl_send_message(SSL *s, char * message, size_t message_len)
{
    int processed = 0;

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Sending [%zd] %.*s\n", message_len, (int)message_len, message);
    for (const char *start = message; start - message < (int)message_len; start += processed) {

        processed = SSL_write(s, start, message_len - (start - message));
        printf("Client SSL_write returned %d\n", processed);
        if (processed <= 0) {
            int ssl_err = SSL_get_error(s, processed);
            if (ssl_err != SSL_ERROR_WANT_READ && ssl_err != SSL_ERROR_WANT_WRITE) {
                __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"ssl_send_message failed: ssl_error=%d: ", ssl_err);
                //ERR_print_errors_fp(stderr);
                //fprintf(stderr, "\n");
            }
        }
    };

    return processed;
}

static int ssl_recv_message(SSL *s, char * buff, size_t buff_len)
{
    int processed = SSL_read(s, buff, buff_len);
    if (processed > 0) {
        // __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"SSL_read: max:%d  ret:%d\n%.*s\n", (int) buff_len, processed, processed, buff);
    } else {
        int err = SSL_get_error(s, processed);
        if (err == SSL_ERROR_ZERO_RETURN) {
            __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"SSL_read: End of file\n");
        } else {
            __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"SSL_read: Error:%d\n", err);
            result_error_code = ERR_SSL_READ_ERROR;
        }
    }
    return processed;
}

static int ssl_print_1609_status(SSL *s)
{
    printf("Information about the other side of the connection:\n");
    uint64_t psid;
    size_t ssp_len;
    uint8_t *ssp = NULL;
    unsigned char hashed_id[CERT_HASH_LEN];

    if (SSL_get_1609_psid_received(s, &psid, &ssp_len, &ssp, hashed_id) <= 0) {
        //ERR_print_errors_fp(stderr);
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_get_1609_psid_received failed\n");
        result_error_code = ERR_RFC8902_PEER_PSID_NOT_FOUND;
        return 0;
    }
    long verify_result = 0;
    if ((verify_result = SSL_get_verify_result(s)) != X509_V_OK) {
        //ERR_print_errors_fp(stderr);
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_get_verify_result failed %ld\n", verify_result);
        free(ssp);
        result_error_code = ERR_RFC8902_PEER_CERT_INVALID;
        return 0;
    }

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"   Peer verification      %ld %s\n", verify_result, verify_result == 0 ? "OK" : "FAIL");
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"   Psid used for TLS is   %llu\n", (long long unsigned int)psid);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"   SSP used for TLS are   %s\n", print_hex_array(ssp_len, ssp).c_str());
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"   Cert used for TLS is   %s\n", print_hex_array(CERT_HASH_LEN, hashed_id).c_str());
    result_peer_cert_hash = print_hex_array(CERT_HASH_LEN, hashed_id);
    result_peer_cert_psid = psid;
    result_peer_cert_ssp = print_hex_array(ssp_len, ssp);

    free(ssp);
    ssp = 0;

    if (psid != optPsid) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"   Expected PSID/AID      %lu, peer had %ld - aborting\n", (unsigned long) optPsid, (unsigned long) psid);
        result_error_code = ERR_RFC8902_PSID_MISMATCH;
        return 0;
    }

    return 1;
}

static int ssl_set_RFC8902_values(SSL *ssl, int server_support, int client_support) {
    if (!SSL_enable_RFC8902_support(ssl, server_support, client_support, optUseAtCert)) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_enable_RFC8902_support failed\n");
        //ERR_print_errors_fp(stderr);
        return 0;
    }

    if (!optUseAtCert) {
        if (!SSL_use_1609_cert_by_hash(ssl, optAtOrEcCertHash)) {
            __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_use_1609_cert_by_hash failed\n");
            //ERR_print_errors_fp(stderr);
            return 0;
        }
    }
    if (optSetCertPsid) {
        if (!SSL_use_1609_PSID(ssl, optPsid)) {
            __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_use_1609_PSID failed\n");
            //ERR_print_errors_fp(stderr);
            return 0;
        }
    }

    return 1;
}

static std::string replace(std::string subject, const std::string& search, const std::string& replace) {
    size_t pos = 0;
    while ((pos = subject.find(search, pos)) != std::string::npos) {
        subject.replace(pos, search.length(), replace);
        pos += replace.length();
    }
    return subject;
}

static bool http_get(const std::string &file_url, std::string &response_str)
{
    response_str.clear();

    int client_socket = -1;
    SSL_CTX *ssl_ctx = 0;
    SSL *ssl = 0;
    int processed = 0;
    int retval;

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Client connection to %s at port %d\n", optServerHost, optServerPort);
    client_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (client_socket < 0) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"socket failed");
        result_error_code = ERR_HTTP_SOCKET_CREATE;
        return false;
    }
    struct sockaddr_in server_addr;
    memset((char*)&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(optServerPort);
    if (inet_addr(optServerHost) == (in_addr_t)(-1)) {
        const struct hostent *he = gethostbyname(optServerHost);
        if (he == 0) {
            __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "gethostbyname failed for %s\n", optServerHost);
            result_error_code = ERR_HTTP_HOST_DNS_ERROR;
            return false;
        }
        const struct in_addr **addr_list = (const struct in_addr **) he->h_addr_list;
        server_addr.sin_addr = *addr_list[0];
    } else {
        server_addr.sin_addr.s_addr = inet_addr(optServerHost);
    }

    if (connect(client_socket, (const struct sockaddr *)&server_addr, sizeof(server_addr))) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"connect failed");
        close(client_socket);
        result_error_code = ERR_HTTP_CONNECT_ERROR;
        return false;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"TCP connected to RFC8902 proxy server.\n");

    ssl_ctx = SSL_CTX_new(TLS_client_method());
    if (!ssl_ctx) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_CTX_new failed\n");
        close(client_socket);
        result_error_code = ERR_RFC8902_NEW;
        return false;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_CTX_new success\n");

    if (!SSL_CTX_set_min_proto_version(ssl_ctx, TLS1_3_VERSION)) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_CTX_set_min_proto_version failed");
        //ERR_print_errors_fp(stderr);
        close(client_socket);
        SSL_CTX_free(ssl_ctx);
        result_error_code = ERR_RFC8902_SET_TLS_V13;
        return false;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_CTX_set_min_proto_version success");

    SSL_CTX_set_verify(ssl_ctx, SSL_VERIFY_PEER | SSL_VERIFY_FAIL_IF_NO_PEER_CERT, NULL);
    ssl = SSL_new(ssl_ctx);
    if (!ssl) {
        //ERR_print_errors_fp(stderr);
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_new failed\n");
        close(client_socket);
        SSL_CTX_free(ssl_ctx);
        result_error_code = ERR_RFC8902_SET_VERIFY;
        return false;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_CTX_set_verify success");

    if (!SSL_set_1609_sec_ent_addr(ssl, optSecEntPort, optSecEntHost)) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_set_1609_sec_ent_addr failed\n");
        //ERR_print_errors_fp(stderr);
        close(client_socket);
        SSL_free(ssl);
        SSL_CTX_free(ssl_ctx);
        result_error_code = ERR_RFC8902_SET_SEC_ENT_ERROR;
        return false;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_set_1609_sec_ent_addr success");

    int server_support = SSL_RFC8902_1609; //  | SSL_RFC8902_X509;
    int client_support = SSL_RFC8902_1609; //  | SSL_RFC8902_X509;
    if (!ssl_set_RFC8902_values(ssl, server_support, client_support)) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "ssl_set_RFC8902_values failed");
        close(client_socket);
        SSL_free(ssl);
        SSL_CTX_free(ssl_ctx);
        result_error_code = ERR_RFC8902_SET_VALUES;
        return false;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "ssl_set_RFC8902_values success");

    if (!SSL_set_fd(ssl, client_socket)) {
        ERR_print_errors_fp(stderr);
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_set_fd failed\n");
        SSL_free(ssl);
        SSL_CTX_free(ssl_ctx);
        close(client_socket);
        result_error_code = ERR_RFC8902_SET_FD;
        return false;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_set_fd success.  sd=%d", client_socket);

    if (SSL_connect(ssl) <= 0) {
        //ERR_print_errors_fp(stderr);
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "SSL_connect failed\n");
        SSL_free(ssl);
        SSL_CTX_free(ssl_ctx);
        close(client_socket);
        result_error_code = ERR_RFC8902_SSL_CONNECT;
        return false;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"SSL connected.\n");

    if (ssl_print_1609_status(ssl) <= 0) {
        SSL_free(ssl);
        SSL_CTX_free(ssl_ctx);
        close(client_socket);
        if (result_error_code == 0) result_error_code = ERR_RFC8902_PRINT_1609;
        return false;
    }

    // Send HTTP GET request
    char line[1024];
    ssize_t ret_line_len = snprintf(line, sizeof(line), "GET %s HTTP/1.1\r\nHost: %s:%d\r\n\r\n", file_url.c_str(), optServerHost, optServerPort);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Sending '%s'\n", replace(line, "\r\n", " CRLF ").c_str());
    if (ssl_send_message(ssl, line, ret_line_len) < 0) {
        SSL_free(ssl);
        SSL_CTX_free(ssl_ctx);
        close(client_socket);
        if (result_error_code == 0) result_error_code = ERR_SSL_SEND_MSG;
        return false;
    }

    // Wait for headers
    HttpHeaders headers;
    std::vector<unsigned char> remaining;
    while (!headers.is_complete()) {
        if ((processed = ssl_recv_message(ssl, line, sizeof(line))) <= 0) {
            int ssl_error = SSL_get_error(ssl, processed);
            //ERR_print_errors_fp(stderr);
            if (ssl_error == SSL_ERROR_ZERO_RETURN) {
                __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Client thinks a server finished sending data (2)\n");
                //ERR_print_errors_fp(stderr);
                break;
            }
            if (ssl_error != SSL_ERROR_WANT_READ && ssl_error != SSL_ERROR_WANT_WRITE) {
                __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Client read failed: ssl_error=%d errno=%s: \n", ssl_error, strerror(errno));
                //ERR_print_errors_fp(stderr);
                SSL_free(ssl);
                SSL_CTX_free(ssl_ctx);
                close(client_socket);
                if (result_error_code == 0) result_error_code = ERR_SSL_RECV_MSG;
                return false;
            }
        }

        remaining = headers.add_data(line, processed);
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Waiting for HTTP header, received %d bytes, Got %d surplus bytes (payload),  complete:%s\n", processed, (int)remaining.size(), headers.is_complete()?"y":"n");
    }
    response_str += std::string((const char*)remaining.data(), remaining.size());

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Reply protocol:       %s\n", headers.get_reply_protocol().c_str());
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Reply status:         %d\n", headers.get_reply_status());
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Reply content-length: %d\n", headers.get_content_length());
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Reply content-type:   %s\n", headers.get_content_type().c_str());
    result_http_result_code = headers.get_reply_status();

    int totlen = (int)remaining.size();
    if (true) {
        /* Consume all server's data */
        char buff[1000];
        int loopcnt = 0;
        while (true) {
            int len = ssl_recv_message(ssl, buff, sizeof(buff));
            if (len <= 0) break;
            totlen += len;
            loopcnt++;
            //__android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"looping len:%d  totlen:%d  loopcnt:%d\n", len, totlen, loopcnt);
            response_str += std::string(buff, len);
        }
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Client received %d bytes in %d loops(1).  ContentLen was %d bytes\n", totlen, loopcnt, headers.get_content_length());
    }

    retval = SSL_shutdown(ssl);
    if (retval < 0) {
        int ssl_err = SSL_get_error(ssl, retval);
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Client SSL_shutdown(1) failed: ssl_err=%d\n", ssl_err);
        //ERR_print_errors_fp(stderr);
        SSL_free(ssl);
        SSL_CTX_free(ssl_ctx);
        close(client_socket);
        result_error_code = ERR_SSL_SHUTDOWN_1_ERROR;
        return false;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Client shut down TLS session.\n");

    // Wait for the payload
    if (retval != 1) {
        /* Consume all server's data to access the server's shutdown */
        char buff[1000];
        int loopcnt = 0;
        while (true) {
            int len = ssl_recv_message(ssl, buff, sizeof(buff));
            if (len <= 0) break;
            totlen += len;
            loopcnt++;
            //__android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"looping len:%d  totlen:%d  loopcnt:%d\n", len, totlen, loopcnt);
            response_str += std::string(buff, len);
        }
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Client received %d bytes in %d loops(2).  ContentLen was %d bytes\n", totlen, loopcnt, headers.get_content_length());

        retval = SSL_shutdown(ssl);
        if (retval != 1) {
            int ssl_err = SSL_get_error(ssl, retval);
            __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Waiting for server shutdown using SSL_shutdown(2) failed: ssl_err=%d\n", ssl_err);
            SSL_free(ssl);
            SSL_CTX_free(ssl_ctx);
            close(client_socket);
            result_error_code = ERR_SSL_SHUTDOWN_2_ERROR;
            return false;
        }
    }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Client thinks a server shut down the TLS session.\n");

    SSL_free(ssl);
    SSL_CTX_free(ssl_ctx);
    close(client_socket);

    return true;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_httpGet(JNIEnv* env, jobject clazz, jstring strFileUrl) {
    result_http_response.clear();
    result_http_result_code = 0;
    result_error_code = 0;
    result_peer_cert_hash = "";
    result_peer_cert_psid = 0;
    result_peer_cert_ssp = "";
    const char *pFileUrl = env->GetStringUTFChars (strFileUrl, 0);
    std::string sFileUrl = pFileUrl;
    // Release memory used to hold ASCII representation.
    env->ReleaseStringUTFChars (strFileUrl, pFileUrl);
    int ret = http_get(sFileUrl, result_http_response);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "client c-code done %d", ret);
    return ret;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_httpGetResponse(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(result_http_response.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_getHttpResultCode(
        JNIEnv* env,
        jobject /* this */) {
    return result_http_result_code;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_getErrorCode(
        JNIEnv* env,
        jobject /* this */) {
    return result_error_code;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_getPeerCertHash(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(result_peer_cert_hash.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_getPeerCertSsp(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(result_peer_cert_ssp.c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfree_its_iso21177poc_common_app_Rfc8902_getPeerCertPsid(
        JNIEnv* env,
        jobject /* this */) {
    return result_peer_cert_psid;
}