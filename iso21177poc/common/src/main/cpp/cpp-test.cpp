// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("common");
//    }
//

#include <jni.h>
#include <string>
#include <sys/utsname.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfree_its_iso21177poc_common_app_DatexFetchHttp_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    static int cnt = 1;
    std::string hello = "Hello from C++ " + std::to_string(++cnt);
    int ii = 0;
    long ll = 0;
    void *p = &ll;
    hello += ".  sizeof(int)=" + std::to_string(sizeof(ii));
    hello += ".  sizeof(long)=" + std::to_string(sizeof(ll));
    hello += ".  sizeof(ptr)=" + std::to_string(sizeof(p));
    hello += ".  __STDC__="  + std::to_string(__STDC__);
    hello += ".  OML_SYM="  + std::to_string(OML_SYM);
#ifdef NDEBUG
    hello += ". NDEBUG";
#endif
    // sizeof(int)=4.  sizeof(long)=8.  sizeof(ptr)=8.  __STDC__=1
    // Compiling for:
    //   arm64-v8a
    //   armeabi-v7a
    //   x86
    //   x86_64

    // This is fine on x86 and arm64, but gives warnings on x86 and arm-32.
//    unsigned long ul = 10000000000000000000UL;
//    unsigned long long ull = 10000000000000000000ULL;

    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfree_its_iso21177poc_common_app_DatexFetchHttp_unameRelease(
        JNIEnv* env,
        jobject /* this */) {
    struct utsname buf;
    memset(&buf, 0, sizeof(buf));
    int ret = uname(&buf);
    if (ret == 0) {
        return env->NewStringUTF(buf.release);
    } else {
        return env->NewStringUTF("uname error");
    }
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_qfree_its_iso21177poc_common_app_DatexFetchHttp_unameVersion(
        JNIEnv* env,
        jobject /* this */) {
    struct utsname buf;
    memset(&buf, 0, sizeof(buf));
    int ret = uname(&buf);
    if (ret == 0) {
        return env->NewStringUTF(buf.version);
    } else {
        return env->NewStringUTF("uname error");
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfree_its_iso21177poc_common_app_DatexFetchHttp_unameMachine(
        JNIEnv* env,
        jobject /* this */) {
    struct utsname buf;
    memset(&buf, 0, sizeof(buf));
    int ret = uname(&buf);
    if (ret == 0) {
        return env->NewStringUTF(buf.machine);
    } else {
        return env->NewStringUTF("uname error");
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfree_its_iso21177poc_common_app_DatexFetchHttp_testParams(JNIEnv* env, jobject clazz, jstring strParam, jlong i) {
    const char *pParam = env->GetStringUTFChars (strParam, 0);
    std::string sParam = pParam;
    // Release memory used to hold ASCII representation.
    env->ReleaseStringUTFChars (strParam, pParam);
    sParam = "testparam:  str='" + sParam + "'  i=" + std::to_string(i);
    return env->NewStringUTF(sParam.c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfree_its_iso21177poc_common_app_DatexFetchHttp_add(JNIEnv* env, jobject clazz, jlong a, jlong b) {
    return a + b;
}

