//
// Created by fayou on 2025/4/14.
//

#ifndef ANDROIDPERFORMANCEMONITORING_LOG_UTILS_H
#define ANDROIDPERFORMANCEMONITORING_LOG_UTILS_H


class log_utils {
public:

    static void info(const char *tag, const char *fmt, ...);

    static void error(const char *tag, const char *fmt, ...);

    static void warn(const char *tag, const char *fmt, ...);

    static void debug(const char *tag, const char *fmt, ...);
};


#endif //ANDROIDPERFORMANCEMONITORING_LOG_UTILS_H
