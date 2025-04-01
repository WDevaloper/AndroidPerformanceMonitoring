#include "crash_handler.h"
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
#include <sys/time.h>

// 架构相关头文件
#if defined(__i386__) || defined(__x86_64__)

#include <sys/ucontext.h>  // x86架构需要此头文件
#include <jni.h>

#else
#include <ucontext.h>      // ARM架构使用标准头文件
#endif

// 异步安全日志宏
#define ASYNC_LOG(fd, msg) do { \
    const char* _m = msg; \
    write(fd, _m, strlen(_m)); \
} while(0)

std::string CrashHandler::m_logPath;
std::string CrashHandler::m_appVersion;
std::atomic<bool> CrashHandler::g_crashHandling(false);

void CrashHandler::Init(const std::string &logPath) {
    m_logPath = logPath;

    // 设置备用信号栈
    stack_t ss{};
    ss.ss_sp = malloc(SIGSTKSZ * 2);
    ss.ss_size = SIGSTKSZ * 2;
    sigaltstack(&ss, nullptr);

    InstallSignalHandlers();
}

void CrashHandler::SetVersionInfo(const std::string &version) {
    m_appVersion = version;
}

void CrashHandler::InstallSignalHandlers() {
    struct sigaction sa{};
    sa.sa_sigaction = SignalHandler;
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_NODEFER;

    const int signals[] = {SIGSEGV, SIGABRT, SIGBUS, SIGFPE, SIGILL, SIGTRAP};
    for (int sig: signals) {
        sigaction(sig, &sa, nullptr);
    }
}

void CrashHandler::SignalHandler(int sig, siginfo_t *info, void *context) {
    if (g_crashHandling.exchange(true)) {
        _exit(1);
    }

    WriteCrashLog(sig, info, context);

    // 恢复默认处理并退出
    signal(sig, SIG_DFL);
    kill(getpid(), sig);
}

struct BacktraceState {
    void **current;
    void **end;
};

static _Unwind_Reason_Code UnwindCallback(struct _Unwind_Context *context, void *arg) {
    BacktraceState *state = static_cast<BacktraceState *>(arg);
    void *pc = reinterpret_cast<void *>(_Unwind_GetIP(context));
    if (pc && state->current < state->end) {
        *state->current++ = pc;
    }
    return pc ? _URC_NO_REASON : _URC_END_OF_STACK;
}

