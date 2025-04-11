//
// Created by fayou on 2025/4/11.
//

#ifndef ANDROIDPERFORMANCEMONITORING_AND_APM_H
#define ANDROIDPERFORMANCEMONITORING_AND_APM_H

namespace apm {
    class AndApm {
    public:
        long nativeHandle;

        static long init();

        void start();

        void stop();

        void destroy();
    };

} // apm

#endif //ANDROIDPERFORMANCEMONITORING_AND_APM_H
