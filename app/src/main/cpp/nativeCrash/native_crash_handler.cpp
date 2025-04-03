// 架构相关头文件
#if defined(__i386__) || defined(__x86_64__)

#include <sys/ucontext.h>  // x86架构需要此头文件

#else
#include <ucontext.h>      // ARM架构使用标准头文件
#endif

#include "native_crash_handler.h"
#include <unistd.h>
#include <sys/syscall.h>
#include <android/log.h>
#include <dlfcn.h>
#include <csignal>
#include <fcntl.h>
#include <unwind.h>
#include <cstring>
#include <cinttypes>
#include <cstdint>
#include <atomic>
#include <utility>
#include <sys/time.h>
#include <sys/wait.h>
#include <dirent.h>
#include <jni.h>


std::string CrashHandler::m_logDir;
std::string CrashHandler::m_version;
std::atomic_bool CrashHandler::m_crashHandling(false);

void CrashHandler::SetVersion(const std::string &version) {
    m_version = version;
}

int CrashHandler::deleteLogFile(const std::string &crashLogFullPath) {
    return unlink(crashLogFullPath.c_str());
}

int CrashHandler::removeDirectory(const std::string &crashLogPath) {
    const char *path = crashLogPath.c_str();
    DIR *dir = opendir(path);
    if (!dir) {
        __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "无法打开目录: %s", path);
        return -1;
    }

    struct dirent *entry;
    while ((entry = readdir(dir)) != nullptr) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue; // 跳过 . 和 ..
        }

        char full_path[PATH_MAX];
        snprintf(full_path, sizeof(full_path), "%s/%s", path, entry->d_name);

        if (entry->d_type == DT_DIR) {
            // 递归删除子目录
            if (removeDirectory(full_path) != 0) {
                closedir(dir);
                return -1;
            }
        } else {
            // 删除文件
            if (unlink(full_path) != 0) {
                __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "无法删除文件: %s",
                                    full_path);
                closedir(dir);
                return -1;
            }
        }
    }

    closedir(dir);

    // 删除空目录
    if (rmdir(path) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "无法删除目录: %s", path);
        return -1;
    }
    return 0;
}

void CrashHandler::Init(JNIEnv *env, const std::string &logDir, jobject callback) {
    __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "Init,logDir: %s", logDir.c_str());
    m_logDir = logDir;
    InstallSignalHandlers();
    ThreadInit(env, callback);// 创建event_fd和回调线程
}

void CrashHandler::ThreadInit(JNIEnv *env, jobject callback) {
    __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "ThreadInit");
    env->GetJavaVM(&g_vm);
    // 线程安全 互斥锁
    pthread_mutex_lock(&g_callbackMutex);

    if (g_callback) {
        env->DeleteGlobalRef(g_callback);
    }
    g_callback = env->NewGlobalRef(callback);

    //释放互斥锁
    pthread_mutex_unlock(&g_callbackMutex);

    // 创建event_fd
    g_eventFd = eventfd(0, EFD_NONBLOCK);

    // 创建回调线程
    pthread_create(&g_callbackThread, nullptr, CallbackThread, nullptr);
}

/**
 * 回调线程实现
 *
 * @param arg
 * @return
 */
[[noreturn]] void *CrashHandler::CallbackThread(void *arg) {
    JNIEnv *env;
    g_vm->AttachCurrentThread(&env, nullptr);

    uint64_t eventCount;
    while (true) {
        if (read(g_eventFd, &eventCount, sizeof(eventCount)) > 0) {
            pthread_mutex_lock(&g_callbackMutex);
            if (g_callback) {
                jclass callbackClass = env->GetObjectClass(g_callback);
                jmethodID method = env->GetMethodID(callbackClass, "onCrashReport",
                                                    "(Ljava/lang/String;)V");
                jstring jPath = env->NewStringUTF(m_logDir.c_str());
                env->CallVoidMethod(g_callback, method, jPath);
                env->DeleteLocalRef(jPath);
            }
            pthread_mutex_unlock(&g_callbackMutex);
        }
    }
    g_vm->DetachCurrentThread();
}

