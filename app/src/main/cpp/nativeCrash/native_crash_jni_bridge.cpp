#include <jni.h>
#include <android/log.h>
#include "native_crash_handler.h"

JavaVM *_vm;

//在jni中使用的Java_com_hellondk_NDKTool_test来进行与java方法的匹配，这种方式我们称之为静态注册。
extern "C"
JNIEXPORT void JNICALL
Java_com_github_crash_crash_cnative_NativeCrash_initCrashHandler(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jstring log_dir,
                                                                 jstring version) {

    const char *path = env->GetStringUTFChars(log_dir, nullptr);
    const char *ver = env->GetStringUTFChars(version, nullptr);
    CrashHandler::Init(path);
    CrashHandler::SetVersion(ver);
    env->ReleaseStringUTFChars(log_dir, path);
    env->ReleaseStringUTFChars(version, ver);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_crash_crash_cnative_NativeCrash_SetVersion(JNIEnv *env, jclass clazz,
                                                           jstring version) {
    CrashHandler::SetVersion(env->GetStringUTFChars(version, nullptr));
}

// 在适当位置添加全局引用清理
extern "C"
JNIEXPORT void JNICALL
Java_com_github_crash_crash_cnative_NativeCrash_nativeCrashCleanup(JNIEnv *env, jclass clazz) {
}

// ---------动态注册-----------
extern "C"
JNIEXPORT void JNICALL
testCrash(JNIEnv *env, jclass clazz) {
    //    __builtin_trap();
    abort();
}

//需要动态注册的native方法数组
static const JNINativeMethod methods[] = {{"testCrash", "()V", (void *) testCrash}};


//需要动态注册native方法的 Java类名
static const char *className = "com/github/crash/crash/cnative/NativeCrash";

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