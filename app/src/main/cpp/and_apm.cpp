#include <android/log.h>
#include <jni.h>
#include "and_apm.h"

namespace apm {
    long AndApm::init() {
        __android_log_print(ANDROID_LOG_ERROR, "AndCrash", "init");
        auto *apmPtr = new AndApm();
        return reinterpret_cast<long>(apmPtr);
    }

    void AndApm::start() {
        __android_log_print(ANDROID_LOG_ERROR, "AndCrash", "start");
    }

    void AndApm::stop() {
        __android_log_print(ANDROID_LOG_ERROR, "AndCrash", "stop");
    }

    void AndApm::destroy(long ptr) {
        __android_log_print(ANDROID_LOG_ERROR, "AndCrash", "destroy");
        delete reinterpret_cast<AndApm *>(ptr);
    }
}