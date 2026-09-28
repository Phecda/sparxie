cmake_minimum_required(VERSION 3.22.1)

project(iperf3-native LANGUAGES C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

file(GLOB IPERF_SOURCES CONFIGURE_DEPENDS
    "${CMAKE_CURRENT_LIST_DIR}/../../iperf3/src/*.c"
)

add_library(iperf3 SHARED
    src/main/cpp/iperf3_jni.c
    ${IPERF_SOURCES}
)

target_include_directories(iperf3 PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/../../iperf3/src"
)

find_library(ANDROID_LOG_LIB log)
find_library(ANDROID_LIB android)

target_link_libraries(iperf3
    ${ANDROID_LOG_LIB}
    ${ANDROID_LIB}
    m
)

target_link_options(iperf3 PRIVATE
    "-Wl,-z,max-page-size=16384"
)
