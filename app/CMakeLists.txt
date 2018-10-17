# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

#set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#file(GLOB native_srcs "src/main/cpp/*.cpp")
#add_library( # Sets the name of the library.
#             native-lib

             # Sets the library as a shared library.
 #            SHARED

             # Provides a relative path to your source file(s).
 #            ${native_srcs} )



# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

include_directories(/Users/LesPark-sh/Downloads/ffmpeg-3.2.12 /Users/LesPark-sh/Downloads/x264-snapshot-20181016-2245)
#include_directories(/Users/LesPark-sh/Downloads/ffmpeg-3.2.12)

add_library( avcodec-57
             SHARED
             IMPORTED)
set_target_properties( avcodec-57
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libavcodec-57.so)

add_library( avutil-55
             SHARED
             IMPORTED)
set_target_properties( avutil-55
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libavutil-55.so)

add_library( avfilter-6
             SHARED
             IMPORTED)
set_target_properties( avfilter-6
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libavfilter-6.so)

add_library( avformat-57
             SHARED
             IMPORTED)
set_target_properties( avformat-57
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libavformat-57.so)


add_library( swresample-2
             SHARED
             IMPORTED)
set_target_properties( swresample-2
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libswresample-2.so)


add_library( swscale-4
             SHARED
             IMPORTED)
set_target_properties( swscale-4
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libswscale-4.so)

add_library( x264-157
             SHARED
             IMPORTED)
set_target_properties( x264-157
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libx264-157.so)

add_library( postproc-54
             SHARED
             IMPORTED)
set_target_properties( postproc-54
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libpostproc-54.so)

file(GLOB media_kit_src "src/main/cpp/mediakit/*.c")

add_library( # Sets the name of the library.
             media-kit

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${media_kit_src} )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
#                      native-lib
                       media-kit
                       avcodec-57
                       postproc-54
                       avutil-55
                       swscale-4
                       swresample-2
                       avformat-57
                       avfilter-6
                       x264-157
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )