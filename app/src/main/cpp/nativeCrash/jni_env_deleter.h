#include <jni.h>
#include <memory>

struct JniEnvDeleter {
    void operator()(JNIEnv *env) {
        JavaVM *vm;
        env->GetJavaVM(&vm);
        vm->DetachCurrentThread();
    }
};

std::unique_ptr<JNIEnv, JniEnvDeleter> attachEnv(JavaVM *vm);