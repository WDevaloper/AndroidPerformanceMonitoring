#include "include/log_utils.h"
#include <android/log.h>


void log_utils::debug(const char *tag, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_DEBUG, tag, fmt, args);
    va_end(args);
}

void log_utils::error(const char *tag, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_ERROR, tag, fmt, args);
    va_end(args);
}

void log_utils::info(const char *tag, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_INFO, tag, fmt, args);
    va_end(args);
}

void log_utils::warn(const char *tag, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_WARN, tag, fmt, args);
    va_end(args);
}

