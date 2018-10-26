package com.lq.mediakit.jni;

public class FFmpegVideoProcessor {


    public native int clipVideo(String input, String output, int from, int to);

    public native int trasVideo(String input, String output);
}
