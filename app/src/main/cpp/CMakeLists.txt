cmake_minimum_required(VERSION 3.18.1)
project("holdtune")

find_package(oboe REQUIRED CONFIG)

add_library(holdtune SHARED
        audio-bridge.cpp
        AudioEngine.cpp
        AudioDecoder.cpp)

target_link_libraries(holdtune
        oboe::oboe
        log
        android
        mediandk)
