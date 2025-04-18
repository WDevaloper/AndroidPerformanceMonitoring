#include <dirent.h>
#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <asm-generic/fcntl.h>
#include <fcntl.h>
#include <cstring>
#include "include/hprof_dump.h"


void HprofDump::suspend_threads() {
    DIR *task_dir = opendir("/proc/self/task");
    if (!task_dir) {
        perror("opendir");
        exit(1);
    }

    struct dirent *entry;
    while ((entry = readdir(task_dir))) {
        if (entry->d_name[0] == '.') continue;
        pid_t tid = atoi(entry->d_name);
        if (tid != getpid()) { // 不暂停主线程
            if (kill(tid, SIGSTOP) == -1) {
                perror("kill SIGSTOP");
            }
        }
    }
    closedir(task_dir);
}

void HprofDump::resume_threads() {
    DIR *task_dir = opendir("/proc/self/task");
    if (!task_dir) {
        perror("opendir");
        exit(1);
    }

    struct dirent *entry;
    while ((entry = readdir(task_dir))) {
        if (entry->d_name[0] == '.') continue;
        pid_t tid = atoi(entry->d_name);
        if (tid != getpid()) { // 不恢复主线程（未暂停）
            if (kill(tid, SIGCONT) == -1) {
                perror("kill SIGCONT");
            }
        }
    }
    closedir(task_dir);
}

void HprofDump::dump_memory(const char *filename) {
    int mem_fd = open("/proc/self/mem", O_RDONLY);
    if (mem_fd < 0) {
        perror("open /proc/self/mem");
        exit(1);
    }

    FILE *maps = fopen("/proc/self/maps", "r");
    if (!maps) {
        perror("fopen /proc/self/maps");
        exit(1);
    }

    FILE *dump = fopen(filename, "wb");
    if (!dump) {
        perror("fopen dump file");
        exit(1);
    }

    char line[256];
    while (fgets(line, sizeof(line), maps)) {
        unsigned long start, end;
        if (sscanf(line, "%lx-%lx", &start, &end) != 2) continue;

        // 跳过非可读内存段（如栈、内核空间）
        if (strstr(line, "r--") == nullptr && strstr(line, "r-x") == nullptr) continue;

        size_t size = end - start;
        void *buf = malloc(size);
        if (!buf) {
            perror("malloc");
            continue;
        }

        // 读取内存内容
        if (lseek(mem_fd, (off_t) start, SEEK_SET) == -1) {
            perror("lseek");
            free(buf);
            continue;
        }

        ssize_t nread = read(mem_fd, buf, size);
        if (nread != (ssize_t) size) {
            perror("read");
            free(buf);
            continue;
        }

        fwrite(buf, size, 1, dump);
        free(buf);
    }

    fclose(maps);
    fclose(dump);
    close(mem_fd);
}
