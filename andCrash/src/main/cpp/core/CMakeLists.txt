# 创建接口库（无源码）
add_library(core-lib STATIC log_utils.cpp)

# 暴露公共头文件
target_include_directories(core-lib PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)
find_library(log-lib log)
# 传递编译选项
target_compile_definitions(core-lib INTERFACE USE_GPU_ACCELERATION=1)

target_link_libraries(core-lib ${log-lib})