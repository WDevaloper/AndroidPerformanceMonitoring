#ifndef ANDROID_CRASH_HANDLER_H
#define ANDROID_CRASH_HANDLER_H

#include <string>
#include <atomic>

class CrashHandler {
public:
    static void Init(const std::string &logPath);

    static void SetVersionInfo(const std::string &version);

private:
    static void InstallSignalHandlers();

    static void SignalHandler(int sig, siginfo_t *info, void *context);

    static void CaptureStackTrace(void *context);

    static void WriteCrashLog(int sig, const siginfo_t *info, void *context);

    static std::string m_logPath;
    static std::string m_appVersion;
    static const int MAX_STACK_FRAMES = 64;
    static std::atomic<bool> g_crashHandling;
};

#endif //ANDROID_CRASH_HANDLER_H