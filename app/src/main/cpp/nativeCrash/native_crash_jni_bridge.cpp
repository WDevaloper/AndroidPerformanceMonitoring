#include <jni.h>
#include <android/log.h>
#include "native_crash_handler.h"

JavaVM *_vm;

// 修改回调设置逻辑，保存全局引用
struct CallbackContext {
    JavaVM *vm;
    jobject callback; // 全局引用
};

//在jni中使用的Java_com_hellondk_NDKTool_test来进行与java方法的匹配，这种方式我们称之为静态注册。
extern "C"
JNIEXPORT void JNICALL
Java_com_github_crash_crash_cnative_NativeCrash_initCrashHandler(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jstring log_dir,
                                                                 jstring version,
                                                                 jobject callback) {

    const char *path = env->GetStringUTFChars(log_dir, nullptr);
    const char *ver = env->GetStringUTFChars(version, nullptr);
    CrashHandler::Init(path);
    CrashHandler::SetVersion(ver);



    // 设置 Crash 回调
    CrashHandler::SetCrashCallback([callback, env](const std::string &logPath) {
        jclass callbackClass = env->GetObjectClass(callback);
        jmethodID onCrashMethod = env->GetMethodID(callbackClass, "onCrash",
                                                   "(Ljava/lang/String;)V");
        jstring logPathStr = env->NewStringUTF(logPath.c_str());
        if (logPathStr != nullptr) {
            env->CallVoidMethod(callback, onCrashMethod, logPathStr);
            env->DeleteLocalRef(logPathStr);
            env->DeleteLocalRef(callbackClass);
        } else {
            __android_log_write(ANDROID_LOG_ERROR, "NativeCrash",
                                "Failed to create logPath string in callback");
        }
    });

    env->ReleaseStringUTFChars(log_dir, path);
    env->ReleaseStringUTFChars(version, ver);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_crash_crash_cnative_NativeCrash_SetVersion(JNIEnv *env, jclass clazz,
                                                           jstring version) {
    CrashHandler::SetVersion(env->GetStringUTFChars(version, nullptr));
}
extern "C"
JNIEXPORT void JNICALL
Java_com_github_crash_crash_cnative_NativeCrash_setCallback(JNIEnv *env, jclass clazz,
                                                            jobject callback) {

    CrashHandler::SetCrashCallback(
            [context = new CallbackContext{_vm, env->NewGlobalRef(callback)}](
                    const std::string &logPath) {
                JNIEnv *env;
                context->vm->AttachCurrentThread(&env, nullptr);

                jclass callbackClass = env->GetObjectClass(context->callback);
                jmethodID onCrashMethod = env->GetMethodID(callbackClass, "onCrash",
                                                           "(Ljava/lang/String;)V");
                jstring logPathStr = env->NewStringUTF(logPath.c_str());
                if (logPathStr) {
                    env->CallVoidMethod(context->callback, onCrashMethod, logPathStr);
                    env->DeleteLocalRef(logPathStr);
                }
                env->DeleteLocalRef(callbackClass);
                context->vm->DetachCurrentThread();

                // 清理全局引用（根据实际生命周期管理）
                env->DeleteGlobalRef(context->callback);
                delete context;
            });
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