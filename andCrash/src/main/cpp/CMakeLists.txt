#指定了你的项目需要的 CMake 的最低版本。
cmake_minimum_required(VERSION 3.4.1)

#项目名称
project(nativeCrash)


#需要记住 源码 -> 通过编译器 -> 通过链接器(生成一个可执行文件（.exe, .bin 等），或者一个库（.dll, .so, .a, .lib等)
# 支持的ABI
set(CMAKE_ANDROID_ARCH_ABI armeabi-v7a arm64-v8a x86 x86_64)

# 设置C++标准
set(CMAKE_CXX_STANDARD 11)  # 关键配置
set(CMAKE_CXX_STANDARD_REQUIRED ON)
# 保留调试符号
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -fno-omit-frame-pointer")

# 定义了一个名称为 nativeCrash 的库，其将作为一个动态库（SHARED）编译
#add_library(library_name library_type source_code_file)
add_library(
        nativeCrash # 库的名字
        SHARED  # 库的类型，这里是动态库
        # 库的源码文件
        native_crash_handler.cpp native_crash_jni_bridge.cpp jni_env_deleter.cpp
)

#用于在预定义的路径中寻找库（在这里是 NDK 的 log 库），并将其路径存储在 log-lib 变量中。
#find_library(variable_to_store_path library_name)
find_library(
        log-lib  # 系统库的名字
        log   # 你要寻找的库
)


# 将目标库 native-lib 与找到的日志库 log-lib 链接起来。target_link_libraries(target_lib other_libs)
#将log-lib 链接到 nativeCrash
# 将你的库和其他库链接起来
#target_link_libraries(target_library other_libraries)
target_link_libraries(
        nativeCrash # 你的库
        ${log-lib}) # 其他的库


