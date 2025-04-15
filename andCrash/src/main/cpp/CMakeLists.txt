#指定了你的项目需要的 CMake 的最低版本。
cmake_minimum_required(VERSION 3.4.1)

project("nativeCrash")

# 支持的ABI
set(CMAKE_ANDROID_ARCH_ABI armeabi-v7a arm64-v8a x86 x86_64)
# 设置C++标准
set(CMAKE_CXX_STANDARD 17)  # 关键配置
set(CMAKE_CXX_STANDARD_REQUIRED ON)
# 保留调试符号
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -fno-omit-frame-pointer")
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION TRUE)
# 分离调试符号
set(CMAKE_BUILD_TYPE RelWithDebInfo)


# 添加子模块
add_subdirectory(core)

add_library(
        nativeCrash
        SHARED
        native_crash_handler.cpp native_crash_jni_bridge.cpp jni_env_deleter.cpp
)
find_library(log-lib log)

target_compile_options(nativeCrash PRIVATE -O2 -g)


target_link_libraries(nativeCrash ${log-lib} core-lib)


