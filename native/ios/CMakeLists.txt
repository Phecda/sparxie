cmake_minimum_required(VERSION 3.22.1)

project(iperf3-ios LANGUAGES C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

file(GLOB IPERF_SOURCES CONFIGURE_DEPENDS
    "${CMAKE_CURRENT_LIST_DIR}/../iperf3/src/*.c"
)

add_library(iperf3 STATIC ${IPERF_SOURCES})

target_include_directories(iperf3 PUBLIC
    "${CMAKE_CURRENT_LIST_DIR}/../iperf3/src"
)
