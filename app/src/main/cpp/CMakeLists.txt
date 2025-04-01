cmake_minimum_required(VERSION 3.4.1)
project(nativeCrash)

# 支持的ABI
set(CMAKE_ANDROID_ARCH_ABI armeabi-v7a arm64-v8a x86 x86_64)

# 设置C++标准
set(CMAKE_CXX_STANDARD 11)  # 关键配置
set(CMAKE_CXX_STANDARD_REQUIRED ON)
# 保留调试符号
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -fno-omit-frame-pointer")

add_library(nativeCrash SHARED crash_handler.cpp jni_bridge.cpp)

find_library(log-lib log)

target_link_libraries(nativeCrash ${log-lib})


