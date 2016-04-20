cmake_minimum_required(VERSION 3.5)
project(improved_pngquant)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES
    blur.c
    blur.h
    mediancut.c
    mempool.c
    nearest.c
    pam.c
    pngquant.c
    rwpng.c
    viter.c)

include_directories(.)

add_library(improved_pngquant SHARED ${SOURCE_FILES})

find_package(PNG REQUIRED)
if (PNG_FOUND)
    INCLUDE_DIRECTORIES(${PNG_INCLUDE_DIRS})
    TARGET_LINK_LIBRARIES(improved_pngquant ${PNG_LIBRARY})
else (PNG_FOUND)
    MESSAGE(FATAL_ERROR "libpng is not found")
endif (PNG_FOUND)

find_package(JNI REQUIRED)
INCLUDE_DIRECTORIES(${JNI_INCLUDE_DIRS})
TARGET_LINK_LIBRARIES(improved_pngquant ${JNI_LIBRARY})