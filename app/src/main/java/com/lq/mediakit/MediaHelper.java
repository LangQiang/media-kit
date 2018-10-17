package com.lq.mediakit;


import android.util.Log;

public class MediaHelper {

    static {
        try{
            System.loadLibrary("avutil-55");
            System.loadLibrary("swresample-2");
            System.loadLibrary("avcodec-57");
            System.loadLibrary("avformat-57");
            System.loadLibrary("swscale-4");
            System.loadLibrary("avfilter-6");
            //System.loadLibrary("sffhelloworld");
            System.loadLibrary("media-kit");
        }catch(Exception e){
            e.printStackTrace();
            Log.e("ffmpeg",e.toString());
        }

    }

    private CallBack mCallBack;

    public static abstract class CallBack {
        public abstract void onStart();
        public abstract void onProgress(int progress,int duration);
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
        this.mCallBack = callBack;
        new Thread(new Runnable() {
            @Override
            public void run() {
                callBack.onStart();
                addGifWater(mp4InputPath,gif,outPutPath,waterW + "",waterH + "",xPercent + "",yPercent + "");
                callBack.onEnd();
            }
        }).start();
    }

    private void onProgress(int second,int duration) {
        if (mCallBack != null) {
            mCallBack.onProgress(second,duration);
        }
    }

    private native void addGifWater(String mp4InputPath, String gif, String outPutPath, String waterW, String waterH, String xPercent, String yPercent);
}
