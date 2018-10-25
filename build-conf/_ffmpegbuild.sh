#!/bin/sh
NDK=/Users/LesPark-sh/Downloads/android-ndk-r14b
SYSROOT=$NDK/platforms/android-9/arch-arm
TOOLCHAIN=$NDK/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64
function build_one
{
./configure \
--prefix=$PREFIX \
--enable-shared \
--disable-static \
--disable-doc \
--enable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-ffserver \
--disable-avdevice \
--enable-small \
--disable-decoders \
--enable-libx264 \
--enable-gpl \
--enable-protocols \
--enable-muxer=mp4 \
--enable-decoder=h264 \
--enable-decoder=gif \
--enable-decoder=aac \
--enable-decoder=png \
--enable-decoder=mjpeg \
--enable-decoder=mpeg4 \
--enable-decoder=aac_latm \
--enable-decoder=yuv4 \
--enable-decoder=zlib \
--enable-decoder=pcm_s16le \
--disable-doc \
--disable-symver \
--cross-prefix=$TOOLCHAIN/bin/arm-linux-androideabi- \
--target-os=linux \
--arch=arm \
--enable-cross-compile \
--sysroot=$SYSROOT \
--extra-cflags="-Os -fpic -I/Users/LesPark-sh/Downloads/x264-snapshot-20181016-2245/android/arm/include $ADDI_CFLAGS" \
--extra-ldflags="-L/Users/LesPark-sh/Downloads/x264-snapshot-20181016-2245/android/arm/lib $ADDI_LDFLAGS"\
$ADDITIONAL_CONFIGURE_FLAG
make clean
make
make install
}
CPU=arm
PREFIX=$(pwd)/android/$CPU
ADDI_CFLAGS="-marm"
build_one