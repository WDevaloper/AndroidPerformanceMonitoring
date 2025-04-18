//
// Created by fayou on 2025/4/18.
//

#ifndef ANDROIDPERFORMANCEMONITORING_HPROF_DUMP_H
#define ANDROIDPERFORMANCEMONITORING_HPROF_DUMP_H


class HprofDump {
public:
    static void suspend_threads();

    static void resume_threads();

    static void dump_memory(const char *filename);
};


#endif //ANDROIDPERFORMANCEMONITORING_HPROF_DUMP_H
