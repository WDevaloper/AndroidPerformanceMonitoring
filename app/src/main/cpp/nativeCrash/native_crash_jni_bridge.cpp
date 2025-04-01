#include <jni.h>
#include "native_crash_handler.h"

extern "C" JNIEXPORT void JNICALL
Java_com_github_crash_crash_cnative_NativeCrash_initCrashHandler(
        JNIEnv *env,
        jclass clazz,
        jstring logPath,
        jstring version) {

    const char *path = env->GetStringUTFChars(logPath, nullptr);
    const char *ver = env->GetStringUTFChars(version, nullptr);

    CrashHandler::Init(path);
    CrashHandler::SetVersionInfo(ver);

    env->ReleaseStringUTFChars(logPath, path);
    env->ReleaseStringUTFChars(version, ver);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_github_crash_crash_cnative_NativeCrash_testCrash(JNIEnv *env, jclass clazz) {
    // 触发SIGSEGV
    volatile int *ptr = nullptr;
    *ptr = 42;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_github_crash_crash_cnative_NativeCrash_setCrashLogPathNative(JNIEnv *env, jclass clazz,
                                                                      jstring path) {
    CrashHandler::SetLogDir(env->GetStringUTFChars(path, nullptr));
}