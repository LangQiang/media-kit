package com.lq.mediakit.jni;


import android.util.Log;

import java.io.File;

public class MediaHelper {
    private final static int TYPE_CLIPS = 1;
    private boolean isBusy;
    private int clipDuration;
    private int type = 0;

    static {
        try {
//            System.loadLibrary("avutil-55");
//            System.loadLibrary("swresample-2");
//            System.loadLibrary("avcodec-57");
//            System.loadLibrary("avformat-57");
//            System.loadLibrary("swscale-4");
//            System.loadLibrary("avfilter-6");
//            System.loadLibrary("x264-157");
//            System.loadLibrary("postproc-54");
            System.loadLibrary("media-kit");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ffmpeg", e.toString());
        }

    }

    private CallBack mCallBack;

    public static abstract class CallBack {
        public abstract void onStart();

        public abstract void onProgress(int progress, int duration);

        public abstract void onEnd();
    }


    private MediaHelper() {
        if (SingleInstanceHolder.mediaHelper != null) {
            throw new IllegalStateException("你想干什么！");
        }
    }

    private static class SingleInstanceHolder {
        static MediaHelper mediaHelper = new MediaHelper();

    }

    public static MediaHelper getInstance() {
        return SingleInstanceHolder.mediaHelper;
    }


    public void addGifWater(final String mp4InputPath, final String gif, final String outPutPath, final int waterW, final int waterH, final float xPercent, final float yPercent, final CallBack callBack) {
        if (isBusy) {
            Log.e("MediaKit", "busy");
            return;
        }
        type = 0;
        this.mCallBack = callBack;
        File file = new File(mp4InputPath);
        if (!file.exists() || !file.getAbsolutePath().toLowerCase().endsWith(".mp4")) {
            Log.e("MediaKit","open file failed");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                isBusy = true;
                callBack.onStart();
                addGifWater(mp4InputPath, gif, outPutPath, waterW + "", waterH + "", xPercent + "", yPercent + "");
                callBack.onEnd();
                isBusy = false;
            }
        }).start();
    }

    private void onProgress(int second, int duration) {
        if (mCallBack != null) {
            if (type == TYPE_CLIPS)
                duration = clipDuration;
            mCallBack.onProgress(second, duration);
        }
    }

    public void videoClips(final String mp4InputPath, final String outPutPath, final String startTime, final String duration, final CallBack callBack) {
        if (isBusy) {
            Log.e("MediaKit", "busy");
            return;
        }
        File file = new File(mp4InputPath);
        if (!file.exists() || !file.getAbsolutePath().toLowerCase().endsWith(".mp4")) {
            Log.e("MediaKit","open file failed");
            return;
        }
        type = TYPE_CLIPS;
        this.clipDuration = parseTime(duration);
        this.mCallBack = callBack;
        new Thread(new Runnable() {
            @Override
            public void run() {
                isBusy = true;
                callBack.onStart();
                videoClips(mp4InputPath, outPutPath, startTime, duration);
                callBack.onEnd();
                isBusy = false;
            }
        }).start();
    }

    private int parseTime(String time) {
        String[] times = time.split(":");
        return (Integer.parseInt(times[0]) * 3600 + Integer.parseInt(times[1]) * 60 + Integer.parseInt(times[2])) * 1000;
    }

    private native void addGifWater(String mp4InputPath, String gif, String outPutPath, String waterW, String waterH, String xPercent, String yPercent);

    private native void videoClips(String mp4InputPath, String outPutPath, String startTime, String duration);

    public native String getFFmpegVersion();
}