void CrashHandler::InstallSignalHandlers() {
    __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "InstallSignalHandlers");
    struct sigaction sa{};      // 清空结构体
    sa.sa_sigaction = SignalHandler;  // 指定处理函数
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_RESETHAND;  // 关键标志：
    // SA_SIGINFO：需要siginfo_t信息
    // SA_ONSTACK：使用备用栈
    // SA_RESETHAND：处理一次后恢复默认行为
    // 需要捕获的信号列表
    const int signals[] = {SIGSEGV, SIGABRT, SIGBUS, SIGFPE, SIGILL};
    for (int sig: signals) {
        if (sigaction(sig, &sa, nullptr) == -1) {  // 注册信号处理
            // 实际项目应记录错误日志
            __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "sigaction failed: %s",
                                strerror(errno));
        }
    }


    // 备用信号栈
    stack_t ss{};
    ss.ss_sp = malloc(SIGSTKSZ);
    if (!ss.ss_sp) {
        __android_log_write(ANDROID_LOG_ERROR, "CrashHandler", "Failed to allocate signal stack");
        return;
    }
    ss.ss_size = SIGSTKSZ;
    ss.ss_flags = 0;
    if (sigaltstack(&ss, nullptr) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "CrashHandler", "sigaltstack failed: %s",
                            strerror(errno));
    }
}

void CrashHandler::SignalHandler(int sig, siginfo_t *info, void *ucontext) {
    __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "SignalHandler");
    // 原子锁防止重复进入
    if (m_crashHandling.exchange(true)) {
        _exit(1);  // 立即终止防止递归崩溃
    }

    const std::string logPath = GenerateCrashLogPath();
    __android_log_print(ANDROID_LOG_ERROR, "NativeCrash", "SignalHandler:%s", logPath.c_str());

    // 异步安全方式打开文件（不使用fopen）
    int fd = open(logPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0640);
    if (fd == -1) return;  // 打开失败直接返回

    // 写入基础信息（使用dprintf保证线程安全）
    dprintf(fd, "*** Native Crash Report ***\n");
    dprintf(fd, "App Version: %s\n", m_version.c_str());
    dprintf(fd, "Signal: %d (%s)\n", sig, strsignal(sig));
    dprintf(fd, "Fault Address: %p\n", info->si_addr);
    dprintf(fd, "PID: %d, TID: %ld\n\n", getpid(), syscall(SYS_gettid));

    // 关键数据采集
    DumpRegisters(ucontext, fd);    // 寄存器转储
    DumpStackTrace(ucontext, fd);   // 堆栈跟踪
    DumpMemoryMaps(fd);             // 内存映射
    close(fd);  // 必须关闭文件描述符

    // 原子锁释放
    m_crashHandling.store(false);

    NotifyJavaCallback(logPath);// 通知Java回调

    // 休眠1毫秒等待文件写入完成
    struct timespec delay = {0, 1000 * 1000}; // 0秒 + 1000000纳秒 = 1毫秒
    nanosleep(&delay, nullptr);

    // 恢复默认信号处理并重新触发信号（确保进程终止）
    signal(sig, SIG_DFL);
    kill(getpid(), sig);
}

// 通知event_fd函数  线程中read 阻塞
void CrashHandler::NotifyJavaCallback(const std::string &crashLogPath) {
    uint64_t value = 1;
    write(g_eventFd, &value, sizeof(value));
}

