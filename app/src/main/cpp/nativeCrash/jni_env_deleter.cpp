#include <jni.h>
#include <memory>
#include "jni_env_deleter.h"

std::unique_ptr<JNIEnv, JniEnvDeleter> attachEnv(JavaVM *vm) {
    JNIEnv *env;
    jint res = vm->AttachCurrentThread(&env, nullptr);
    if (res == JNI_OK) {
        return std::unique_ptr<JNIEnv, JniEnvDeleter>(env, JniEnvDeleter{});
    } else {
        return nullptr;
    }
}