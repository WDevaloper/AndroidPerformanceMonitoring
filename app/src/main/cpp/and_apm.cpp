#include <android/log.h>
#include <jni.h>
#include "and_apm.h"

namespace apm {
    long AndApm::init() {
        auto *apmPrt = new apm::AndApm();
        return (long) apmPrt;
    }

    void AndApm::start() {
        __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "start");
    }

    void AndApm::stop() {
        __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "stop");
    }

    void AndApm::destroy() {
        __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "destroy");
        delete (this);
    }
}