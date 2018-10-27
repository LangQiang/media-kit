/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.lq.mediakit.media;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FrameExtractor10 {

    private static final String TAG = "FrameExtractor";

    private static final int BITMAP_WIDTH = 120;
    private static final int BITMAP_HEIGHT = 120;

    public interface Callback {
        void onFrameExtracted(Bitmap bitmap, long timestamp);
    }

    private final ExecutorService mExecutor;

    private final MediaMetadataRetriever mRetriever = new MediaMetadataRetriever();

    private final Canvas mCanvas = new Canvas();
    private final Rect mRect = new Rect();
    private SparseArray mMetaDataCache = new SparseArray();
    private String mVideoPath;

    private class Task extends AsyncTask<Void, Void, Bitmap> {

        private final long timestampNano;

        private Callback callback;

        public Task(Callback callback, long timestampNano) {
            this.callback = callback;
            this.timestampNano = timestampNano;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            if (isCancelled()) {
                return null;
            }
            long micro = TimeUnit.NANOSECONDS.toMicros(timestampNano);
            Bitmap bmp = mRetriever.getFrameAtTime(micro);

            if (bmp == null) {
                return null;
            }

            if (isCancelled()) {
                return null;
            }

            Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);

            mCanvas.setBitmap(bitmap);
            Rect srcRect = new Rect();
            int bmpWidth = bmp.getWidth();
            int bmpHeight = bmp.getHeight();
            if (bmpWidth >= bmpHeight) {
                srcRect.left = (bmpWidth - bmpHeight) / 2;
                srcRect.right = (bmpWidth - bmpHeight) / 2 + bmpHeight;
                srcRect.top = 0;
                srcRect.bottom = bmpHeight;
            } else {
                srcRect.left = 0;
                srcRect.right = bmpWidth;
                srcRect.top = (bmpHeight - bmpWidth) / 2;
                srcRect.bottom = (bmpHeight - bmpWidth) / 2 + bmpWidth;
            }
            mCanvas.drawBitmap(bmp, srcRect, mRect, null);
            bmp.recycle();
            return bitmap;
        }

        @Override
        protected void onCancelled(Bitmap bitmap) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            callback.onFrameExtracted(bitmap, timestampNano);
        }

    }


    public FrameExtractor10() {
        mExecutor = Executors.newSingleThreadExecutor();

        int width = 128;
        int height = 128;
        mRect.set(0, 0, width, height);
    }

    public AsyncTask<Void, Void, Bitmap> newTask(Callback callback, long timestampNano) {
        return new Task(callback, timestampNano).executeOnExecutor(mExecutor);
    }

    public boolean setDataSource(String source) {
        try {
            mVideoPath = source;
            mRetriever.setDataSource(source);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public long getVideoDuration() {
        Object result;
        if ((result = mMetaDataCache.get(MediaMetadataRetriever.METADATA_KEY_DURATION)) != null) {
            return (Long) result;
        } else if (mVideoPath != null) {
            String durationStr = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if(durationStr != null && !"".equals(durationStr)) {
                Long duration = Long.parseLong(durationStr);
                mMetaDataCache.put(MediaMetadataRetriever.METADATA_KEY_DURATION, duration);
                return duration;
            }else {
                Log.e(TAG, "Retrieve video duration failed");
                return 0;
            }
        } else {
            Log.e(TAG, "Has no video source,so duration is 0");
            return 0;
        }
    }

    public void release() {
        mExecutor.shutdownNow();
        while (true) {
            try {
                if (mExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
            }
        }

        mRetriever.release();
    }

}
