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
    // sizeof(int)=4.  sizeof(long)=8.  sizeof(ptr)=8.  __STDC__=1
    // Compiling for:
    //   arm64-v8a
    //   armeabi-v7a
    //   x86
    //   x86_64
    return env->NewStringUTF(hello.c_str());
}