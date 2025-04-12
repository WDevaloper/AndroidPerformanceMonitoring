#ifndef ANDROID_CRASH_HANDLER_H
#define ANDROID_CRASH_HANDLER_H

#include <string>
#include <atomic>
#include <functional>  // 用于std::function回调
#include <jni.h>
#include <sys/eventfd.h>
#include <pthread.h>

// 新增全局变量
static JavaVM *g_vm = nullptr;
static jobject g_callback = nullptr;
static pthread_mutex_t g_callbackMutex = PTHREAD_MUTEX_INITIALIZER;
static int g_eventFd = -1;
static pthread_t g_callbackThread;

class CrashHandler final {
public:
    // 初始化方法（线程安全）
    static void Init(JNIEnv *env, const std::string &logDir, jobject callback);

    // 设置应用版本信息
    static void SetVersion(const std::string &version);

    static void SetLogDir(const std::string &logDir);

    // 线程初始化方法
    static void ThreadInit(JNIEnv *en, jobject callback);

    // 线程执行函数声明  回调方法（线程安全）
    [[noreturn]] static void *CallbackThread(void *arg);

    // 通知Java回调方法
    static void NotifyJavaCallback(const std::string &crashLogPath);

    static int deleteLogFile(const std::string &crashLogFullPath);

    static int removeDirectory(const std::string &crashLogPath);

    // 删除拷贝构造函数和赋值运算符（单例模式）
    CrashHandler(const CrashHandler &) = delete;

    void operator=(const CrashHandler &) = delete;

private:
    // 信号处理器安装方法
    static void InstallSignalHandlers();

    // 实际的信号处理函数（符合POSIX标准）
    static void SignalHandler(int sig, siginfo_t *info, void *ucontext);

    // 寄存器转储方法
    static void DumpRegisters(void *ucontext, int fd);

    // 堆栈跟踪捕获方法
    static void DumpStackTrace(void *ucontext, int fd);

    // 内存映射记录方法
    static void DumpMemoryMaps(int fd);

    // 生成日志路径
    static std::string GenerateCrashLogPath();

    static std::string GetCurrentTime();

    // 静态成员变量
    static std::string m_logDir;         // 日志目录
    static std::string m_version;        // 应用版本
    static std::atomic_bool m_crashHandling; // 原子标志防止递归崩溃
    static struct sigaction old_sa[NSIG];
};

#endif //ANDROID_CRASH_HANDLER_H