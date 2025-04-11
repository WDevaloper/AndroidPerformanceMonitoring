#include <jni.h>
#include <android/log.h>
#include "native_crash_handler.h"

JavaVM *_vm;
//需要动态注册native方法的 Java类名   当前native_crash_jni_bridge.cpp是所有JNI的代理类
static const char *className = "com/github/andcrash/nativecrash/NativeCrash";
static const char *callbackSignature = "(Ljava/lang/String;Ljava/lang/String;Lcom/github/andcrash/nativecrash/NativeCrashCallback;)V";

extern "C"
JNIEXPORT void JNICALL
testCrash(JNIEnv *env, jclass clazz) {
    //    __builtin_trap();
    abort();
}

extern "C"
JNIEXPORT void JNICALL
InitCrashHandler(JNIEnv *env,
                 jclass clazz,
                 jstring log_dir,
                 jstring version,
                 jobject callback) {
    const char *path = env->GetStringUTFChars(log_dir, nullptr);
    const char *ver = env->GetStringUTFChars(version, nullptr);
    CrashHandler::Init(env, path, callback);
    CrashHandler::SetVersion(ver);
    env->ReleaseStringUTFChars(log_dir, path);
    env->ReleaseStringUTFChars(version, ver);
}


extern "C"
JNIEXPORT void JNICALL
SetVersion(JNIEnv *env,
           jclass clazz,
           jstring version) {
    CrashHandler::SetVersion(env->GetStringUTFChars(version, nullptr));
}
extern "C"
JNIEXPORT jint JNICALL
DeleteCrashLogFile(JNIEnv *env, jclass clazz,
                   jstring log_path) {
    const char *path = env->GetStringUTFChars(log_path, nullptr);
    int result = CrashHandler::deleteLogFile(path);
    env->ReleaseStringUTFChars(log_path, path);
    return result;
}

//需要动态注册的native方法数组
static const JNINativeMethod methods[] = {{"testCrash",          "()V",                   (void *) testCrash},
                                          {"initCrashHandler",   callbackSignature,       (void *) InitCrashHandler},
                                          {"SetVersion",         "(Ljava/lang/String;)V", (void *) SetVersion},
                                          {"deleteCrashLogFile", "(Ljava/lang/String;)I", (void *) DeleteCrashLogFile}

};


//调用System.loadLibrary()函数时， 内部就会去查找so中的 JNI_OnLoad 函数，如果存在此函数则调用。
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    _vm = vm;

    //获得JNIEnv（线程相关的）
    JNIEnv *env = JNI_OK;
    int r = vm->GetEnv((void **) &env, JNI_VERSION_1_6);

    //小于0失败，等于0成功
    if (r != JNI_OK) {
        return JNI_ERR;
    }
    jclass registerClass = env->FindClass(className);

    //注册Native   参数3：方法数量
    env->RegisterNatives(registerClass, methods, sizeof(methods) / sizeof(JNINativeMethod));
    env->DeleteLocalRef(registerClass);
    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env = JNI_OK;
    int r = vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (r == JNI_OK) {
        jclass registerClass = env->FindClass(className);
        env->UnregisterNatives(registerClass);
    }
}