cmake_minimum_required(VERSION 3.4.1)



add_library(apm SHARED and_apm.cpp apm_bridge.cpp)

find_library(log-lib log)

target_link_libraries(apm  ${log-lib})