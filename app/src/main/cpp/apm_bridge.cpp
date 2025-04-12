#include <jni.h>
#include "and_apm.h"

//这个文件相当于中介，介于C/C++和Java之间进行通信

JNIEXPORT jlong JNICALL
init(JNIEnv *env, jclass thiz) {
    return apm::AndApm::init();
}

JNIEXPORT void JNICALL
start(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *apm = (apm::AndApm *) ptr;
    apm->start();
}

JNIEXPORT void JNICALL
stop(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *apm = (apm::AndApm *) ptr;
    apm->stop();
}

JNIEXPORT void JNICALL
destroy(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *apm = (apm::AndApm *) ptr;
    long cppPtr = static_cast<long>(ptr);
    apm->destroy(cppPtr);
}

static const JNINativeMethod methods[] = {{"nativeStart",   "(J)V", (void *) start},
                                          {"nativeStop",    "(J)V", (void *) stop},
                                          {"nativeInit",    "()J",  (void *) init},
                                          {"nativeDestroy", "(J)V", (void *) destroy}};

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = JNI_OK;
    int r = vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (r != JNI_OK) {
        return JNI_ERR;
    }
    jclass registerClass = env->FindClass("com/github/crash/AndAPM");

    //注册Native   参数3：方法数量
    env->RegisterNatives(registerClass, methods, sizeof(methods) / sizeof(JNINativeMethod));
    env->DeleteLocalRef(registerClass);
    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env = JNI_OK;
    int r = vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (r == JNI_OK) {
        jclass registerClass = env->FindClass("com/github/crash/AndAPM");
        env->UnregisterNatives(registerClass);
    }
}