void CrashHandler::WriteCrashLog(int sig, const siginfo_t *info, void *context) {
    //文件名 时间
    time_t now = time(nullptr);
    char date[32];
    strftime(date, sizeof(date), "%Y-%m-%d_%H-%M-%S", localtime(&now));
    std::string dateStr = std::string(date);
    std::string fileName = std::string("crash_native").append("_").append(dateStr).append(".log");

    if (m_logPath.back() != '/') m_logPath.append("/");
    std::string logFile = m_logPath.append(fileName);

    // 打开日志文件
    int fd = open(logFile.c_str(), O_WRONLY | O_CREAT | O_APPEND, 0640);
    if (fd == -1) return;

    // 写入基础信息
    char buf[512];
    int len = snprintf(buf, sizeof(buf),
                       "\n\n*** Crash Report (v%s) ***\n"
                       "Signal: %d (%s)\n"
                       "Fault Address: %p\n"
                       "PID: %d, TID: %ld\n"
                       "Timestamp: %ld\n\n",
                       m_appVersion.c_str(),
                       sig, strsignal(sig),
                       info->si_addr,
                       getpid(),
                       static_cast<long>(syscall(SYS_gettid)),
                       static_cast<long>(time(nullptr)));

    write(fd, buf, len);

    ucontext_t *ucontext = static_cast<ucontext_t *>(context);
    len = snprintf(buf, sizeof(buf), "Registers:\n");
    write(fd, buf, len);

// 按架构处理寄存器
#if defined(__arm__)
    // ARMv7 (32-bit)
    for (int i = 0; i < 16; ++i) {
        len = snprintf(buf, sizeof(buf), "R%-2d: 0x%08lx\n",
                      i, ucontext->uc_mcontext.arm_r[i]);
        write(fd, buf, len);
    }
    len = snprintf(buf, sizeof(buf), "PC: 0x%08lx\n",
                  ucontext->uc_mcontext.arm_pc);

#elif defined(__aarch64__)
    // ARM64 (64-bit)
    for (int i = 0; i < 31; ++i) { // X0-X30
        len = snprintf(buf, sizeof(buf), "X%-2d: 0x%016lx\n",
                      i, ucontext->uc_mcontext.regs[i]);
        write(fd, buf, len);
    }
    len = snprintf(buf, sizeof(buf), "PC: 0x%016lx\n",
                  ucontext->uc_mcontext.pc);

#elif defined(__i386__)
    // x86 (32-bit)
    len = snprintf(buf, sizeof(buf),
                   "EIP: 0x%08lx\n"
                   "EAX: 0x%08lx\n"
                   "EBX: 0x%08lx\n"
                   "ECX: 0x%08lx\n"
                   "EDX: 0x%08lx\n",
                   ucontext->uc_mcontext.gregs[REG_EIP],
                   ucontext->uc_mcontext.gregs[REG_EAX],
                   ucontext->uc_mcontext.gregs[REG_EBX],
                   ucontext->uc_mcontext.gregs[REG_ECX],
                   ucontext->uc_mcontext.gregs[REG_EDX]);

#elif defined(__x86_64__)
    // x86_64 (64-bit)
    len = snprintf(buf, sizeof(buf),
        "RIP: 0x%016lx\n"
        "RAX: 0x%016lx\n"
        "RBX: 0x%016lx\n"
        "RCX: 0x%016lx\n"
        "RDX: 0x%016lx\n",
        ucontext->uc_mcontext.gregs[REG_RIP],
        ucontext->uc_mcontext.gregs[REG_RAX],
        ucontext->uc_mcontext.gregs[REG_RBX],
        ucontext->uc_mcontext.gregs[REG_RCX],
        ucontext->uc_mcontext.gregs[REG_RDX]);

#else
    len = snprintf(buf, sizeof(buf), "Unsupported architecture\n");
#endif

    write(fd, buf, len);

    // 捕获并写入堆栈
    CaptureStackTrace(context);

    // 写入内存映射
    int mapsFd = open("/proc/self/maps", O_RDONLY);
    if (mapsFd != -1) {
        write(fd, "\nMemory Map:\n", 12);
        char mapsBuf[4096];
        ssize_t bytes;
        while ((bytes = read(mapsFd, mapsBuf, sizeof(mapsBuf))) > 0) {
            write(fd, mapsBuf, bytes);
        }
        close(mapsFd);
    }

    close(fd);
}

void CrashHandler::CaptureStackTrace(void *context) {
    void *stack[MAX_STACK_FRAMES];
    BacktraceState state{stack, stack + MAX_STACK_FRAMES};
    _Unwind_Backtrace(UnwindCallback, &state);

    int fd = open(m_logPath.c_str(), O_WRONLY | O_APPEND);
    if (fd == -1) return;

    write(fd, "\nStack Trace:\n", 14);

    Dl_info info{};
    for (size_t i = 0; i < MAX_STACK_FRAMES && stack[i]; ++i) {
        if (dladdr(stack[i], &info)) {
            const char *name = info.dli_sname ? info.dli_sname : "??";
            uintptr_t offset = reinterpret_cast<uintptr_t>(stack[i]) -
                               reinterpret_cast<uintptr_t>(info.dli_saddr);
            uintptr_t base = reinterpret_cast<uintptr_t>(info.dli_fbase);

            char line[256];
            int len = snprintf(line, sizeof(line),
                               "#%02zu pc %08" PRIxPTR " %s (%s+%#" PRIxPTR ")\n",
                               i,
                               reinterpret_cast<uintptr_t>(stack[i]) - base,
                               info.dli_fname,
                               name,
                               offset);
            write(fd, line, len);
        }
    }
    close(fd);
}