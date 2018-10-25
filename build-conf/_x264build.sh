#!/bin/sh
NDK=/Users/LesPark-sh/Downloads/android-ndk-r14b
SYSROOT=$NDK/platforms/android-9/arch-arm
TOOLCHAIN=$NDK/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64
function build_one
{
./configure \
    --prefix=$PREFIX \
    --cross-prefix=$TOOLCHAIN/bin/arm-linux-androideabi- \
    --sysroot=$SYSROOT \
    --host=arm-linux \
    --enable-pic \
    --enable-shared \
    --enable-static \
    --disable-cli \
    --extra-cflags="-march=armv7-a  -mfloat-abi=softfp -mfpu=neon"
    make clean
    make
    make install

 }
CPU=arm
PREFIX=$(pwd)/android/arm
build_one