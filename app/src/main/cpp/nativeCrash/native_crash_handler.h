#ifndef ANDROID_CRASH_HANDLER_H
#define ANDROID_CRASH_HANDLER_H

#include <string>
#include <atomic>
#include <functional>  // 用于std::function回调

class CrashHandler final {
public:
    using CrashCallback = std::function<void(const std::string &)>;  // 定义回调类型

    // 初始化方法（线程安全）
    static void Init(const std::string &logDir);

    // 设置崩溃回调（用于上报）
    static void SetCrashCallback(CrashCallback callback);

    // 设置应用版本信息
    static void SetVersion(const std::string &version);

    // 删除拷贝构造函数和赋值运算符（单例模式）
    CrashHandler(const CrashHandler &) = delete;

    void operator=(const CrashHandler &) = delete;

private:
    // 信号处理器安装方法
    static void InstallSignalHandlers();

    // 实际的信号处理函数（符合POSIX标准）
    static void SignalHandler(int sig, siginfo_t* info, void* ucontext);

    // 寄存器转储方法
    static void DumpRegisters(void* ucontext, int fd);

    // 堆栈跟踪捕获方法
    static void DumpStackTrace(void* ucontext, int fd);

    // 内存映射记录方法
    static void DumpMemoryMaps(int fd);

    // 生成日志路径
    static std::string GenerateCrashLogPath();

    // 触发回调上传
    static void TriggerUpload(const std::string& logPath);

    // 静态成员变量
    static std::string m_logDir;         // 日志目录
    static std::string m_version;        // 应用版本
    static CrashCallback m_callback;     // 回调函数
    static std::atomic_bool m_crashHandling; // 原子标志防止递归崩溃
};

#endif //ANDROID_CRASH_HANDLER_H