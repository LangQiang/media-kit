#!/usr/bin/env sh

set -e

TEST_OK_FFMPEG_VERSION=3.2.12

FF_ARCH=arm
# debug or null
FF_BUILD_OPT=

ANDROID_NDK=/Users/chu/Library/Android/ndk/r15c 

FF_SYSROOT=$ANDROID_NDK/platforms/android-15/arch-arm
FF_TOOLCHAIN=$ANDROID_NDK/toolchains

FF_PREFIX=$(pwd)/android/$FF_ARCH
FF_CROSS_PREFIX=$FF_TOOLCHAIN/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64/bin/arm-linux-androideabi-

# armv7a only
FF_CFG_FLAGS="--arch=arm --cpu=cortex-a8 --enable-neon --enable-thumb"

FF_EXTRA_CFLAGS="-march=armv7-a -mcpu=cortex-a8 -mfpu=vfpv3-d16 -mfloat-abi=softfp -mthumb"
FF_EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"

FF_CFLAGS="-O3 -Wall -pipe \
    -std=c99 \
    -ffast-math \
    -fstrict-aliasing -Werror=strict-aliasing \
    -Wno-psabi -Wa,--noexecstack \
    -DANDROID -DNDEBUG"

# Load common options
export COMMON_FF_CFG_FLAGS=

. ffmpeg_module.sh

echo "COMMON_FF_CFG_FLAGS: \n$COMMON_FF_CFG_FLAGS"

FF_CFG_FLAGS="$FF_CFG_FLAGS $COMMON_FF_CFG_FLAGS"

echo "FF_CFG_FLAGS: \n$FF_CFG_FLAGS"

#-------------------
# Standard options
FF_CFG_FLAGS="$FF_CFG_FLAGS --prefix=$FF_PREFIX"

# Advanced options (expert only)
FF_CFG_FLAGS="$FF_CFG_FLAGS --cross-prefix=$FF_CROSS_PREFIX"
FF_CFG_FLAGS="$FF_CFG_FLAGS --enable-cross-compile"
FF_CFG_FLAGS="$FF_CFG_FLAGS --target-os=android"
FF_CFG_FLAGS="$FF_CFG_FLAGS --sysroot=$FF_SYSROOT"
FF_CFG_FLAGS="$FF_CFG_FLAGS --enable-pic"
FF_CFG_FLAGS="$FF_CFG_FLAGS --enable-shared"
FF_CFG_FLAGS="$FF_CFG_FLAGS --disable-static"
# FF_CFG_FLAGS="$FF_CFG_FLAGS --disable-symver"

# Optimization options (experts only):
FF_CFG_FLAGS="$FF_CFG_FLAGS --enable-asm"
FF_CFG_FLAGS="$FF_CFG_FLAGS --enable-inline-asm"

if [ "$FF_BUILD_OPT" = "debug" ]; then
	FF_CFG_FLAGS="$FF_CFG_FLAGS --disable-optimizations"
	FF_CFG_FLAGS="$FF_CFG_FLAGS --enable-debug"
	FF_CFG_FLAGS="$FF_CFG_FLAGS --disable-small"
else
	FF_CFG_FLAGS="$FF_CFG_FLAGS --enable-optimizations"
	FF_CFG_FLAGS="$FF_CFG_FLAGS --enable-debug"
	FF_CFG_FLAGS="$FF_CFG_FLAGS --enable-small"
fi

# 重建 
# rm -ri $FF_PREFIX
mkdir -p $FF_PREFIX

#--------------------
echo ""
echo "--------------------"
echo "[*] configurate ffmpeg"
echo "--------------------"

./configure $FF_CFG_FLAGS \
	--extra-cflags="$FF_CFLAGS $FF_EXTRA_CFLAGS" \
	--extra-ldflags="$FF_EXTRA_LDFLAGS"

#--------------------
echo ""
echo "--------------------"
echo "[*] compile ffmpeg"
echo "--------------------"

make clean
make 
make -j8 install