void CrashHandler::DumpRegisters(void *ucontext, int fd) {
    auto *ctx = static_cast<ucontext_t *>(ucontext);

#if defined(__arm__)
    // ARMv7
    dprintf(fd, "ARM Registers:\n");
    for (int i = 0; i < 16; ++i) {
        dprintf(fd, "R%-2d: 0x%08x\n", i, ctx->uc_mcontext.arm_r[i]);
    }
    dprintf(fd, "CPSR: 0x%08x\n", ctx->uc_mcontext.arm_cpsr);
#elif defined(__aarch64__)
    // ARM64
    dprintf(fd, "ARM64 Registers:\n");
    for (int i = 0; i < 31; ++i) {
        dprintf(fd, "X%-2d: 0x%016" PRIx64 "\n", i, ctx->uc_mcontext.regs[i]);
    }
    dprintf(fd, "SP: 0x%016" PRIx64 "\n", ctx->uc_mcontext.sp);
    dprintf(fd, "PC: 0x%016" PRIx64 "\n", ctx->uc_mcontext.pc);
#elif defined(__i386__)
    // x86
    dprintf(fd, "x86 Registers:\n");
    dprintf(fd, "EAX: 0x%08x\n", ctx->uc_mcontext.gregs[REG_EAX]);
    dprintf(fd, "EBX: 0x%08x\n", ctx->uc_mcontext.gregs[REG_EBX]);
    dprintf(fd, "ECX: 0x%08x\n", ctx->uc_mcontext.gregs[REG_ECX]);
    dprintf(fd, "EDX: 0x%08x\n", ctx->uc_mcontext.gregs[REG_EDX]);
    dprintf(fd, "EIP: 0x%08x\n", ctx->uc_mcontext.gregs[REG_EIP]);
#elif defined(__x86_64__)
    // x64
    dprintf(fd, "x64 Registers:\n");
    dprintf(fd, "RAX: 0x%016" PRIx64 "\n", ctx->uc_mcontext.gregs[REG_RAX]);
    dprintf(fd, "RBX: 0x%016" PRIx64 "\n", ctx->uc_mcontext.gregs[REG_RBX]);
    dprintf(fd, "RCX: 0x%016" PRIx64 "\n", ctx->uc_mcontext.gregs[REG_RCX]);
    dprintf(fd, "RDX: 0x%016" PRIx64 "\n", ctx->uc_mcontext.gregs[REG_RDX]);
    dprintf(fd, "RIP: 0x%016" PRIx64 "\n", ctx->uc_mcontext.gregs[REG_RIP]);
#endif
}

struct BacktraceState {  // 堆栈遍历状态结构体
    void **current;
    void **end;
};

static _Unwind_Reason_Code UnwindCallback(
        struct _Unwind_Context *ctx, void *arg) {
    auto *state = static_cast<BacktraceState *>(arg);
    void *pc = reinterpret_cast<void *>(_Unwind_GetIP(ctx));  // 获取指令指针
    if (pc && state->current < state->end) {
        *state->current++ = pc;  // 存储有效地址
    }
    return pc ? _URC_NO_REASON : _URC_END_OF_STACK;  // 继续或终止
}

void CrashHandler::DumpStackTrace(void *ucontext, int fd) {
    void *stack[128];  // 最多捕获128层堆栈
    BacktraceState state{stack, stack + 128};

    // 使用libunwind进行堆栈展开
    _Unwind_Backtrace(UnwindCallback, &state);

    dprintf(fd, "\nStack Trace:\n");
    for (size_t i = 0; stack[i]; ++i) {  // 遍历有效堆栈地址
        Dl_info info{};
        if (dladdr(stack[i], &info)) {  // 解析符号信息
            const char *name = info.dli_sname ?: "??";  // 符号名或占位符
            uintptr_t offset = (uintptr_t) stack[i] - (uintptr_t) info.dli_saddr;

            // 格式化输出：序号、地址、模块、函数名+偏移
            dprintf(fd, "#%02zu pc %08" PRIxPTR " %s (%s+%#" PRIxPTR ")\n",
                    i,
                    (uintptr_t) stack[i] - (uintptr_t) info.dli_fbase,
                    info.dli_fname,
                    name,
                    offset);
        }
    }
}

void CrashHandler::DumpMemoryMaps(int fd) {
    // 写入内存映射
    int mapsFd = open("/proc/self/maps", O_RDONLY);
    if (mapsFd != -1) {
        dprintf(fd, "\nMemory Map:\n");
        char mapsBuf[4096];
        ssize_t bytes;
        while ((bytes = read(mapsFd, mapsBuf, sizeof(mapsBuf))) > 0) {
            write(fd, mapsBuf, bytes);
        }
        close(mapsFd);
    }
    // 写入结束标记
    dprintf(fd, "\n*** End of Crash Report ***\n");
}

std::string CrashHandler::GenerateCrashLogPath() {
    // 生成日志路径（示例：/data/crash/20230315-143022.crash）

    time_t now = time(nullptr);
    struct tm *tm = localtime(&now);
    char timeStr[32];
    strftime(timeStr, sizeof(timeStr), "%Y%m%d-%H%M%S", tm);
    if (m_logDir.back() != '/') {
        m_logDir.append("/");
    }
    return m_logDir + "/" + "crash-" + timeStr + ".log";
}